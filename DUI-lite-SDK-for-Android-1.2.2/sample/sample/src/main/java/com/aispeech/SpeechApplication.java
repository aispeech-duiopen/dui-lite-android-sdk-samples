package com.aispeech;

import android.app.Application;
import android.content.Context;



public class SpeechApplication extends Application {

	private static String TAG = "SpeechApplication";
	private static Context mContext;

	@Override
	public void onCreate() {
		super.onCreate();
		mContext = getApplicationContext();
	}

	public static Context getContext() {
		return mContext;
	}

}
