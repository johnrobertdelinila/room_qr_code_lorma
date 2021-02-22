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

import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProviders;

import com.example.johnrobertdelinila.roomqrcode.LiveBarcodeScanningActivity;
import com.example.johnrobertdelinila.roomqrcode.MainActivity;
import com.example.johnrobertdelinila.roomqrcode.R;
import com.example.johnrobertdelinila.roomqrcode.camera.WorkflowModel;
import com.example.johnrobertdelinila.roomqrcode.camera.WorkflowModel.WorkflowState;
import com.example.johnrobertdelinila.roomqrcode.utils.Room;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcode;

/** Displays the bottom sheet to present barcode fields contained in the detected barcode. */
public class BarcodeResultFragment extends BottomSheetDialogFragment {

  private static final String TAG = "BarcodeResultFragment";
  private static final String ARG_VENDOR = "arg_vendor";
  private static final String ARG_ERROR = "arg_error";
  private static final String ARG_BARCODE = "arg_barcode";

  public static void show(FragmentManager fragmentManager, DocumentSnapshot documentSnapshot, String err, FirebaseVisionBarcode visionBarcode) {
    BarcodeResultFragment barcodeResultFragment = new BarcodeResultFragment();
    Bundle bundle = new Bundle();

    Room room = null;
    if (documentSnapshot != null) {
      room = documentSnapshot.toObject(Room.class);
    }
    bundle.putSerializable(ARG_VENDOR, room);
    bundle.putSerializable(ARG_BARCODE, visionBarcode.getRawValue());

    bundle.putString(ARG_ERROR, err);
    barcodeResultFragment.setArguments(bundle);
    barcodeResultFragment.show(fragmentManager, TAG);
  }

  public static void dismiss(FragmentManager fragmentManager) {
    BarcodeResultFragment barcodeResultFragment =
        (BarcodeResultFragment) fragmentManager.findFragmentByTag(TAG);
    if (barcodeResultFragment != null) {
      barcodeResultFragment.dismiss();
    }
  }

  @Nullable
  @Override
  public View onCreateView(@NonNull LayoutInflater layoutInflater, @Nullable ViewGroup viewGroup, @Nullable Bundle bundle) {
    View view = layoutInflater.inflate(R.layout.barcode_bottom_sheet, viewGroup);

    String err = "";
    Room room = null;
    String barcode = null;

    Bundle arguments = getArguments();
    if (arguments != null && arguments.containsKey(ARG_VENDOR)) {
      room = (Room) arguments.getSerializable(ARG_VENDOR);
      err = arguments.getString(ARG_ERROR);
      barcode = arguments.getString(ARG_BARCODE);
    } else {
      Log.e(TAG, "No barcode field list passed in!");
    }

    Log.e("ROOM", String.valueOf(room));

    if (err != null && err.length() > 0) {
        view.findViewById(R.id.vendor_error).setVisibility(View.VISIBLE);
        ((TextView) view.findViewById(R.id.vendor_error)).setText(err);
        view.findViewById(R.id.vendor_container).setVisibility(View.GONE);
    }else {
        if (room != null && room.getRoomName() != null) {
          view.findViewById(R.id.room_name).setVisibility(View.VISIBLE);
          ((TextView) view.findViewById(R.id.room_name)).setText(room.getRoomName());
        }
    }

    String finalBarcode = barcode;
    view.findViewById(R.id.btn_attendance).setOnClickListener(v -> {
      if (getActivity() != null) {
        MainActivity.rawValue = finalBarcode;
        MainActivity.isPerfomedQrScan = true;
        MainActivity.isTimeIn = ((RadioGroup) view.findViewById(R.id.radio_group)).getCheckedRadioButtonId() == R.id.time_in;
        getActivity().onBackPressed();
        dismiss();
      }
    });

    return view;
  }

  @Override
  public void onDismiss(@NonNull DialogInterface dialogInterface) {
    if (getActivity() != null) {
      // Back to working state after the bottom sheet is dismissed.
      ViewModelProviders.of(getActivity())
          .get(WorkflowModel.class)
          .setWorkflowState(WorkflowState.DETECTING);
    }
    super.onDismiss(dialogInterface);
  }
}
