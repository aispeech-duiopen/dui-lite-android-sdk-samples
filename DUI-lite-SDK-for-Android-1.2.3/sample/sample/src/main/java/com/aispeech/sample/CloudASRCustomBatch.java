/*******************************************************************************
 * Copyright 2017 AISpeech
 ******************************************************************************/
package com.aispeech.sample;

import android.app.Activity;
import android.content.res.AssetFileDescriptor;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;
import android.view.View;
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

import java.io.InputStream;

public class CloudASRCustomBatch extends Activity implements View.OnClickListener {

    final String Tag = this.getClass().getName();
    AICloudASREngine mEngine;

    TextView resultText;
    Button btnStart;
    Button btnStop;
    Toast mToast;

    HandlerThread mHandlerThread;
    Handler mHandler;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.asr);

        resultText = (TextView) findViewById(R.id.text_result);
        resultText.setText("正在加载资源...");
        btnStart = (Button) findViewById(R.id.btn_start);
        btnStop = (Button) findViewById(R.id.btn_end);

        btnStart.setEnabled(false);
        btnStop.setEnabled(false);
        btnStart.setOnClickListener(this);
        btnStop.setOnClickListener(this);

        mToast = Toast.makeText(this, "", Toast.LENGTH_LONG);

        initHandler();

        if (!AICloudASREngine.checkLibValid()) {
            mToast.setText("so加载失败");
            mToast.show();
        } else {
            mEngine = AICloudASREngine.createInstance();
            // 声明自行feed数据
            mEngine.setUseCustomFeed(true);
            mEngine.setLocalVadEnable(true);
            mEngine.setVadResource(SampleConstants.VAD_RES);
            mEngine.setPauseTime(500);
            mEngine.setServer("ws://asr.dui.ai/runtime/v2/recognize");//设置服务器地址，默认不用设置
            mEngine.setEnablePunctuation(false);//设置是否启用标点符号识别,默认为false关闭
            mEngine.setResourceType("comm");//设置识别引擎的资源类型,默认为comm
            mEngine.init(new AICloudASRListenerImpl());
            mEngine.setNoSpeechTimeOut(0);
            //mEngine.setMaxSpeechTimeS(0);//音频最大录音时长
            mEngine.setCloudVadEnable(true);//设置是否开启服务端的vad功能,默认开启为true
            mEngine.setSaveAudioPath("/sdcard/aispeech");//保存的音频路径,格式为.ogg
        }
    }

    static final int MSG_START_TEST = 0;
    static final int MSG_STOP_TEST = 1;
    static final int MSG_ERROR = 2;
    static final int MSG_RESULT = 3;

    private void initHandler() {
        mHandlerThread = new HandlerThread("batch_test_thread");
        mHandlerThread.start();
        mHandler = new Handler(mHandlerThread.getLooper()) {

            // 全局计数器
            int count = 0;

            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                switch (msg.what) {
                    case MSG_START_TEST:
                        // 启动测试音频
                        startTestWav(count);
                        showTip("测试开始");
                        break;
                    case MSG_STOP_TEST:
                        count = 0;
                        if (mEngine != null) {
                            mEngine.cancel();
                            showTip("测试结束");
                        }
                        break;
                    case MSG_ERROR:
                        AIError error = (AIError) msg.obj;
                        Log.e(Tag, "error:" + error.toString());
                        startTestWav(++count);
                        break;
                    case MSG_RESULT:
                        AIResult results = (AIResult) msg.obj;
                        Log.i(Tag, "result JSON = " + results.getResultObject().toString());
                        // 启动测试下一音频
                        startTestWav(++count);
                        break;
                    default:
                        break;
                }
            }
        };
    }

    @Override
    public void onClick(View v) {
        if (v == btnStart) {
            mHandler.sendEmptyMessage(MSG_START_TEST);
        } else if (v == btnStop) {
            mHandler.sendEmptyMessage(MSG_STOP_TEST);
        }
    }

    // 启动第index段音频的测试
    private void startTestWav(int index) {
        if(index >= 1)
            return;
        byte[] data = readWavFile(index);
        if (data != null && data.length > 0) {
            if (mEngine != null) {
                mEngine.start();
                byte[] buffer = new byte[3200];
                int i = 0;
                int j = 0;
                int dataLength = data.length;
                while (i < dataLength) {
                    j += 3200;
                    if (j > dataLength) {
                        j = dataLength;
                    }
                    System.arraycopy(data, i, buffer, 0, j - i);
                    // 自行feed数据
                    mEngine.feedData(buffer, j - i);
                    i += 3200;
                    try {
                        Thread.sleep(100);
                        if(mEngine == null) return;
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                mEngine.stopRecording();
            }

        } else {
            // 音频读完 ， 测试结束
            mHandler.sendEmptyMessage(MSG_STOP_TEST);
        }
    }

    // TODO 读取第index段音频,我这里用的是存放音频到assets目录,这里替换为自己的实现
    private byte[] readWavFile(int index) {
        try {
            Log.e("XXX", "read " + index + ".wav");
            InputStream is = getAssets().open(index + ".wav");
            AssetFileDescriptor fd = getAssets().openFd(index + ".wav");
            if (fd.getLength() != AssetFileDescriptor.UNKNOWN_LENGTH) {
                byte[] data = new byte[(int) fd.getLength()];
                is.read(data);
                return data;
            } else {
                throw new Exception("unknow file length");
            }
        } catch (Exception e) {
            Log.e(Tag, "Audio file not found in assets floder");
            e.printStackTrace();
        }
        return null;
    }

    private class AICloudASRListenerImpl implements AIASRListener {

        @Override
        public void onReadyForSpeech() {
        }

        @Override
        public void onBeginningOfSpeech() {
            resultText.append("检测到说话\n");
        }

        @Override
        public void onEndOfSpeech() {
            resultText.append("检测到语音停止，开始识别...\n");
        }

        @Override
        public void onRmsChanged(float rmsdB) {
            showTip("RmsDB = " + rmsdB);
        }

        @Override
        public void onError(AIError error) {
                resultText.append(error.toString() + "\n");
                Message.obtain(mHandler, MSG_ERROR, error).sendToTarget();
        }

        @Override
        public void onResults(AIResult results) {
            if (results.isLast()) {
                // 可以使用JSONResultParser来解析识别结果
                // 结果按概率由大到小排序
                JSONResultParser parser = new JSONResultParser(results.getResultObject().toString());
                resultText.append("text:" + parser.getText() + "\n");
                Message.obtain(mHandler, MSG_RESULT, results).sendToTarget();
            }
        }

        @Override
        public void onNotOneShot() {

        }

        @Override
        public void onInit(int status) {
            Log.i(Tag, "Init result " + status);
            if (status == AIConstant.OPT_SUCCESS) {
                resultText.setText("初始化成功!");
                btnStart.setEnabled(true);
                btnStop.setEnabled(true);
            } else {
                resultText.setText("初始化失败!code:" + status);
            }
        }


        @Override
        public void onResultDataReceived(byte[] buffer, int size) {

        }

        @Override
        public void onRawDataReceived(byte[] buffer, int size) {

        }


    }

    @Override
    public void onBackPressed() {
        this.finish();
        super.onBackPressed();
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
    public void onStop() {
        super.onStop();
        if (mEngine != null) {
            mEngine.cancel();
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

}