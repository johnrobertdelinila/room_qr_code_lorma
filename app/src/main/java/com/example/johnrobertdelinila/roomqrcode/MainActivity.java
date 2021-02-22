package com.example.johnrobertdelinila.roomqrcode;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.johnrobertdelinila.roomqrcode.utils.DatabaseHelper;
import com.example.johnrobertdelinila.roomqrcode.utils.MyAdapter;
import com.example.johnrobertdelinila.roomqrcode.utils.Upload;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.SetOptions;
import com.kaopiz.kprogresshud.KProgressHUD;
import com.tapadoo.alerter.Alerter;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    public static String rawValue;
    public static boolean isPerfomedQrScan = false;
    public static boolean isTimeIn = true;
    private static String staticFullName;
    public static final int READ_PHONE_PERMISSION = 2000;
    private static final int PERMISSION_REQUESTS = 1;

    /*private BottomAppBar bottomAppBar;
    private BottomNavigationView bottomNavigationView;*/

    private KProgressHUD hud;
    private TextView textNoData;
    // Realtime database
    private static FirebaseDatabase mDatabase = FirebaseDatabase.getInstance();
    public static DatabaseReference rootRef = mDatabase.getReference();
    public static DatabaseReference imeiRef = rootRef.child("attendance");
    private Query notificationRef = null;
    private ChildEventListener notificationListener = new ChildEventListener() {
        @Override
        public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
            String message = dataSnapshot.child("message").getValue(String.class);
            Toast.makeText(MainActivity.this, message, Toast.LENGTH_LONG).show();
            Map<String, Object> updateSeen = new HashMap<>();
            updateSeen.put("seen", true);
            dataSnapshot.getRef().updateChildren(updateSeen);
        }

        @Override
        public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

        }

        @Override
        public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {

        }

        @Override
        public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

        }

        @Override
        public void onCancelled(@NonNull DatabaseError databaseError) {

        }
    };
    // Firestore
    private FirebaseFirestore mFirestore = FirebaseFirestore.getInstance();
    private CollectionReference attendance = mFirestore.collection("attendance");
    private CollectionReference notifications = mFirestore.collection("notifications");
    private CollectionReference instructors = mFirestore.collection("instructors");
    private com.google.firebase.firestore.Query notificationQuery = null;
    private ListenerRegistration notificationReg = null;

    private DatabaseHelper mDatabaseHelper;

    private ArrayList<Upload> uploads = new ArrayList<>();
    private MyAdapter myAdapter;
    private String IMEI = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        hud = KProgressHUD.create(this)
                .setStyle(KProgressHUD.Style.SPIN_INDETERMINATE)
                .setLabel(getString(R.string.text_please_wait))
                .setCancellable(true)
                .setAnimationSpeed(2)
                .setDimAmount(0.5f);

        mDatabaseHelper = new DatabaseHelper(getApplicationContext());
        textNoData = findViewById(R.id.text_no_data);
        myAdapter = new MyAdapter(uploads, this, mDatabaseHelper, textNoData);

        RecyclerView recyclerView = findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.addItemDecoration(new DividerItemDecoration(recyclerView.getContext(), DividerItemDecoration.VERTICAL));

        populateRecyclerView(recyclerView);

       /* bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomAppBar = findViewById(R.id.bar);
        setSupportActionBar(bottomAppBar);

        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .add(R.id.container, new MyScansFragment())
                    .commit();
        }

        findViewById(R.id.fab).setOnClickListener(v -> {
            BarcodeGraphic.numOfSuccessRead = 0;
            startActivity(new Intent(MainActivity.this, CameraActivity.class));
        });

        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            switch (item.getItemId()) {
                case R.id.my_scans:
                    navigateTo(new MyScansFragment(), false);
                    return true;
                case R.id.profile:
                    navigateTo(new ProfileFragment(), false);
                    return true;
                default:
                    return true;
            }
        });*/

        fetchImei();

        findViewById(R.id.fab).setOnClickListener(v -> {
            if (allPermissionsGranted()) {
                startActivity(new Intent(MainActivity.this, LiveBarcodeScanningActivity.class));
            } else {
                getRuntimePermissions();
            }
        });
    }

    private void getRuntimePermissions() {
        List<String> allNeededPermissions = new ArrayList<>();
        for (String permission : getRequiredPermissions()) {
            if (!isPermissionGranted(this, permission)) {
                allNeededPermissions.add(permission);
            }
        }

        if (!allNeededPermissions.isEmpty()) {
            ActivityCompat.requestPermissions(
                    this, allNeededPermissions.toArray(new String[0]), PERMISSION_REQUESTS);
        }
    }


    private boolean allPermissionsGranted() {
        for (String permission : getRequiredPermissions()) {
            if (!isPermissionGranted(this, permission)) {
                return false;
            }
        }
        return true;
    }

    private static boolean isPermissionGranted(Context context, String permission) {
        return ContextCompat.checkSelfPermission(context, permission)
                == PackageManager.PERMISSION_GRANTED;
    }

    private String[] getRequiredPermissions() {
        try {
            PackageInfo info =
                    this.getPackageManager()
                            .getPackageInfo(this.getPackageName(), PackageManager.GET_PERMISSIONS);
            String[] ps = info.requestedPermissions;
            if (ps != null && ps.length > 0) {
                return ps;
            } else {
                return new String[0];
            }
        } catch (Exception e) {
            return new String[0];
        }
    }

    private void populateRecyclerView(RecyclerView recyclerView) {
        Cursor data = mDatabaseHelper.getData();
        while(data.moveToNext()) {
            String imei = data.getString(1);
            String key = String.valueOf(data.getInt(0));
            String timestamp = data.getString(2);
            String rawValue = data.getString(3);
            Boolean timeIn = data.getInt(4) != 0;
            Boolean online = data.getInt(5) != 0;
            Date offlineDate = new Date(Long.parseLong(timestamp));
            Upload upload = new Upload(rawValue, imei, offlineDate, timeIn, online);
            upload.setKey(key);

            uploads.add(upload);
        }
        myAdapter = new MyAdapter(uploads, this, mDatabaseHelper, textNoData);
        recyclerView.setAdapter(myAdapter);
        showTextNoData();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (isPerfomedQrScan) {
            showConfirmationUpload();
            isPerfomedQrScan = false;
        }
        listenForNotification();
    }

    private void showConfirmationUpload() {
        AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        dialog.setTitle("Attendance");
        Long timestamp = new Timestamp(System.currentTimeMillis()).getTime();
        SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());
        String timeIn = sdf.format(new Date(timestamp));
        dialog.setMessage("Time: " + timeIn + "\nAre you sure to "+(isTimeIn ? "Time In" : "Time Out")+"?");
        dialog.setNegativeButton("CANCEL", (dialog1, which) -> dialog1.dismiss());
        dialog.setPositiveButton("CONFIRM", (dialog1, which) -> {
            if (IMEI != null) {
                uploadToDatabase();
            }else {
                Toast.makeText(this, "Can't get your phone's IMEI.", Toast.LENGTH_SHORT).show();
            }
        });
        dialog.show();
    }

    @SuppressLint("HardwareIds")
    private void fetchImei() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[] {
                    Manifest.permission.READ_PHONE_STATE
            }, MainActivity.READ_PHONE_PERMISSION);
        }else {
            TelephonyManager telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Please turn on the permission request for IMEI in the settings.", Toast.LENGTH_SHORT).show();
                return;
            }
            IMEI = telephonyManager.getDeviceId();
            if (IMEI == null) {
                Toast.makeText(this, "Can't get the phone IMEI", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
            // Realtime database
            notificationRef = rootRef.child("notifications").child(IMEI).orderByChild("seen").equalTo(false).limitToFirst(1);
            // Firestore
            notificationQuery = notifications
                    .whereEqualTo("imei", IMEI)
                    .whereEqualTo("seen", false)
                    .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                    .limit(1);
            listenForNotification();

            instructors.document(IMEI).get()
                    .addOnCompleteListener(task -> {
                        if (task.getException() != null) {
                            return;
                        }
                        DocumentSnapshot documentSnapshot = task.getResult();
                        if (documentSnapshot == null) {
                            return;
                        }
                        if (!documentSnapshot.exists()) {
                            return;
                        }
                        String fullName = (String) documentSnapshot.get("full_name");
                        if (fullName == null) {
                            return;
                        }
                        staticFullName = fullName;
                    });
        }
    }

    private void uploadToDatabase() {
        if (rawValue == null) {
            Toast.makeText(this, "QR Code's value is null", Toast.LENGTH_SHORT).show();
        }else {
            if (isOnline()) {
                hud.show();
                Upload upload = new Upload(rawValue, IMEI, null, isTimeIn, true);
                attendance.add(upload)
                    .addOnCompleteListener(task -> {
                        hud.dismiss();
                        if (task.getException() != null) {
                            Toast.makeText(this, task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                            return;
                        }
                        Alerter.create(MainActivity.this)
                            .setTitle("Attendance")
                            .setText("Success Time In.")
                            .setIcon(R.drawable.done_icon)
                            .setBackgroundColorRes(R.color.colorAccent)
                            .setIconColorFilter(0) // Optional - Removes white tint
                            .enableSwipeToDismiss()
                            .show();
                    });
            }else {
                Timestamp timestamp = new Timestamp(System.currentTimeMillis());
                String offlineTimestamp = String.valueOf(timestamp.getTime());
                if (addData(IMEI, offlineTimestamp, rawValue, isTimeIn, false)) {
                    Alerter.create(MainActivity.this)
                            .setTitle("Attendance")
                            .setText("It looks like you don't have internet connection.")
                            .setIconColorFilter(0) // Optional - Removes white tint
                            .enableSwipeToDismiss()
                            .show();

                    Upload upload1 = mDatabaseHelper.getTheLastUpload();
                    uploads.add(upload1);
                    myAdapter.notifyItemInserted(uploads.size() - 1);
                    showTextNoData();
                }
            }
        }
    }

    protected boolean isOnline() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        return netInfo != null && netInfo.isConnectedOrConnecting();
    }

    public boolean addData(String imei, String timestamp, String rawValue, Boolean isTimeIn, Boolean isOnline) {
        boolean isSuccess = mDatabaseHelper.addUpload(imei, timestamp, rawValue, isTimeIn, isOnline);
        if (!isSuccess) {
            Toast.makeText(this, "Something went wrong.", Toast.LENGTH_SHORT).show();
        }
        return isSuccess;
    }

    private void listenForNotification() {
        // Realtime database
        if (notificationRef != null) {
            notificationRef.addChildEventListener(notificationListener);
        }
        // Firestore
        if (notificationQuery != null) {
            notificationReg = notificationQuery.addSnapshotListener((queryDocumentSnapshots, e) -> {
                if (e != null) {
                    Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
                    return;
                }
                if (queryDocumentSnapshots == null) {
                    Toast.makeText(this, "Query snapshot is null", Toast.LENGTH_SHORT).show();
                    return;
                }
                for (DocumentChange dc : queryDocumentSnapshots.getDocumentChanges()) {
                    if (dc.getType() == DocumentChange.Type.ADDED) {// New notification found
                        dc.getDocument().getReference().update("seen", true);
                        String message = (String) dc.getDocument().getData().get("message");
                        if (message == null) {
                            Toast.makeText(this, "Message is null", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        Toast.makeText(MainActivity.this, message, Toast.LENGTH_LONG).show();
                    }
                }
            });
        }
    }

    @SuppressLint("HardwareIds")
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == MainActivity.READ_PHONE_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                TelephonyManager telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Please turn on the permission request for IMEI in the settings.", Toast.LENGTH_SHORT).show();
                    return;
                }
                IMEI = telephonyManager.getDeviceId();
                if (IMEI == null) {
                    Toast.makeText(this, "Can't get your phone's IMEI", Toast.LENGTH_SHORT).show();
                    finish();
                    return;
                }
                notificationRef = rootRef.child("notifications").child(IMEI).orderByChild("seen").equalTo(false).limitToFirst(1);
                notificationQuery = notifications
                        .whereEqualTo("imei", IMEI)
                        .whereEqualTo("seen", false)
                        .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                        .limit(1);
                listenForNotification();
            }else {
                Toast.makeText(this, "Please turn on the permission request for IMEI in the settings.", Toast.LENGTH_SHORT).show();
                finish();
            }
        }else if (requestCode == PERMISSION_REQUESTS) {
            if (allPermissionsGranted()) {
                startActivity(new Intent(MainActivity.this, LiveBarcodeScanningActivity.class));
            }
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (notificationRef != null && notificationListener != null) {
            notificationRef.removeEventListener(notificationListener);
        }
        if (notificationReg != null) {
            notificationReg.remove();
        }
    }

    private void showTextNoData() {
        if (uploads.size() > 0) {
            textNoData.setVisibility(View.GONE);
        }else {
            textNoData.setVisibility(View.VISIBLE);
        }
    }

    public void showRegistration(View view) {
        AlertDialog.Builder dialog = new AlertDialog.Builder(MainActivity.this);
        dialog.setTitle("Register");
        View view1 = getLayoutInflater().inflate(R.layout.layout_register, null);
        TextInputEditText editFullName = view1.findViewById(R.id.fullname_edit_text);
        if (staticFullName != null) {
            editFullName.setText(staticFullName);
        }
        dialog.setView(view1);
        dialog.setPositiveButton("DONE", (dialog1, which) -> {
            if (editFullName != null && editFullName.getText() != null) {
                hud.show();
                String fullName = editFullName.getText().toString();
                Map<String, Object> account = new HashMap<>();
                account.put("full_name", fullName);
                instructors.document(IMEI).set(account, SetOptions.merge())
                        .addOnCompleteListener(task -> {
                            if (task.getException() != null) {
                                return;
                            }
                            hud.dismiss();
                            Toast.makeText(this, "Account updated successfully.", Toast.LENGTH_SHORT).show();
                            staticFullName = fullName;
                        });

            }
        });
        dialog.setNegativeButton("CANCEL", (dialog12, which) -> dialog12.dismiss());
        dialog.show();
    }
}
