package com.adam.fyp_attendance_app.firebase;

import android.content.SharedPreferences;
import android.util.Log;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;

public class AttendanceFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "AttendanceFbMsgService";
    private static final String SHARED_PREFS_FILE = "com.adam.fyp_attendance_app.DATA_STORE_FILE";
    private static final String DATE_TIME_RANDOM_NUMBERS = "dateTimeRandomNumbers";
    private static final SimpleDateFormat MYSQL_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        if (remoteMessage.getData().size() > 0) {
            SharedPreferences prefs = getSharedPreferences(SHARED_PREFS_FILE, MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();

            for (Map.Entry<String, String> entry : remoteMessage.getData().entrySet()) {
                try {
                    JSONObject jsonObjNewDate = new JSONObject(entry.getValue());
                    JSONObject jsonObjNewRandomChallenge = new JSONObject().put(entry.getKey(), jsonObjNewDate);

                    // if prefs file has no value for DATE_TIME_RANDOM_NUMBERS key, then create a new
                    // key value pair with the new jsonObjNewRandomChallenge object
                    if(prefs.getString(DATE_TIME_RANDOM_NUMBERS, null) == null) {
                        editor.putString(DATE_TIME_RANDOM_NUMBERS, jsonObjNewRandomChallenge.toString());
                        editor.apply();
                    } else {
                        JSONObject jsonObjStoredDate = new JSONObject(prefs.getString(DATE_TIME_RANDOM_NUMBERS, null));
                        for(Iterator iterator = jsonObjStoredDate.keys(); iterator.hasNext();) {
                            String storedDateStr = (String)iterator.next();
                            Date storedDate = getDateFromString(storedDateStr);
                            Date newDate = getDateFromString(entry.getKey());
                            if(storedDate.equals(newDate)) {
                                JSONObject todaysDate = new JSONObject(jsonObjStoredDate.get(storedDateStr).toString());
                                for(Iterator timeRandomNumberIterator = jsonObjNewDate.keys(); timeRandomNumberIterator.hasNext();) {
                                    String randomNumberTime = (String)timeRandomNumberIterator.next();
                                    todaysDate.put(randomNumberTime,jsonObjNewDate.get(randomNumberTime));
                                    jsonObjStoredDate.put(storedDateStr, todaysDate);
                                    editor.putString(DATE_TIME_RANDOM_NUMBERS, jsonObjStoredDate.toString());
                                    editor.apply();
                                }
                             // push messages may arrive out of order, so only the most recently dated ones
                             // should be processed
                            } else if(storedDate.before(newDate)){
                                // this is for a new day so recreate the the whole "dateTimeRandomNumbers" value
                                editor.putString(DATE_TIME_RANDOM_NUMBERS, jsonObjNewRandomChallenge.toString());
                                editor.apply();
                            }
                        }
                    }
                } catch (JSONException e) {
                    Log.e(TAG, e.getMessage());
                    return;
                }
            }
        }
    }

    private Date getDateFromString(String dateStr) {
        Date date = new Date();
        try {
            date =  MYSQL_DATE_FORMAT.parse(dateStr);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return date;
    }

}