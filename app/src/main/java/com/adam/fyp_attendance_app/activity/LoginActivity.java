package com.adam.fyp_attendance_app.activity;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.adam.fyp_attendance_app.R;
import com.adam.fyp_attendance_app.async.AsyncLogin;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;

public class LoginActivity extends AppCompatActivity {

    private EditText edTxtStudentId;
    private EditText edTxtPassword;
    private Button btnLogin;
    private AlertDialog alertDialog;
    private String firebaseDeviceId;

    private static final String TAG = "LoginActivity";
    private static final String ALERT_MSG = "You must allow permissions for login to work" +
            " otherwise application will exit";
    private static final String TOAST_MSG_KEY = "toastMessage";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        edTxtStudentId = findViewById(R.id.edTxtStudentId);
        edTxtPassword = findViewById(R.id.edTxtPassword);
        btnLogin = findViewById(R.id.btnLogin);

        String toastMsg = null;
        if(getIntent().hasExtra(TOAST_MSG_KEY)) {
            toastMsg = getIntent().getStringExtra(TOAST_MSG_KEY);
            Toast.makeText(getApplicationContext(), toastMsg, Toast.LENGTH_LONG).show();
        }

        // running on main thread so that login can only execute if success
        FirebaseInstanceId.getInstance().getInstanceId()
                .addOnCompleteListener(new OnCompleteListener<InstanceIdResult>() {
                    @Override
                    public void onComplete(@NonNull Task<InstanceIdResult> task) {
                        if (!task.isSuccessful()) {
                            Log.w(TAG, "getInstanceId failed", task.getException());
                            return;
                        }

                        // Get new Instance ID token
                        final String firebaseDeviceId = task.getResult().getToken();
                        LoginActivity.this.firebaseDeviceId = firebaseDeviceId;
                    }
                });

        doLoginPreCheckAndLogin();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(alertDialog != null) {
            alertDialog.dismiss();
            if(ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionsFromUser();
            } else {
                doLoginPreCheckAndLogin();
            }
        }
    }

    private void doLoginPreCheckAndLogin() {
        if(ActivityCompat.checkSelfPermission(LoginActivity.this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionsFromUser();
        } else {
            TelephonyManager telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
            final String imei = telephonyManager.getDeviceId();
            btnLogin.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    new AsyncLogin(LoginActivity.this, edTxtStudentId.getText().toString(),
                            imei, edTxtPassword.getText().toString(), firebaseDeviceId).execute();
                }
            });
        }
    }

    private void requestPermissionsFromUser() {
        AlertDialog.Builder alertDialogBuilder  = new AlertDialog.Builder(LoginActivity.this);
        alertDialogBuilder.setTitle("Please Note:");
        alertDialogBuilder.setMessage(ALERT_MSG)
                .setCancelable(false)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        openAppSettings();
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                        System.exit(0);
                    }
                });
        alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }

    private void openAppSettings() {
        Uri packageUri = Uri.fromParts("package",
                getApplicationContext().getPackageName(), null );
        Intent applicationDetailsSettingsIntent = new Intent();

        applicationDetailsSettingsIntent.setAction( Settings.ACTION_APPLICATION_DETAILS_SETTINGS );
        applicationDetailsSettingsIntent.setData( packageUri );
        applicationDetailsSettingsIntent.addFlags( Intent.FLAG_ACTIVITY_NEW_TASK );

        getApplicationContext().startActivity( applicationDetailsSettingsIntent );
    }

}
