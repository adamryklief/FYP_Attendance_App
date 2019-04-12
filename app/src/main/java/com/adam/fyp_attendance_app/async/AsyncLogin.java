package com.adam.fyp_attendance_app.async;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import com.adam.fyp_attendance_app.Attendance;
import com.adam.fyp_attendance_app.activity.AttendanceActivity;
import com.adam.fyp_attendance_app.activity.LoginActivity;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import static android.content.Context.MODE_PRIVATE;

public class AsyncLogin extends AsyncTask<Void, Integer, Object> {
    private WeakReference<LoginActivity> weakRefLoginActivity;
    private String studentId;
    private String imei;
    private String password;
    private String firebaseDeviceId;

    private static final String TAG = "AsyncLogin";
    private static final String URL_BASIC_AUTH = "http://35.197.216.231/webapp/basicauth";
    private static final String SHARED_PREFS_FILE = "com.adam.fyp_attendance_app.DATA_STORE_FILE";
    private static final String AUTH_TOKEN = "auth_token";

    public AsyncLogin(LoginActivity context, String studentId, String imei, String password, String firebaseDeviceId) {
        // safer to use weak reference in case the AsyncTask lives longer than the activity
        weakRefLoginActivity = new WeakReference<>(context);
        this.studentId = studentId;
        this.imei = imei;
        this.password = password;
        this.firebaseDeviceId = firebaseDeviceId;
    }

    @Override
    protected Object doInBackground(Void... voids) {
        try {
            return login(studentId, imei, password, firebaseDeviceId);
        } catch (IOException | JSONException e) {
            Log.w(TAG, "doInBackground() failed: " + e.getMessage());
            return e.getMessage() + "\nPlease retry logging in";
        }
    }

    @Override
    protected void onPostExecute(Object result) {
        LoginActivity loginActivity = weakRefLoginActivity.get();
        if(loginActivity == null || loginActivity.isFinishing())
            return;
        if(result instanceof Map) {
            Map<?,?> myMap = (Map)result;
            int responseCode = 0;
            String responseBody = "";
            for(Map.Entry<?, ?> entry : myMap.entrySet()) {
                if(entry.getKey() instanceof Integer)
                    responseCode = (Integer)entry.getKey();
                responseBody = entry.getValue().toString();
            }
            switch(responseCode) {
                case 200 :
                    storeTokenToPrefs(responseBody);
                    loginActivity.startActivity(new Intent(loginActivity, AttendanceActivity.class));
                    loginActivity.finish();
                    break;
                case 403 :
                    Toast.makeText(loginActivity.getApplicationContext(), responseBody, Toast.LENGTH_LONG).show();
                    break;
                case 401 :
                    Toast.makeText(loginActivity.getApplicationContext(), responseBody, Toast.LENGTH_LONG).show();
                    break;
                default :
                    String htmlTitle = responseBody.substring(responseBody.indexOf("<title>") + 7, responseBody.indexOf("</title>"));
                    Toast.makeText(loginActivity.getApplicationContext(), htmlTitle, Toast.LENGTH_LONG).show();
            }
        } else {
            Toast.makeText(loginActivity.getApplicationContext(), result.toString(), Toast.LENGTH_LONG).show();
        }
    }

    private static Object login(String studentId, String imei, String password, String firebase_device_id)
            throws IOException, JSONException {
        OkHttpClient okHttpClient = new OkHttpClient();

        String mediaType = "application/json; charset=utf-8";
        JSONObject jsonObject = new JSONObject().put("student_id", studentId).put("imei", imei)
                .put("password", password).put("firebase_device_id", firebase_device_id);

        RequestBody body = RequestBody.create(MediaType.parse(mediaType), jsonObject.toString());
        Request request = new Request.Builder()
                .url(URL_BASIC_AUTH)
                .post(body)
                .build();
        Response response = okHttpClient.newCall(request).execute();

        Map<Integer, String> responseMap = new HashMap<>();
        responseMap.put(response.code(), response.body().string());

        return responseMap;
    }

    private void storeTokenToPrefs(String jsonTokenString) {
        String tokenValue;
        try {
            tokenValue = new JSONObject(jsonTokenString).getString(AUTH_TOKEN);
            SharedPreferences prefs = Attendance.getAppContext().getSharedPreferences(SHARED_PREFS_FILE, MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString(AUTH_TOKEN, tokenValue);
            editor.apply();
        } catch (JSONException e) {
            Log.e(TAG, "storeTokenToPrefs() failed: " + e.getMessage());
        }
    }

}