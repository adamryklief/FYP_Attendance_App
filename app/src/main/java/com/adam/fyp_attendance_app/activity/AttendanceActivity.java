package com.adam.fyp_attendance_app.activity;

import android.content.Context;
import android.content.SharedPreferences;
import android.nfc.NfcAdapter;
import android.nfc.NfcManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.adam.fyp_attendance_app.R;
import com.adam.fyp_attendance_app.async.AsyncAttend;
import com.adam.fyp_attendance_app.async.AsyncLogout;
import com.adam.fyp_attendance_app.cardreader.SmartCardReader;

public class AttendanceActivity extends AppCompatActivity
        implements SmartCardReader.ResponseToChallengeCallback {

    private static final String TAG = "AttendanceActivity";
    public static int READER_FLAGS =
            NfcAdapter.FLAG_READER_NFC_A | NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK;
    private static final String SHARED_PREFS_FILE = "com.adam.fyp_attendance_app.DATA_STORE_FILE";
    private static final String AUTH_TOKEN = "auth_token";

    private NfcManager manager;
    private NfcAdapter adapter;

    public SmartCardReader smartCardReader;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_attendance);

        smartCardReader = new SmartCardReader(this);

        manager = (NfcManager) this.getSystemService(Context.NFC_SERVICE);
        adapter = manager.getDefaultAdapter();
        if(adapter != null && !adapter.isEnabled()) {
            Toast.makeText(this,
                    "Please turn your NFC on to sign in to class", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.app_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == R.id.log_out)
            new AsyncLogout(AttendanceActivity.this).execute();
        return true;
    }

    @Override
    public void onPause() {
        super.onPause();
        NfcAdapter nfc = NfcAdapter.getDefaultAdapter(this);
        if(nfc != null) {
            nfc.disableReaderMode(this);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        NfcAdapter nfc = NfcAdapter.getDefaultAdapter(this);
        if(adapter != null && !adapter.isEnabled()) {
            Toast.makeText(this,
                    "Please turn your NFC on to sign in", Toast.LENGTH_LONG).show();
        }
        if(nfc != null) {
            nfc.enableReaderMode(this, smartCardReader, READER_FLAGS, null);
        }
    }

    @Override
    public void onResponseToChallengeReceived(String responseToChallenge) {
        if(!responseToChallenge.equals("")) {
            SharedPreferences prefs = getSharedPreferences(SHARED_PREFS_FILE, MODE_PRIVATE);
            String authToken = prefs.getString(AUTH_TOKEN, null);
            new AsyncAttend(AttendanceActivity.this, responseToChallenge, authToken).execute();
        } else {
            runOnUiThread(new Runnable() {
                public void run() {
                    Toast.makeText(AttendanceActivity.this,"Unable to sign you in. " +
                            "You don't have a scheduled lecture right now",Toast.LENGTH_LONG).show();
                }
            });
        }
    }

}
