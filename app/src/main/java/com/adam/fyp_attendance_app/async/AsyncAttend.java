package com.adam.fyp_attendance_app.async;

import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import com.adam.fyp_attendance_app.activity.AttendanceActivity;

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

public class AsyncAttend extends AsyncTask<Void, Integer, Object> {
    private static final String TAG = "AsyncAttend";
    private static final String URL_ATTENDANCE = "http://35.197.216.231/webapp/attendance";

    private WeakReference<AttendanceActivity> weakRefAttendanceActivity;
    private String encodedPayload;
    private String authToken;

    public AsyncAttend(AttendanceActivity context, String encodedPayload, String authToken) {
        // better to use weak reference in case the AsyncTask lives longer than the activity
        weakRefAttendanceActivity = new WeakReference<>(context);
        this.encodedPayload = encodedPayload;
        this.authToken = authToken;
    }

    @Override
    protected Object doInBackground(Void... voids) {
        try {
            return attend(encodedPayload, authToken);
        } catch (IOException | JSONException e) {
            Log.w(TAG, "doInBackground() failed: " + e.getMessage());
            return null;
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
            if(responseCode == 200 || responseCode == 403 || responseCode == 500) {
                Toast.makeText(attendanceActivity.getApplicationContext(), responseBody, Toast.LENGTH_LONG).show();
            } else {
                String htmlTitle = responseBody.substring(responseBody.indexOf("<title>") + 7, responseBody.indexOf("</title>"));
                Toast.makeText(attendanceActivity.getApplicationContext(), htmlTitle, Toast.LENGTH_LONG).show();
            }
        } else {
            Toast.makeText(attendanceActivity.getApplicationContext(),
                    "Unable to log your attendance. Please retry tapping-to-attend", Toast.LENGTH_LONG).show();
        }
    }

    private Object attend(String encodedPayload, String authToken) throws IOException, JSONException {
        OkHttpClient okHttpClient = new OkHttpClient();

        String mediaType = "application/json; charset=utf-8";
        JSONObject jsonObject = new JSONObject().put("encodedPayload", encodedPayload);

        RequestBody body = RequestBody.create(MediaType.parse(mediaType), jsonObject.toString());
        Request request = new Request.Builder()
                .url(URL_ATTENDANCE)
                .addHeader("Authorization","Bearer " + authToken)
                .post(body)
                .build();


        Response response = okHttpClient.newCall(request).execute();

        Map<Integer, String> responseMap = new HashMap<>();
        responseMap.put(response.code(), response.body().string());

        return responseMap;
    }
}
