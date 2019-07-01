package com.aispeech.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;


/**
 * Created by wuwei on 2018/7/16.
 */

public class LocalRegisterFeedSP {
    public static final String TAG = "LocalRegisterSP";
    private static LocalRegisterFeedSP mInstance;
    private static SharedPreferences mSharedPreferences;
    private LocalRegisterFeedSP(Context context) {
        mSharedPreferences = context.getSharedPreferences("feedRegisters", Context.MODE_PRIVATE);
    }

    public synchronized static LocalRegisterFeedSP getInstance(Context context) {
        if (mInstance == null) {
            mInstance = new LocalRegisterFeedSP(context);
        }
        return mInstance;
    }

    public void updateRegisters(String registeredStr) {
        Log.d(TAG, "update register to " + registeredStr);
        SharedPreferences.Editor updateEditor = mSharedPreferences.edit();
        updateEditor.putString("registeredStr", registeredStr);
        updateEditor.commit();
    }

    public String getRegisteredInfo() {
        return mSharedPreferences.getString("registeredStr", "");
    }

}
