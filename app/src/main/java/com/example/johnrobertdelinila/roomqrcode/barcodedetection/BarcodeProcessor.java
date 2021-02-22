/*
 * Copyright 2019 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.johnrobertdelinila.roomqrcode.barcodedetection;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Context;
import android.graphics.RectF;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import androidx.annotation.MainThread;

import com.example.johnrobertdelinila.roomqrcode.camera.CameraReticleAnimator;
import com.example.johnrobertdelinila.roomqrcode.camera.FrameProcessorBase;
import com.example.johnrobertdelinila.roomqrcode.camera.GraphicOverlay;
import com.example.johnrobertdelinila.roomqrcode.camera.WorkflowModel;
import com.example.johnrobertdelinila.roomqrcode.camera.WorkflowModel.WorkflowState;
import com.example.johnrobertdelinila.roomqrcode.settings.PreferenceUtils;
import com.example.johnrobertdelinila.roomqrcode.utils.QrCode;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcode;
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcodeDetector;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;

import java.io.IOException;
import java.util.List;

/** A processor to run the barcode detector. */
public class BarcodeProcessor extends FrameProcessorBase<List<FirebaseVisionBarcode>> {

  private static final String TAG = "BarcodeProcessor";

  private final FirebaseVisionBarcodeDetector detector =
      FirebaseVision.getInstance().getVisionBarcodeDetector();
  private final WorkflowModel workflowModel;
  private final CameraReticleAnimator cameraReticleAnimator;
  private final Activity activity;

  public BarcodeProcessor(GraphicOverlay graphicOverlay, WorkflowModel workflowModel, Activity activity) {
    this.workflowModel = workflowModel;
    this.cameraReticleAnimator = new CameraReticleAnimator(graphicOverlay);
    this.activity = activity;
  }

  @Override
  protected Task<List<FirebaseVisionBarcode>> detectInImage(FirebaseVisionImage image) {
    return detector.detectInImage(image);
  }

  @MainThread
  @Override
  protected void onSuccess(
      FirebaseVisionImage image,
      List<FirebaseVisionBarcode> results,
      GraphicOverlay graphicOverlay) {
    if (!workflowModel.isCameraLive()) {
      return;
    }

    Log.d(TAG, "Barcode result size: " + results.size());

    // Picks the barcode, if exists, that covers the center of graphic overlay.
    FirebaseVisionBarcode barcodeInCenter = null;
    for (FirebaseVisionBarcode barcode : results) {
      RectF box = graphicOverlay.translateRect(barcode.getBoundingBox());
      if (box.contains(graphicOverlay.getWidth() / 2f, graphicOverlay.getHeight() / 2f)) {
        barcodeInCenter = barcode;
        break;
      }
    }

    graphicOverlay.clear();
    if (barcodeInCenter == null) {
      cameraReticleAnimator.start();
      graphicOverlay.add(new BarcodeReticleGraphic(graphicOverlay, cameraReticleAnimator));
      workflowModel.setWorkflowState(WorkflowState.DETECTING);

    } else {
      cameraReticleAnimator.cancel();
      float sizeProgress =
          PreferenceUtils.getProgressToMeetBarcodeSizeRequirement(graphicOverlay, barcodeInCenter);
      if (sizeProgress < 1) {
        // Barcode in the camera view is too small, so prompt user to move camera closer.
        graphicOverlay.add(new BarcodeConfirmingGraphic(graphicOverlay, barcodeInCenter));
        workflowModel.setWorkflowState(WorkflowState.CONFIRMING);

      } else {

        /*if (PreferenceUtils.shouldDelayLoadingBarcodeResult(graphicOverlay.getContext())) {
          ValueAnimator loadingAnimator = createLoadingAnimator(graphicOverlay, barcodeInCenter);
          loadingAnimator.start();
          graphicOverlay.add(new BarcodeLoadingGraphic(graphicOverlay, loadingAnimator));
          workflowModel.setWorkflowState(WorkflowState.SEARCHING);
        } else {
          workflowModel.setWorkflowState(WorkflowState.DETECTED);
          workflowModel.detectedBarcode.setValue(barcodeInCenter);
        }*/

        // Barcode size in the camera view is sufficient.
        ValueAnimator loadingAnimator = createLoadingAnimator(graphicOverlay);
        loadingAnimator.start();
        graphicOverlay.add(new BarcodeLoadingGraphic(graphicOverlay, loadingAnimator));
        workflowModel.documentSnapshot = null;
        workflowModel.error = null;
        workflowModel.setWorkflowState(WorkflowState.SEARCHING);
        if (barcodeInCenter.getRawValue() != null) {
          FirebaseVisionBarcode finalBarcodeInCenter = barcodeInCenter;

          // Check first if the device have internet connection
          if (isInternetConnected()) {
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            db.collection("qr_codes").document(finalBarcodeInCenter.getRawValue()).get()
                    .addOnCompleteListener(task -> {
                      if (task.getException() != null) {
                        // Error in firestore qr codes
                        terminate(task.getException().getMessage(), graphicOverlay, finalBarcodeInCenter);
                        Log.e("STATUS", "Error in firestore qr codes");
                        return;
                      }
                      DocumentSnapshot documentSnapshot = task.getResult();
                      if (documentSnapshot == null || !documentSnapshot.exists())  {
                        // The qr does not exist in the database
                        terminate(null, graphicOverlay, finalBarcodeInCenter);
                        Log.e("STATUS", "The qr does not exist in the database");
                        return;
                      }
                      QrCode qrCode = documentSnapshot.toObject(QrCode.class);
                      if (qrCode == null) {
                        // Qr code is invalid
                        terminate(null, graphicOverlay, finalBarcodeInCenter);
                        Log.e("STATUS", "Qr code is invalid");
                        return;
                      }
                      // Insurance
                      if (qrCode.getDeactivated() == null || qrCode.getRoom() == null) {
                        qrCode.setDeactivated((Boolean) documentSnapshot.get("isDeactivated"));
                        qrCode.setRoom((String) documentSnapshot.get("room"));
                      }
                      if (qrCode.getDeactivated() == null || qrCode.getDeactivated() || qrCode.getRoom() == null) {
                        // Qr code is not deactivated, but still going to attendance
                        terminate(null, graphicOverlay, finalBarcodeInCenter);
                        Log.e("STATUS", "Qr code is not deactivated, but still going to attendance");
                        return;
                      }

                      db.collection("rooms").document(qrCode.getRoom()).get()
                              .addOnCompleteListener(task1 -> {
                                if (task1.getException() != null) {
                                  // Error in firestore rooms
                                  terminate(task1.getException().getMessage(), graphicOverlay, finalBarcodeInCenter);
                                  Log.e("STATUS", "Error in firestore rooms");
                                  return;
                                }
                                workflowModel.documentSnapshot = task1.getResult();
                                terminate(null, graphicOverlay, finalBarcodeInCenter);
                              });

                    });
          }else {
            // No Internet connection
            terminate(null, graphicOverlay, finalBarcodeInCenter);
          }
        }

      }
    }
    graphicOverlay.invalidate();
  }

  private void terminate(String message, GraphicOverlay graphicOverlay, FirebaseVisionBarcode finalBarcodeInCenter) {
    if (message != null) {
      workflowModel.error = message;
    }
    graphicOverlay.clear();
    workflowModel.setWorkflowState(WorkflowState.DETECTED);
    workflowModel.detectedBarcode.setValue(finalBarcodeInCenter);
  }

  private boolean isInternetConnected() {
    ConnectivityManager cm = (ConnectivityManager) activity.getSystemService(Context.CONNECTIVITY_SERVICE);
    NetworkInfo netInfo = cm.getActiveNetworkInfo();
    return netInfo != null && netInfo.isConnectedOrConnecting();
  }

  /*private ValueAnimator createLoadingAnimator(
      GraphicOverlay graphicOverlay, FirebaseVisionBarcode barcode) {
    float endProgress = 1.1f;
    ValueAnimator loadingAnimator = ValueAnimator.ofFloat(0f, endProgress);
    loadingAnimator.setDuration(15000);
    loadingAnimator.addUpdateListener(
        animation -> {
          if (Float.compare((float) loadingAnimator.getAnimatedValue(), endProgress) >= 0) {
            graphicOverlay.clear();
            workflowModel.setWorkflowState(WorkflowState.SEARCHED);
            workflowModel.detectedBarcode.setValue(barcode);
          } else {
            graphicOverlay.invalidate();
          }
        });
    return loadingAnimator;
  }*/

  private ValueAnimator createLoadingAnimator(GraphicOverlay graphicOverlay) {
    float endProgress = 1.1f;
    ValueAnimator loadingAnimator = ValueAnimator.ofFloat(0f, endProgress);
    loadingAnimator.setDuration(15000);
    loadingAnimator.addUpdateListener(
            animation -> {
              if (Float.compare((float) loadingAnimator.getAnimatedValue(), endProgress) < 0) {
                graphicOverlay.invalidate();
              }
            });
    return loadingAnimator;
  }

  @Override
  protected void onFailure(Exception e) {
    Log.e(TAG, "Barcode detection failed!", e);
  }

  @Override
  public void stop() {
    try {
      detector.close();
    } catch (IOException e) {
      Log.e(TAG, "Failed to close barcode detector!", e);
    }
  }
}
