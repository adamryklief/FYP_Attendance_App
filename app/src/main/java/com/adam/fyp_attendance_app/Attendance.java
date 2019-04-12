package com.adam.fyp_attendance_app;

import android.app.Application;
import android.content.Context;

public class Attendance extends Application {

    private static Context mContext;

    public void onCreate() {
        super.onCreate();
        mContext = getApplicationContext();
    }

    public static Context getAppContext() {
        return mContext;
    }
}
