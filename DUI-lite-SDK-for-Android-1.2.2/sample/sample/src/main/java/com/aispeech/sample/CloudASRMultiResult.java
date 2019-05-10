/*******************************************************************************
 * Copyright 2017 AISpeech
 ******************************************************************************/
package com.aispeech.sample;

import android.app.Activity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.aispeech.AIError;
import com.aispeech.AIResult;
import com.aispeech.common.AIConstant;
import com.aispeech.common.JSONResultParser;
import com.aispeech.export.engines.AICloudASREngine;
import com.aispeech.export.listeners.AIASRListener;
import com.aispeech.util.SampleConstants;

public class CloudASRMultiResult extends Activity implements OnClickListener {

    public static final String TAG = CloudASRMultiResult.class.getCanonicalName();

    AICloudASREngine mEngine;

    TextView textResult = null;
    Button btnStart;
    Button btnStop;
    Toast mToast;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.asr);

        textResult = (TextView) findViewById(R.id.text_result);
        btnStart = (Button) findViewById(R.id.btn_start);
        btnStop = (Button) findViewById(R.id.btn_end);
        btnStart.setEnabled(false);
        btnStop.setEnabled(false);
        btnStart.setOnClickListener(this);
        btnStop.setOnClickListener(this);

        if (!AICloudASREngine.checkLibValid()) {
            mToast.setText("so加载失败");
            mToast.show();
        } else {
            mEngine = AICloudASREngine.createInstance();
            mEngine.setRealback(true);//打开实时反馈功能
            mEngine.setLocalVadEnable(true);
            mEngine.setVadResource(SampleConstants.VAD_RES);
            mEngine.setPauseTime(500);
            mEngine.setServer("ws://asr.dui.ai/runtime/v2/recognize");//设置服务器地址，默认不用设置
            mEngine.setResourceType("custom");//默认为comm,若设置为custom,则须设置lmId
            mEngine.setLmId("custom-lm-id");//客户自定义lmId
            mEngine.init(new AIASRListenerImpl());
            mToast = Toast.makeText(this, "", Toast.LENGTH_LONG);
        }
    }

    @Override
    public void onClick(View v) {
        if (v == btnStart) {
            mEngine.start();
        } else if (v == btnStop) {
            mEngine.cancel(); //关闭录音机，取消本次识别
        }
    }

    private class AIASRListenerImpl implements AIASRListener {

        StringBuilder textSb = new StringBuilder("[text]:");

        public void onError(AIError error) {
            textResult.setText(error.toString());
        }

        public void onResults(AIResult results) {
            if (results.getResultType() == AIConstant.AIENGINE_MESSAGE_TYPE_JSON) {
                Log.i(TAG, results.getResultObject().toString());
                JSONResultParser parser = new JSONResultParser((String) results.getResultObject());
                String text = parser.getText();
                String var = parser.getVar();
                if (!TextUtils.isEmpty(text)) {
                    textSb.append(text);
                }
                StringBuilder varSb = new StringBuilder("[var]:");
                if (!TextUtils.isEmpty(var)) {
                    varSb.append(var);
                }
                StringBuilder sb = new StringBuilder();
                sb.append(textSb);
                sb.append("\n");
                sb.append(varSb);
                textResult.setText(sb.toString());
            }
        }

        @Override
        public void onInit(int status) {
            if (status == AIConstant.OPT_SUCCESS) {
                textResult.setText("初始化成功!");
                btnStart.setEnabled(true);
                btnStop.setEnabled(true);
            } else {
                textResult.setText("初始化失败!code:" + status);
            }
        }

        @Override
        public void onBeginningOfSpeech() {
            //本地vad打开时，才会执行
            showTip("检测到说话");
        }

        @Override
        public void onNotOneShot() {

        }

        @Override
        public void onEndOfSpeech() {
            //本地vad打开时，才会执行
            showTip("检测到语音停止，开始识别...");
        }

        @Override
        public void onReadyForSpeech() {
            showTip("请说话");
            textResult.setText("请说话");
        }

        @Override
        public void onRmsChanged(float rmsdB) {
//            showTip("RmsDB = " + rmsdB);
            Log.d(TAG,""+ rmsdB);
        }

        @Override
        public void onResultDataReceived(byte[] buffer, int size) {

        }

        @Override
        public void onRawDataReceived(byte[] buffer, int size) {

        }

    }
    
    private void showTip(final String str) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mToast.setText(str);
                mToast.show();
            }
        });
    }

    @Override
    public void onBackPressed() {
        this.finish();
        super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        if (mEngine != null) {
            mEngine.destroy();
        }
        super.onDestroy();
    }

}
