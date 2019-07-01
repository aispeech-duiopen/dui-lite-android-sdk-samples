/*******************************************************************************
 * Copyright 2014 AISpeech
 ******************************************************************************/
package com.aispeech.sample;

import android.app.Activity;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.aispeech.AIError;
import com.aispeech.common.AIConstant;
import com.aispeech.export.engines.AILocalTTSEngine;
import com.aispeech.export.listeners.AILocalTTSListener;
import com.aispeech.util.SampleConstants;

import java.util.ArrayList;
import java.util.List;


public class LocalTTS extends Activity implements OnClickListener ,AdapterView.OnItemSelectedListener {

        final String CN_PREVIEW ="很难想象有什么事物会像廉价、强大、无处不在的人工智能，那样拥有“改变一切”的力量。" +
            "未来，我们的日常行为将被彻底改变。";
    final String Tag = this.getClass().getName();


    TextView tip;
    EditText content;
    Button btnStart, btnPlayerPause, btnPlayerResume, btnPlayerStop;

    Toast mToast;

    AILocalTTSEngine mEngine;
    Spinner spinner_res;

    private String[] mBackResBinArray = new String[] {SampleConstants.TTS_BACK_RES_ZHILING,
            SampleConstants.TTS_BACK_RES_LUCY, SampleConstants.TTS_BACK_RES_XIJUN};

    private String[] mBackResBinMd5sumArray = new String[] {SampleConstants.TTS_BACK_RES_ZHILING_MD5,
            SampleConstants.TTS_BACK_RES_LUCY_MD5, SampleConstants.TTS_BACK_RES_XIJUN_MD5};

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.local_tts);

        tip = (TextView) findViewById(R.id.tip);
        content = (EditText) findViewById(R.id.content);
        btnStart = (Button) findViewById(R.id.btn_start);
        btnPlayerPause = (Button) findViewById(R.id.btn_pause);
        btnPlayerResume = (Button) findViewById(R.id.btn_resume);
        btnPlayerStop = (Button) findViewById(R.id.btn_stop);
        spinner_res = (Spinner) findViewById(R.id.spinner_local_tts_res);
        spinner_res.setOnItemSelectedListener(this);
        initSpinnerData();
        content.setText(CN_PREVIEW);

        btnStart.setEnabled(false);
        btnPlayerPause.setOnClickListener(this);
        btnPlayerResume.setOnClickListener(this);
        btnPlayerStop.setOnClickListener(this);
        btnStart.setOnClickListener(this);

        mToast = Toast.makeText(this, "", Toast.LENGTH_LONG);
        if (!AILocalTTSEngine.checkLibValid()) {
            mToast.setText("so加载失败");
            mToast.show();
        } else {
            initEngine();
        }
    }

    private void initSpinnerData(){
        List<String> backRes = new ArrayList<>();
        for (String res : mBackResBinArray) {
            backRes.add(res);
        }
        ArrayAdapter<String> spinnerBackRes = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_dropdown_item, backRes);
        spinner_res.setAdapter(spinnerBackRes);
        spinner_res.setSelection(0);
    }

    private void initEngine() {
        if (mEngine != null) {
            mEngine.destroy();
        }
        mEngine = AILocalTTSEngine.getInstance();//单例模式
        mEngine.setFrontResBin(SampleConstants.TTS_FRONT_RES, SampleConstants.TTS_FRONT_RES_MD5);//设置assets目录下前端合成资源名和相应的Md5文件名
        mEngine.setDictDb(SampleConstants.TTS_DICT_RES, SampleConstants.TTS_DICT_MD5);//设置assets目录下合成字典名和相应的Md5文件名
        mEngine.setBackResBinArray(mBackResBinArray, mBackResBinMd5sumArray);//设置后端合成音色资源，如果只需设置一个，则array只需要传一个成员值就可以，init前设置setBackResBin接口无效
//        mEngine.setFrontResBinPath("/sdcard/speech/tts/local_front.bin");//设置合成前端资源的外部路径，包含文件名(需要手动拷贝到指定目录)
//        mEngine.setDictDbPath("/sdcard/speech/tts/aitts_sent_dict_idx_middle_2.0.4_20180806.db");//设置合成字典的外部路径，包含文件名(需要手动拷贝到指定目录)
//        mEngine.setBackResBinPath("/sdcard/speech/tts/zhilingf_common_back_ce_local.v2.1.0.bin");//设置合成音色的外部路径，包含文件名(需要手动拷贝到指定目录)
        mEngine.init(new AILocalTTSListenerImpl());//初始化合成引擎
        mEngine.setSpeechRate(0.85f);//设置合成音语速，范围为0.5～2.0
        mEngine.setStreamType(AudioManager.STREAM_MUSIC);//设置audioTrack的播放流，默认为music
        mEngine.setUseSSML(false);//设置是否使用ssml合成语法，默认为false
        mEngine.setSpeechVolume(100);//设置合成音频的音量，范围为1～500
        mEngine.setUseCache(true); // default is true
    }


    @Override
    public void onClick(View v) {
        if (v == btnStart) {
            String refText = content.getText().toString();
            if (!TextUtils.isEmpty(refText)) {
                if (mEngine != null) {
                    mEngine.setSaveAudioFileName(Environment.getExternalStorageDirectory() + "/tts/"
                            + System.currentTimeMillis() + ".wav");//保存合成音频到指定路径，格式为wav
                    mEngine.speak(refText, "1024");//合成并播放
                    //mEngine.synthesize(refText, "1024");//合成音频，不播放，同时输出实时pcm音频,音频回调在onSynthesizeDataArrived接口
//                    mEngine.synthesizeToFile(refText, Environment.getExternalStorageDirectory() + "/tts/"
//                            + System.currentTimeMillis() + ".wav", "1024");//合成音频并保存音频到指定路径
                }
                tip.setText("正在合成...");
            } else {
                tip.setText("没有合法文本");
            }
        } else if (v == btnPlayerPause) {
            tip.setText("播放已暂停");
            if (mEngine != null) {
                mEngine.pause();
            }

        } else if (v == btnPlayerResume) {
            tip.setText("恢复播放");
            if (mEngine != null) {
                mEngine.resume();
            }

        } else if (v == btnPlayerStop) {
            tip.setText("合成已停止");
            if (mEngine != null) {
                mEngine.stop();
            }
        }
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        Log.d(Tag, "select res: " + spinner_res.getSelectedItem().toString());
        if (mEngine != null) {
            mEngine.setBackResBin(spinner_res.getSelectedItem().toString());
            //动态设置assets目录下发音人资源名，只适用于在合成前调用，init前调用无效。且设置的资源名在init前setBackResBinArray已经设置过
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }

    private class AILocalTTSListenerImpl implements AILocalTTSListener {

        @Override
        public void onInit(int status) {
            Log.i(Tag, "初始化完成，返回值：" + status);
            Log.i(Tag, "onInit");
            if (status == AIConstant.OPT_SUCCESS) {
                tip.setText("初始化成功!");
                btnStart.setEnabled(true);
            } else {
                tip.setText("初始化失败!code:" + status);
            }
        }

        @Override
        public void onError(String utteranceId, AIError error) {
            tip.setText("检测到错误");
            content.setText(content.getText() + "\nError:\n" + error.toString());
        }

        @Override
        public void onSynthesizeStart(String utteranceId) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    tip.setText("合成开始");
                    Log.d(Tag, "合成开始");
                }
            });

        }

        @Override
        public void onSynthesizeDataArrived(String utteranceId, byte[] audioData) {
            //Log.d(Tag, "合成pcm音频数据:" + audioData.length);
            //正常合成结束后会收到size大小为0的audioData,即audioData.length == 0。应用层可以根据该标志停止播放
            //若合成过程中取消(stop或release)，则不会收到该结束标志
        }

        @Override
        public void onSynthesizeFinish(String utteranceId) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    tip.setText("合成结束");
                    Log.d(Tag, "合成结束");
                }
            });
        }

        @Override
        public void onSpeechStart(String utteranceId) {
            tip.setText("开始播放");
            Log.i(Tag, "开始播放");
        }

        @Override
        public void onSpeechProgress(int currentTime, int totalTime, boolean isRefTextTTSFinished) {
            showTip("当前:" + currentTime + "ms, 总计:" + totalTime + "ms, 可信度:" + isRefTextTTSFinished);
        }

        @Override
        public void onSpeechFinish(String utteranceId) {
            tip.setText("播放完成");
            Log.i(Tag, "播放完成");
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
            Log.i(Tag, "release in LocalTTS");
            mEngine.destroy();
            mEngine = null;
        }
    }

}
