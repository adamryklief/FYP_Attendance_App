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

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import static android.content.Context.MODE_PRIVATE;

public class AsyncLogout extends AsyncTask<Void, Integer, Object> {
    private WeakReference<AttendanceActivity> weakRefAttendanceActivity;

    private static final String TAG = "AsyncLogin";
    private static final String URL_STUDENT_LOGOUT = "http://35.197.216.231/webapp/student-logout";
    private static final String SHARED_PREFS_FILE = "com.adam.fyp_attendance_app.DATA_STORE_FILE";
    private static final String AUTH_TOKEN = "auth_token";
    private static final String TOAST_MSG_KEY = "toastMessage";

    public AsyncLogout(AttendanceActivity context) {
        // safer to use weak reference in case the AsyncTask lives longer than the activity
        weakRefAttendanceActivity = new WeakReference<>(context);
    }

    @Override
    protected Object doInBackground(Void... voids) {
        try {
            SharedPreferences prefs = Attendance.getAppContext().getSharedPreferences(SHARED_PREFS_FILE, MODE_PRIVATE);
            String authToken = prefs.getString(AUTH_TOKEN, null);
            return logOut(authToken);
        } catch (IOException e) {
            Log.w(TAG, "doInBackground() failed: " + e.getMessage());
            return e.getMessage() + "\nPlease retry logging out"; // useless code...refactor
        }
    }

    @Override
    protected void onPostExecute(Object result) {
        AttendanceActivity attendanceActivity = weakRefAttendanceActivity.get();
        if(attendanceActivity == null || attendanceActivity.isFinishing())
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
            if(responseCode == 200) {
                SharedPreferences prefs = Attendance.getAppContext().getSharedPreferences(SHARED_PREFS_FILE, MODE_PRIVATE);
                SharedPreferences.Editor editor = prefs.edit();
                editor.remove(AUTH_TOKEN);
                editor.apply();
                Intent intent = new Intent(attendanceActivity, LoginActivity.class);
                intent.putExtra(TOAST_MSG_KEY, responseBody); // to display successful logout toast
                attendanceActivity.startActivity(intent);
                attendanceActivity.finish();
            } else if(responseCode == 500) {
                Toast.makeText(attendanceActivity.getApplicationContext(), responseBody, Toast.LENGTH_LONG).show();
            } else {
                String htmlTitle = responseBody.substring(responseBody.indexOf("<title>") + 7, responseBody.indexOf("</title>"));
                Toast.makeText(attendanceActivity.getApplicationContext(), htmlTitle, Toast.LENGTH_LONG).show();
            }
        } else {
            Toast.makeText(attendanceActivity.getApplicationContext(), result.toString(), Toast.LENGTH_LONG).show();
        }
    }

    private static Object logOut(String authToken) throws IOException {
        OkHttpClient okHttpClient = new OkHttpClient();

        RequestBody body = RequestBody.create(null, new byte[0]);
        Request request = new Request.Builder()
                .url(URL_STUDENT_LOGOUT)
                .addHeader("Authorization","Bearer " + authToken)
                .post(body)
                .build();

        Response response = okHttpClient.newCall(request).execute();

        Map<Integer, String> responseMap = new HashMap<>();
        responseMap.put(response.code(), response.body().string());

        return responseMap;
    }

}