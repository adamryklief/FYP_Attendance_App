package com.adam.fyp_attendance_app.cardreader;

import android.content.SharedPreferences;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.IsoDep;
import android.util.Log;

import com.adam.fyp_attendance_app.Attendance;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;

import static android.content.Context.MODE_PRIVATE;

public class SmartCardReader implements NfcAdapter.ReaderCallback {

    private static final String TAG = "SmartCardReader";
    private static final String SMART_CARD_AID = "F222222222";
    private static final String SELECT_APDU_HEADER = "00A40400";
    private static final String SHARED_PREFS_FILE = "com.adam.fyp_attendance_app.DATA_STORE_FILE";
    private static final String DATE_TIME_RANDOM_NUMBERS = "dateTimeRandomNumbers";

    private WeakReference<ResponseToChallengeCallback> weakRefResponseToChallengeCallback;

    public interface ResponseToChallengeCallback {
        void onResponseToChallengeReceived(String responseToChallenge);
    }

    public SmartCardReader(ResponseToChallengeCallback responseToChallengeCallback) {
        weakRefResponseToChallengeCallback = new WeakReference<>(responseToChallengeCallback);
    }

    @Override
    public void onTagDiscovered(Tag tag) {
        IsoDep isoDep = IsoDep.get(tag);
        if(isoDep != null) {
            try {
                isoDep.connect(); // connect to the remote NFC device

                // BEGIN Send command APDU for host card emulator smart card service
                byte[] command = BuildSelectApdu(SMART_CARD_AID);
                isoDep.transceive(command);
                // END Send command APDU for host card emulator smart card service

                int randomNumberChallengeForCurrentLecture = getChallengeNumberForCurrentLecture();
                String responseToChallenge = "";
                if(randomNumberChallengeForCurrentLecture != 0) {
                    // Better to use String since emulator will treat this value as string
                    byte [] number = String.valueOf(getChallengeNumberForCurrentLecture())
                            .getBytes("ISO-8859-1");
                    byte[] result = isoDep.transceive(number);
                    int resultLength = result.length;
                    byte[] payload = Arrays.copyOf(result, resultLength - 2);
                    byte[] encodedBytes = org.apache.commons.codec.binary.Base64.encodeBase64(payload);
                    responseToChallenge = new String(encodedBytes, "ISO-8859-1");
                }
                weakRefResponseToChallengeCallback.get().onResponseToChallengeReceived(responseToChallenge);
            } catch(IOException e) {
                Log.e(TAG, "Error communicating with card: " + e.toString());
            }
        }
    }

    private int getChallengeNumberForCurrentLecture() {
        SharedPreferences prefs = Attendance.getAppContext().getSharedPreferences(SHARED_PREFS_FILE, MODE_PRIVATE);
        Date current = Calendar.getInstance().getTime();
        String date = new SimpleDateFormat("yyyy-MM-dd").format(current);
        String time = new SimpleDateFormat("HH:00:00").format(current);

        if(prefs.getString(DATE_TIME_RANDOM_NUMBERS, null) == null) {
            return 0;
        }

        JSONObject dateTimeChallengeNumbersJson;
        int randomNumber;
        try {
            dateTimeChallengeNumbersJson =
                    new JSONObject(prefs.getString(DATE_TIME_RANDOM_NUMBERS, null));
            JSONObject todayJson = (JSONObject)dateTimeChallengeNumbersJson.get(date);
            randomNumber = (Integer)(todayJson.get(time));
        } catch (JSONException e) {
            Log.e(TAG, e.getMessage());
            return 0;
        }
        return randomNumber;
    }

    /**
     * The Following three methods taken from Google's repository are used to send
     * a command APDU. This is needed before any further communications can take
     * place with the card emulation service
     */
    // method taken from https://github.com/googlesamples/android-CardReader
    public static byte[] BuildSelectApdu(String aid) {
        // Format: [CLASS | INSTRUCTION | PARAMETER 1 | PARAMETER 2 | LENGTH | DATA]
        return HexStringToByteArray(SELECT_APDU_HEADER + String.format("%02X", aid.length() / 2) + aid);
    }

    // method taken from https://github.com/googlesamples/android-CardReader
    public static byte[] HexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }

    // method taken from https://github.com/googlesamples/android-CardReader
    public static String ByteArrayToHexString(byte[] bytes) {
        final char[] hexArray = {'0','1','2','3','4','5','6','7','8','9','A','B','C','D','E','F'};
        char[] hexChars = new char[bytes.length * 2];
        int v;
        for ( int j = 0; j < bytes.length; j++ ) {
            v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

}
