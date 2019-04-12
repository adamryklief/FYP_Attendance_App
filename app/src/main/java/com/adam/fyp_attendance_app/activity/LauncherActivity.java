package com.adam.fyp_attendance_app.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

public class LauncherActivity extends AppCompatActivity {

    public static final String TAG = "LauncherActivity";
    private static final String SHARED_PREFS_FILE = "com.adam.fyp_attendance_app.DATA_STORE_FILE";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences prefs = getSharedPreferences(SHARED_PREFS_FILE, MODE_PRIVATE);

        if(prefs.getString("auth_token", null) == null) {
            startActivity(new Intent(LauncherActivity.this, LoginActivity.class));
        } else {
            startActivity(new Intent(LauncherActivity.this, AttendanceActivity.class));
            Log.d(TAG, "Auth Token: " + prefs.getString("auth_token", null));
        }
        finish();

    }
}
