/*******************************************************************************
 * Copyright 2014 AISpeech
 ******************************************************************************/
package com.aispeech.sample;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.aispeech.AIError;
import com.aispeech.common.AIConstant;
import com.aispeech.export.engines.AIWakeupEngine;
import com.aispeech.export.listeners.AIWakeupListener;
import com.aispeech.util.SampleConstants;

import org.json.JSONException;
import org.json.JSONObject;

public class LocalWakeup extends Activity implements View.OnClickListener {

    private static final String TAG = "LocalWakeup";
    AIWakeupEngine mEngine;
    TextView resultText;
    Button btnStart;
    Button btnStop;


    Toast mToast;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.asr);

        resultText = (TextView) findViewById(R.id.text_result);
        resultText.setText("语音唤醒演示:唤醒词是 你好小驰");
        btnStart = (Button) findViewById(R.id.btn_start);
        btnStop = (Button) findViewById(R.id.btn_end);

        btnStart.setEnabled(false);
        btnStop.setEnabled(false);
        btnStart.setOnClickListener(this);
        btnStop.setOnClickListener(this);
        mToast = Toast.makeText(this, "", Toast.LENGTH_LONG);

        if (!AIWakeupEngine.checkLibValid()) {
            mToast.setText("so加载失败");
            mToast.show();
        } else {
            mEngine = AIWakeupEngine.createInstance();
            mEngine.setWakeupWord(new String[]{"ni hao xiao chi"}, new String[]{"0.1"});
            mEngine.setResBin(SampleConstants.WAKEUP_RES);
            //mEngine.setResBinPath("/sdcard/aispeech/wakeup1.bin");//设置唤醒资源的绝对路径,包含文件名。默认在assets目录下，无需配置
            mEngine.init(new AISpeechListenerImpl());
        }
    }


    @Override
    public void onClick(View v) {
        if (v == btnStart) {
            mEngine.start();
            resultText.setText("语音唤醒演示:唤醒词是 你好小驰\n可以说话了");
        } else if (v == btnStop) {
            //如下注释的代码为动态设置唤醒env(当前只支持更新thresh)
//            JSONObject envJson = new JSONObject();
//            try {
//                envJson.put("env", "words=ni hao xiao chi;thresh=0.2;");
//            } catch (JSONException e) {
//                e.printStackTrace();
//            }
//            mEngine.set(envJson);
            mEngine.stop();
            resultText.setText("已取消！");
        }
    }

    private class AISpeechListenerImpl implements AIWakeupListener {

        @Override
        public void onError(AIError error) {
            showTip(error.toString());
        }

        @Override
        public void onInit(final int status) {
            Log.i(TAG, "Init result " + status);
            if (status == AIConstant.OPT_SUCCESS) {
                resultText.append("初始化成功!");
                btnStart.setEnabled(true);
                btnStop.setEnabled(true);
            } else {
                resultText.setText("初始化失败!code:" + status);
            }
        }


        @Override
        public void onWakeup(String recordId, final double confidence, final String wakeupWord) {
            Log.d(TAG, "wakeup foreground");
            resultText.append("\n唤醒成功  wakeupWord = " + wakeupWord + "  confidence = " + confidence
                    + "\n");
            //在这里启动其他引擎，比如tts或者识别
        }

        @Override
        public void onReadyForSpeech() {
            Log.d(TAG, "onReadyForSpeech: ");
        }


        @Override
        public void onResultDataReceived(byte[] buffer, int size) {

        }

        @Override
        public void onRawDataReceived(byte[] buffer, int size) {

        }

    }

    private void showTip(final String str) {
        mToast.setText(str);
        mToast.show();
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mEngine != null) {
            mEngine.stop();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mEngine != null) {
            mEngine.destroy();
            mEngine = null;
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (mEngine != null) {
                mEngine.stop();
                mEngine.destroy();
                mEngine = null;
            }
            finish();
        }
        return super.onKeyDown(keyCode, event);
    }
}
