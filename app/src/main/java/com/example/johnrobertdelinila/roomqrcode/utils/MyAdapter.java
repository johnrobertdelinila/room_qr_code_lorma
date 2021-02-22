package com.example.johnrobertdelinila.roomqrcode.utils;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.example.johnrobertdelinila.roomqrcode.R;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.kaopiz.kprogresshud.KProgressHUD;
import com.tapadoo.alerter.Alerter;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class MyAdapter extends RecyclerView.Adapter<MyAdapter.MyViewHolder> {

    private ArrayList<Upload> uploads;
    private Context context;
    private Activity activity;
    private DatabaseHelper mDatabaseHelper;
    private KProgressHUD hud;
    private TextView textNoData;

    // Firestore
    private FirebaseFirestore mFirestore = FirebaseFirestore.getInstance();
    private CollectionReference attendance = mFirestore.collection("attendance");

    public MyAdapter(ArrayList<Upload> uploads, Activity activity, DatabaseHelper mDatabaseHelper, TextView textNoData) {
        this.uploads = uploads;
        this.activity = activity;
        this.mDatabaseHelper = mDatabaseHelper;
        this.textNoData = textNoData;
        hud = KProgressHUD.create(activity)
                .setStyle(KProgressHUD.Style.SPIN_INDETERMINATE)
                .setLabel(activity.getString(R.string.text_please_wait))
                .setCancellable(true)
                .setAnimationSpeed(2)
                .setDimAmount(0.5f);
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.offline_upload_list, viewGroup, false);
        return new MyViewHolder(view);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull MyViewHolder myViewHolder, int i) {
        Upload upload = uploads.get(i);

        /*String stamp = (String) upload.getTimestamp();
        Long timestamp = Long.valueOf(stamp);*/

        SimpleDateFormat sdf_time = new SimpleDateFormat("hh:mm a", Locale.getDefault());
        SimpleDateFormat sdf_date = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());

        String timeIn = sdf_time.format(upload.getTimestamp());
        String date = sdf_date.format(upload.getTimestamp());

        myViewHolder.textTimestamp.setText("Time: " + timeIn);
        myViewHolder.date.setText("Date: " + date);

        myViewHolder.itemView.setOnClickListener(v -> {
            AlertDialog.Builder dialog = new AlertDialog.Builder(activity);
            dialog.setTitle("Attendance");
            dialog.setMessage("Time: " + timeIn + "\nAre you sure to time in?");
            dialog.setNegativeButton("CANCEL", (dialog1, which) -> dialog1.dismiss());
            dialog.setPositiveButton("CONFIRM", (dialog1, which) -> {
                uploadToFirebase(upload, i);
            });
            dialog.show();
        });
    }

    @Override
    public int getItemCount() {
        return uploads.size();
    }

    public static class MyViewHolder extends RecyclerView.ViewHolder {
        TextView textTimestamp, date;
        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            textTimestamp = itemView.findViewById(R.id.timestamp);
            date = itemView.findViewById(R.id.date);
        }
    }

    private String getDateFormat(long transactionTimestamp) {
        String output;

        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        long currentTimestamp = timestamp.getTime();
        long day = 86400000; // 1 day to milliseconds

        SimpleDateFormat sdf;
        if ((currentTimestamp - day) < transactionTimestamp) {
            // today transactions
            sdf = new SimpleDateFormat("h:mm a", Locale.getDefault());
            output = "Last 24 hours " + sdf.format(new Date(transactionTimestamp));
        }else if ((currentTimestamp - (day * 2)) < transactionTimestamp) {
            // yesterday transactions
            sdf = new SimpleDateFormat("h:mm a", Locale.getDefault());
            output = "Yesterday " + sdf.format(new Date(transactionTimestamp));
        } else {
            // more than 2 days
            sdf = new SimpleDateFormat("MMM d, yyyy", Locale.getDefault());
            output = sdf.format(new Date(transactionTimestamp));
        }
        return output;
    }

    private void uploadToFirebase(Upload upload, int position) {
        if (isOnline()) {
            hud.show();
            uploads.remove(upload);
            String id = upload.getKey();
            upload.removeKey();
            attendance.add(upload)
                    .addOnCompleteListener(task -> {
                        hud.dismiss();
                        if (task.getException() != null) {
                            Toast.makeText(activity, task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                            return;
                        }
                        if (mDatabaseHelper.deleteUpload(Integer.parseInt(id))) {
                            Alerter.create(activity)
                                    .setText("Success Time In.")
                                    .setTitle("Attendance")
                                    .setIcon(R.drawable.done_icon)
                                    .setBackgroundColorRes(R.color.colorAccent)
                                    .setIconColorFilter(0) // Optional - Removes white tint
                                    .enableSwipeToDismiss()
                                    .show();
                            notifyItemRemoved(position);
                            notifyItemRangeChanged(position, uploads.size());
                            showTextNoData();
                        }else {
                            Toast.makeText(activity, "Something went wrong in the database.", Toast.LENGTH_SHORT).show();
                        }
                    });

           /* MainActivity.imeiRef.push().setValue(upload)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            if (mDatabaseHelper.deleteUpload(Integer.parseInt(id))) {
                                Alerter.create(activity)
                                        .setText("Success Time In.")
                                        .setTitle("Attendance")
                                        .setIcon(R.drawable.done_icon)
                                        .setBackgroundColorRes(R.color.colorAccent)
                                        .setIconColorFilter(0) // Optional - Removes white tint
                                        .enableSwipeToDismiss()
                                        .show();
                                notifyItemRemoved(position);
                                notifyItemRangeChanged(position, uploads.size());
                                showTextNoData();
                            }else {
                                Toast.makeText(activity, "Something went wrong in the database.", Toast.LENGTH_SHORT).show();
                            }
                            hud.dismiss();
                        }else if (task.getException() != null) {
                            hud.dismiss();
                            Toast.makeText(activity, task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });*/
        }else {
            Toast.makeText(activity, "You have no internet connection.", Toast.LENGTH_SHORT).show();
        }
    }

    protected boolean isOnline() {
        ConnectivityManager cm = (ConnectivityManager)activity.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        if (netInfo != null && netInfo.isConnectedOrConnecting()) {
            return true;
        } else {
            return false;
        }
    }

    private void showTextNoData() {
        if (uploads.size() > 0) {
            textNoData.setVisibility(View.GONE);
        }else {
            textNoData.setVisibility(View.VISIBLE);
        }
    }

}
