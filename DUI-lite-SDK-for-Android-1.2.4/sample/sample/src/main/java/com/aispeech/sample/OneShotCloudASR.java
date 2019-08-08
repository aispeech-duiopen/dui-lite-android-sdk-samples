package com.aispeech.sample;

import android.app.Activity;
import android.media.AudioManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.EditText;
import android.widget.Toast;

import com.aispeech.AIError;
import com.aispeech.AIResult;
import com.aispeech.common.AIConstant;
import com.aispeech.common.JSONResultParser;
import com.aispeech.export.engines.AICloudASREngine;
import com.aispeech.export.engines.AILocalTTSEngine;
import com.aispeech.export.engines.AIWakeupEngine;
import com.aispeech.export.listeners.AIASRListener;
import com.aispeech.export.listeners.AILocalTTSListener;
import com.aispeech.export.listeners.AIWakeupListener;
import com.aispeech.util.SampleConstants;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class OneShotCloudASR  extends Activity {
	static final String TAG = "OneShotCloudASR";
	private EditText mEt;
	private AIWakeupEngine mWakeupEngine;
	private AILocalTTSEngine mTtsEngine;
	private AICloudASREngine mAsrEngine;
    Toast mToast;
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.oneshot);
        mEt = (EditText) this.findViewById(R.id.text_result);
        mToast = Toast.makeText(this, "", Toast.LENGTH_LONG);
        initAsrEngine();
	}
	
    private void initAsrEngine() {
        if (!AICloudASREngine.checkLibValid()) {
            mToast.setText("vad so加载失败");
            mToast.show();
            return;
        }
        if (mAsrEngine != null) {
            mAsrEngine.destroy();
            mAsrEngine = null;
        }
        mAsrEngine = AICloudASREngine.createInstance();
        mAsrEngine.setServer("wss://asr.dui.ai/runtime/v2/recognize");//设置服务器地址，默认不用设置
        mAsrEngine.setEnablePunctuation(false);//设置是否启用标点符号识别,默认为false关闭
        mAsrEngine.setResourceType("aihome");//设置识别引擎的资源类型,默认为comm
        mAsrEngine.setLocalVadEnable(true);
        mAsrEngine.setVadResource(SampleConstants.VAD_RES);
        mAsrEngine.setPauseTime(500);
        mAsrEngine.init(new AIASRListenerImpl());
        mAsrEngine.setNoSpeechTimeOut(0);
        mAsrEngine.setMaxSpeechTimeS(0);//音频最大录音时长
        mAsrEngine.setCloudVadEnable(true);//设置是否开启服务端的vad功能,默认开启为true
        mAsrEngine.setSaveAudioPath("/sdcard/aispeech");//保存的音频路径,格式为.ogg
        JSONArray jsonArray = new JSONArray();
        jsonArray.put("你好小乐");
        mAsrEngine.setCustomWakeupWord(jsonArray);
        mAsrEngine.setWakeupWordVisible(true);
        mAsrEngine.setOneshotOptimization(true);
    }
    
    
	private void initWakeupEngine() {
	    if (!AIWakeupEngine.checkLibValid()) {
            mToast.setText("wakeup so加载失败");
            mToast.show();
            return;
        }
		mWakeupEngine = AIWakeupEngine.createInstance(); //创建实例
        mWakeupEngine.setWakeupWord(new String[]{"ni hao xiao le"}, new String[]{"0.1"});
        mWakeupEngine.setResBin(SampleConstants.WAKEUP_RES); //非自定义唤醒资源可以不用设置words和thresh，资源已经自带唤醒词
        mWakeupEngine.setOneShotCacheTime(1200);
        mWakeupEngine.init(new AIWakeupListenerImpl());
	}
	
	private void startWakeup() {
		mWakeupEngine.setUseOneShotFunction(true);//是否使用oneshot功能
		mWakeupEngine.start();
	}
	
	private void startAsrUsingOneShot() {
		//这里启动带有oneshot功能的识别引擎，该引擎如果检测到连说（唤醒词+命令词），
    	//那么就会直接在唤醒引擎的onResult回调中输出结果，如果检测到不是连说（只有唤醒词），那么会回调onNotOneShot(),用户可以在onNotOneShot()兼容老版本的内容
		mAsrEngine.notifyWakeup();
    	mAsrEngine.setPauseTime(0);//唤醒后pauseTime设为0 ,这里为了加快判断是否是连着说
    	mAsrEngine.setOneShotIntervalTimeThreshold(600); //默认为600，这个值和上面的pauseTime的差值可以根据实际情况调整
    	//如果想增加连说之间的间隔，请同时增加上面两个值，比如同时加500ms，能体验到唤醒词和命令词之间的间隔可以很长
    	mAsrEngine.setUseOneShotFunction(true);
    	mAsrEngine.start();
	}
	
	private void startAsrNotUsingOneShot() {
		//启动不带oneshot功能的识别引擎，这里onNotOneShot()不会被回调，这个只是单纯的识别，和老版本一样
		mAsrEngine.setPauseTime(300); //合成后起的识别pauseTime设为300或其他
    	mAsrEngine.setUseOneShotFunction(false);
		mAsrEngine.start();
	}

    private void initTtsEngine() {
        if (!AILocalTTSEngine.checkLibValid()) {
            mToast.setText("cntts so加载失败");
            mToast.show();
            return;
        }
        if (mTtsEngine != null) {
            mTtsEngine.destroy();
        }
        mTtsEngine = AILocalTTSEngine.getInstance();//单例模式
        mTtsEngine.setFrontResBin(SampleConstants.TTS_FRONT_RES, SampleConstants.TTS_FRONT_RES_MD5);//设置assets目录下合成音资源名和相应的Md5文件名
        mTtsEngine.setDictDb(SampleConstants.TTS_DICT_RES, SampleConstants.TTS_DICT_MD5);//设置assets目录下合成字典名和相应的Md5文件名
        mTtsEngine.setBackResBinArray(new String[] {SampleConstants.TTS_BACK_RES_ZHILING});
        mTtsEngine.init(new AILocalTTSListenerImpl());//初始化合成引擎
        mTtsEngine.setSpeechRate(0.85f);//设置合成音语速，范围为0.5～2.0
        mTtsEngine.setStreamType(AudioManager.STREAM_MUSIC);//设置audioTrack的播放流，默认为music
        mTtsEngine.setUseSSML(false);//设置是否使用ssml合成语法，默认为false
        mTtsEngine.setSpeechVolume(100);//设置合成音频的音量，范围为0～500
    }
    
    
    /**
     * 识别引擎回调接口，用以接收相关事件
     */
    public class AIASRListenerImpl implements AIASRListener {

        @Override
        public void onBeginningOfSpeech() {

        }

        @Override
        public void onEndOfSpeech() {
        }

        @Override
        public void onRawDataReceived(byte[] bytes, int i) {

        }

        @Override
        public void onResultDataReceived(byte[] bytes, int i) {

        }

        @Override
        public void onReadyForSpeech() {
        }

        @Override
        public void onRmsChanged(float rmsdB) {
        }

        @Override
        public void onError(AIError error) {
        	mEt.setText(error.toString());
        }

        @Override
        public void onResults(AIResult results) {
            JSONResultParser parser = new JSONResultParser(results.getResultObject().toString());
            String input = parser.getText();
            boolean isWakeupWord = TextUtils.equals(input, "你好小乐") ||
                    TextUtils.equals(input, "^");//云端识别结果为^，表示是唤醒词
            if (isWakeupWord) {
                Log.e(TAG, "not one shot");
                mTtsEngine.speak("有什么可以帮您", "1024");
            } else {
                try {
                    mEt.append(new JSONObject(results.getResultObject()
                            .toString()).toString(4));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }

        @Override
        public void onInit(int status) {
            if (status == 0) {
            	mEt.append("识别引擎初始化成功!\n");
            	initWakeupEngine();
                initTtsEngine();
            } else {
            	mEt.setText("识别引擎初始化失败\n");
            }
        }


		@Override
		public void onNotOneShot() {
			Log.e(TAG, "not one shot");
			mEt.setText("唤醒成功\n");
			mTtsEngine.speak("有什么可以帮您", "1024");
		}
    }
    
    
	/**
	 * 唤醒回调接口
	 * */
	private class AIWakeupListenerImpl implements AIWakeupListener {

        @Override
        public void onError(AIError error) {
        	mEt.setText(error.toString());
        }

        @Override
        public void onInit(int status) {
            if (status == AIConstant.OPT_SUCCESS) {
            	mEt.append("唤醒引擎初始化成功!\n");
            } else {
            	mEt.setText("初始化失败!code:" + status);
            }
        }

        @Override
        public void onWakeup(String recordId, double confidence, String wakeupWord) {
        	mEt.setText(""); //原本非oneshot中的唤醒后的一些逻辑请移到onNotOneShot()中做
        	startAsrUsingOneShot();//这里启动带有oneshot功能的识别引擎，该引擎如果检测到连说（唤醒词+命令词），
        	//那么就会直接在唤醒引擎的onResult回调中输出结果，如果检测到不是连说（只有唤醒词），那么会回调onNotOneShot(),用户可以在onNotOneShot()兼容老版本的内容
        }

        @Override
        public void onReadyForSpeech() {
        	mEt.append("你可以说你好小乐来唤醒 或者直接说 你好小乐+命令词\n");
        }

        @Override
        public void onRawDataReceived(byte[] bytes, int i) {

        }

        @Override
        public void onResultDataReceived(byte[] bytes, int i) {

        }

    }
	
	/**
	 * 合成回调接口
	 * */
    private class AILocalTTSListenerImpl implements AILocalTTSListener {

        @Override
        public void onInit(int status) {
            if (status == AIConstant.OPT_SUCCESS) {
                mEt.append("合成引擎初始化成功!\n");
                startWakeup();
            } else {
                mEt.setText("初始化失败!code:" + status);
            }
        }

        @Override
        public void onError(String utteranceId, AIError error) {
            mEt.setText(error.toString());
        }

        @Override
        public void onSynthesizeStart(String utteranceId) {
            Log.d(TAG, "合成开始");
        }

        @Override
        public void onSynthesizeDataArrived(String utteranceId, byte[] audioData) {
            //Log.d(Tag, "合成pcm音频数据:" + audioData.length);
            //正常合成结束后会收到size大小为0的audioData,即audioData.length == 0。应用层可以根据该标志停止播放
            //若合成过程中取消(stop或release)，则不会收到该结束标志
        }

        @Override
        public void onSynthesizeFinish(String utteranceId) {
            Log.d(TAG, "合成结束");
        }

        @Override
        public void onSpeechStart(String utteranceId) {
            Log.i(TAG, "开始播放");
        }

        @Override
        public void onSpeechProgress(int currentTime, int totalTime, boolean isRefTextTTSFinished) {
            Log.i(TAG,"当前:" + currentTime + "ms, 总计:" + totalTime + "ms, 可信度:" + isRefTextTTSFinished);
        }

        @Override
        public void onSpeechFinish(String utteranceId) {
            startAsrNotUsingOneShot();
            Log.i(TAG, "播放完成");
        }
    }
    
    
	
	@Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(mWakeupEngine != null) {
        	mWakeupEngine.destroy();
        }
        if(mTtsEngine != null) {
        	mTtsEngine.destroy();
        }
        if(mAsrEngine != null) {
        	mAsrEngine.destroy();
        }
    }

	@Override
	public void onBackPressed() {
		finish();
	}
}

