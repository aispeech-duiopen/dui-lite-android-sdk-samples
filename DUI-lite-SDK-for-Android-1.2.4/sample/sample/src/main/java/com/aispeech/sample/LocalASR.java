/*******************************************************************************

 * Copyright 2014 AISpeech
 ******************************************************************************/
package com.aispeech.sample;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.aispeech.AIError;
import com.aispeech.AIResult;
import com.aispeech.common.FileUtil;
import com.aispeech.export.engines.AILocalASREngine;
import com.aispeech.export.engines.AILocalGrammarEngine;
import com.aispeech.export.listeners.AIASRListener;
import com.aispeech.export.listeners.AILocalGrammarListener;
import com.aispeech.util.GrammarHelper;
import com.aispeech.util.SampleConstants;

import org.json.JSONException;
import org.json.JSONObject;

public class LocalASR extends Activity implements View.OnClickListener {

    final String TAG = this.getClass().getName();
    EditText tv;
    Button bt_res;
    Button bt_asr;
    Toast mToast;
    FileUtil mFileUtils;
    AILocalASREngine mAsrEngine;
    AILocalGrammarEngine mGrammarEngine;
    public static final String NET_BIN_PATH = "/sdcard/speech/local_asr.net.bin";//需要事先确保/sdcard/speech路径存在

    @SuppressLint("NewApi")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.grammar);

        tv = (EditText) findViewById(R.id.tv);
        bt_res = (Button) findViewById(R.id.btn_gen);
        bt_asr = (Button) findViewById(R.id.btn_asr);
        bt_res.setEnabled(false);
        bt_asr.setEnabled(false);
        bt_res.setOnClickListener(this);
        bt_asr.setOnClickListener(this);
        mToast = Toast.makeText(this, "", Toast.LENGTH_LONG);
        mFileUtils = new FileUtil(getApplicationContext());
        mFileUtils.createFileDir("/sdcard/speech/");//创建speech文件夹

        if (!AILocalGrammarEngine.checkLibValid() || !AILocalASREngine.checkLibValid()) {
            mToast.setText("so加载失败");
            mToast.show();
        } else {
            initGrammarEngine();
        }

    }


    private void initLocalAsr() {
        mAsrEngine = AILocalASREngine.createInstance();
        mAsrEngine.setResBin(SampleConstants.EBNFR_RES);
        mAsrEngine.setNetBinPath(NET_BIN_PATH);
        mAsrEngine.setUseConf(true);//识别结果返回阈值
        mAsrEngine.setUsePinyin(true);//识别结果返回拼音
        mAsrEngine.setUseXbnfRec(true);//识别结果返回语义信息
//        mAsrEngine.setUseRealBack(true);//设置开启实时反馈
        mAsrEngine.setVadEnable(true);
        mAsrEngine.setVadRes(SampleConstants.VAD_RES);
        mAsrEngine.setPauseTime(500);
        mAsrEngine.init(new AIASRListenerImpl());
        mAsrEngine.setSaveAudioPath("/sdcard/speech");
        mAsrEngine.setNoSpeechTimeOut(0);
        //mAsrEngine.setMaxSpeechTimeS(0);
    }


    /**
     * 识别引擎回调接口，用以接收相关事件
     */
    public class AIASRListenerImpl implements AIASRListener {

        @Override
        public void onBeginningOfSpeech() {
            showInfo("检测到说话");

        }

        @Override
        public void onEndOfSpeech() {
            showInfo("检测到语音停止，开始识别...");
        }

        @Override
        public void onReadyForSpeech() {
            showInfo("请说话...");
        }

        @Override
        public void onRmsChanged(float rmsdB) {
            showTip("RmsDB = " + rmsdB);
        }

        @Override
        public void onError(AIError error) {
//            showInfo("识别发生错误");
            Log.d(TAG, error.toString());
            showTip(error.toString());
            setAsrBtnState(false, "识别");
        }

        @Override
        public void onResults(AIResult results) {

            Log.i(TAG, results.getResultObject().toString());
            try {
                showInfo(new JSONObject(results.getResultObject().toString()).toString(4));
            } catch (JSONException e) {
                e.printStackTrace();
            }
            setAsrBtnState(true, "识别");
        }

        @Override
        public void onInit(int status) {
            if (status == 0) {
                Log.i(TAG, "end of init asr engine");
                showInfo("识别引擎加载成功");
                setResBtnEnable(true);
                setAsrBtnState(true, "识别");
            } else {
                showInfo("识别引擎加载失败");
            }
        }

        @Override
        public void onNotOneShot() {

        }

        @Override
        public void onResultDataReceived(byte[] buffer, int size) {

        }

        @Override
        public void onRawDataReceived(byte[] buffer, int size) {

        }

    }


    /**
     * 初始化资源编译引擎
     */
    private void initGrammarEngine() {
        if (mGrammarEngine != null) {
            mGrammarEngine.destroy();
        }
        mGrammarEngine = AILocalGrammarEngine.createInstance();
        mGrammarEngine.setRes(SampleConstants.EBNFC_RES);
        mGrammarEngine.init(new AILocalGrammarListenerImpl());
        //设置生成的net.bin文件路径，包含文件名
        mGrammarEngine.setOutputPath(NET_BIN_PATH);
    }
    


    public  class AILocalGrammarListenerImpl implements AILocalGrammarListener {

        @Override
        public void onError(AIError error) {
            showInfo("资源生成发生错误");
            showTip(error.getError());
            setResBtnEnable(true);
        }


        @Override
        public void onBuildCompleted(String path) {
            showInfo("资源生成/更新成功\npath=" + path + "\n重新加载识别引擎...");
            Log.i(TAG, "资源生成/更新成功\npath=" + path + "\n重新加载识别引擎...");
            initLocalAsr();
        }

        @Override
        public void onInit(int status) {
            if (status == 0) {
                showInfo("资源定制引擎加载成功");
                if (mAsrEngine == null) {
                    setResBtnEnable(true);
                }
            } else {
                showInfo("资源定制引擎加载失败");
            }
        }
    }

    @Override
    public void onClick(View view) {
        if (view == bt_res) {
            setResBtnEnable(false);
            setAsrBtnState(false, "识别");
            new Thread(new Runnable() {
                @Override
                public void run() {
                    showInfo("开始生成资源...");
                    startResGen();
                }
            }).start();
        } else if (view == bt_asr) {
            if ("识别".equals(bt_asr.getText())) {
                if (mAsrEngine != null) {
                    setAsrBtnState(true, "停止");
//                    mAsrEngine.setDynamicList("北京市,天安门博物馆");//更新热词,res资源(setResBin)必须需要使用ebnfr.dymc.0.1.0.bin，否则会崩溃
                    mAsrEngine.start();
                } else {
                    showTip("请先生成资源");
                }
            } else if ("停止".equals(bt_asr.getText())) {
                if (mAsrEngine != null) {
                    setAsrBtnState(true, "识别");
                    mAsrEngine.cancel();
                }
            }
        }
    }


    /**
     * 开始生成识别资源
     */

    private void startResGen() {
        // 生成ebnf语法
        GrammarHelper gh = new GrammarHelper(getApplicationContext());
        String contactString = gh.getConatcts();
        contactString = "";
        String appString = gh.getApps();
        // 如果手机通讯录没有联系人
        if (TextUtils.isEmpty(contactString)) {
            contactString = "无联系人";
        }
        String ebnf = gh.importAssets(contactString, "", "asr.xbnf");
        Log.i(TAG, ebnf);
        // 设置ebnf语法,并启动语法编译引擎，更新资源
        mGrammarEngine.buildGrammar(ebnf);
    }


    /**
     * 设置资源按钮的状态
     *
     * @param state
     *            使能状态
     */
    private void setResBtnEnable(final boolean state) {
        runOnUiThread(new Runnable() {

            @Override
            public void run() {
                bt_res.setEnabled(state);
            }
        });
    }

    /**
     * 设置识别按钮的状态
     *
     * @param state
     *            使能状态
     * @param text
     *            按钮文本
     */
    private void setAsrBtnState(final boolean state, final String text) {
        runOnUiThread(new Runnable() {

            @Override
            public void run() {
                bt_asr.setEnabled(state);
                bt_asr.setText(text);
            }
        });
    }
    private void showInfo(final String str) {
        runOnUiThread(new Runnable() {

            @Override
            public void run() {
                tv.setText(str);
            }
        });
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
        if (mAsrEngine != null) {
            mAsrEngine.cancel();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mAsrEngine != null) {
            mAsrEngine.destroy();
            mAsrEngine = null;
        }
        if (mFileUtils != null) {
            mFileUtils = null;
        }
    }

}
