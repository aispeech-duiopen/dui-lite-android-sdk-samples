package com.aispeech.sample;

import android.app.Activity;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.MediaRecorder;
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

import org.json.JSONException;
import org.json.JSONObject;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class OneShotCustomFeedDemo  extends Activity {
	static final String TAG = "OneShotCustomFeedDemo";
	private EditText mEt;
	private AIWakeupEngine mWakeupEngine;
	private AILocalTTSEngine mTtsEngine;
	private AICloudASREngine mAsrEngine;
    private ExecutorService mPool;
    Toast mToast;
    
    private long mVadEndTime = 0; //说话结束时刻
    
    private boolean mIsWakeupEngineWorking = false;
    private boolean mIsAsrEngineWorking = false;
    private boolean mIsNeedCaching = false; //是否需要缓存唤醒点之前的音频（用于oneshot）
    private boolean mIsNeedFeedCachingDataToAsr = false; //是否需要把缓存的音频送往识别
    private AudioRecord mAudioRecorder;
    private static final int INTERVAL_TIME_OF_ONE_SHOT_THRESH = 600;//sdk内部判断是否为oneshot的阈值时间（ms），（vad结束的时间-唤醒点的时间）小于该值认为不是oneshot,则播放“有什么可以帮您”，大于该值认为是oneshot，直接进行识别的指令，该值根据设置的pausetime等真实环境进行调节
    private static final int CACHETIME = 1200; //cache的音频（唤醒点之前的音频）时长1.2s ,根据具体的硬件性能和唤醒词长度调节
    private static final int INTERVAL = 100; // read buffer interval in ms.
    private static final int QUEUESIZE = CACHETIME/INTERVAL;//缓存音频队列的大小
    private static final int AUDIO_CHANNEL = AudioFormat.CHANNEL_IN_MONO; //单声道
    private static int audio_channel_num = (AUDIO_CHANNEL == AudioFormat.CHANNEL_IN_STEREO) ? 2 : 1;
    private static int audio_encoding = AudioFormat.ENCODING_PCM_16BIT;
    private static int sample_rate = 16000;
    private Queue<byte[]> mQueue = new LinkedList<byte[]>(); //用于缓存唤醒音频，然后在唤醒后，先把该音频（你好小乐）送进识别引擎，然后再把正常的录音（比如来首歌）送进识别引擎，来判断用户是不是在连说
    private Lock mLock = new ReentrantLock();
    private volatile Boolean mIsRecording = false;//标记收否正在录音
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.oneshot);
        mEt = (EditText) this.findViewById(R.id.text_result);
        mToast = Toast.makeText(this, "", Toast.LENGTH_LONG);
        initAudioRecorder();
        initAsrEngine();
	}
	
	 private static int calc_buffer_size() {
         int bufferSize = sample_rate * audio_channel_num *  audio_encoding;
         int minBufferSize = AudioRecord.getMinBufferSize(sample_rate, AUDIO_CHANNEL,
        		 audio_encoding);

         if (minBufferSize > bufferSize) {
             int inc_buffer_size = bufferSize * 4; // 4s
             // audio
             if (inc_buffer_size < minBufferSize)
                 bufferSize = minBufferSize * 2;
             else if (inc_buffer_size < 2 * minBufferSize)
                 bufferSize = inc_buffer_size * 2;
             else
                 bufferSize = inc_buffer_size;
         }
         return bufferSize;
     }
	
	private void initAudioRecorder() {
		mAudioRecorder =  new AudioRecord(MediaRecorder.AudioSource.MIC, sample_rate,
                AUDIO_CHANNEL, audio_encoding, calc_buffer_size());
		mAudioRecorder.startRecording();
		 if (mAudioRecorder.getRecordingState() != AudioRecord.RECORDSTATE_RECORDING) {
			 Log.e(TAG, "recorder can not start");
         } else {
        	 mIsRecording = true;
        	 mPool = Executors.newFixedThreadPool(1);
        	 mPool.execute(new Runnable(){
        		 @Override
        		 public void run() {
        			 readDataFromSoloAudioRecorderInloop();
        		 }
        	 });
         }
	}
	
	private void readDataFromSoloAudioRecorderInloop() {
		 int useReadBufferSize = sample_rate * audio_channel_num * audio_encoding * INTERVAL / 1000;
		 byte[] readBuffer = new byte[useReadBufferSize];
		 int readSize = 0;
		 while (true) {
			 if (!mIsRecording) {
                 break;
             }
			if (mAudioRecorder != null) {
				readSize = mAudioRecorder
						.read(readBuffer, 0, useReadBufferSize);
				if (readSize > 0) {
					byte[] bytes = new byte[readSize];
					System.arraycopy(readBuffer, 0, bytes, 0, readSize);
					if (mIsWakeupEngineWorking) {
						mWakeupEngine.feedData(bytes, readSize);
					}
					mLock.lock(); // lock
					if (mIsNeedCaching) {
						if (mQueue.size() > QUEUESIZE) {// 只缓存唤醒点之前1.2s的音频
							mQueue.remove();
						}
						mQueue.offer(bytes);
					}
					if (mIsAsrEngineWorking) {
						if (mIsNeedFeedCachingDataToAsr) {
							// 把queue里的数据(你好小乐)送往asr，asr里有功能来检测vad
                            mIsNeedCaching = true;
							mIsNeedFeedCachingDataToAsr = false;
							for (byte[] byteTemp : mQueue) {
								mAsrEngine.feedData(byteTemp);
							}
						}
						mAsrEngine.feedData(bytes);
					}
					mLock.unlock(); // unlock
				} else {
					Log.e(TAG, "audiorecord read error");
				}
			}
		}
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
        mAsrEngine.setUseCustomFeed(true);
        mAsrEngine.setCloudVadEnable(true);//设置是否开启服务端的vad功能,默认开启为true
        mAsrEngine.setSaveAudioPath("/sdcard/aispeech");//保存的音频路径,格式为.ogg
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
        mWakeupEngine.setUseCustomFeed(true);
        mWakeupEngine.init(new AIWakeupListenerImpl());
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
        mTtsEngine.setBackResBinArray(new String[] {SampleConstants.TTS_BACK_RES_ZHILING,
                SampleConstants.TTS_BACK_RES_LUCY, SampleConstants.TTS_BACK_RES_XIJUN});
        mTtsEngine.init(new AILocalTTSListenerImpl());//初始化合成引擎
        mTtsEngine.setSpeechRate(0.85f);//设置合成音语速，范围为0.5～2.0
        mTtsEngine.setStreamType(AudioManager.STREAM_MUSIC);//设置audioTrack的播放流，默认为music
        mTtsEngine.setUseSSML(false);//设置是否使用ssml合成语法，默认为false
        mTtsEngine.setSpeechVolume(100);//设置合成音频的音量，范围为0～500
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
        	mLock.lock();
        	mIsNeedCaching = false; //唤醒了就可以不缓存音频了
        	mIsNeedFeedCachingDataToAsr = true;//唤醒后要把缓存的音频送往识别
        	startAsrUsingOneShot();
    		mLock.unlock(); //这几个标志位放在lock里
        	mEt.setText("唤醒成功"+ wakeupWord +"\n");
        }

        @Override
        public void onReadyForSpeech() {
        }

        @Override
        public void onRawDataReceived(byte[] buffer, int size) {

        }

        @Override
        public void onResultDataReceived(byte[] buffer, int size) {

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


    private void startAsrUsingOneShot() {
		//这里启动带有oneshot功能的识别引擎，该引擎如果检测到连说（唤醒词+命令词），
    	//那么就会直接在唤醒引擎的onResult回调中输出结果，如果检测到不是连说（只有唤醒词），那么会回调onNotOneShot(),用户可以在onNotOneShot()兼容老版本的内容
		mAsrEngine.notifyWakeup();
    	mAsrEngine.setPauseTime(0);//唤醒后pauseTime设为0 ,这里为了加快判断是否是连着说
    	mAsrEngine.setOneShotIntervalTimeThreshold(INTERVAL_TIME_OF_ONE_SHOT_THRESH);//默认为700，这个值和上面的pauseTime的差值可以根据实际情况调整
    	//如果想增加连说之间的间隔，请同时增加上面两个值，比如同时加500ms，能体验到唤醒词和命令词之间的间隔可以很长
    	mAsrEngine.setUseOneShotFunction(true);
    	mAsrEngine.start();
		mIsAsrEngineWorking = true;
	}
	
	private void startAsrNotUsingOneShot() {
		//启动不带oneshot功能的识别引擎，这里onNotOneShot()不会被回调，这个只是单纯的识别，和老版本一样
		mAsrEngine.setPauseTime(300); //合成后起的识别pauseTime设为300
    	mAsrEngine.setUseOneShotFunction(false);
		mAsrEngine.start();
		mIsAsrEngineWorking = true;
	}
	
	private void startWakeup() {
		mWakeupEngine.start();
		mIsWakeupEngineWorking = true;
		mIsNeedCaching = true;
		mEt.append("你可以说你好小乐来唤醒 或者直接说 你好小乐+命令词\n");
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
        	mVadEndTime = System.currentTimeMillis();
        	if(mVadEndTime > 600) {
        		
        	}
//        	mTtsEngine.speak("有什么可以帮您", "1024");
        }

        @Override
        public void onRawDataReceived(byte[] buffer, int size) {

        }

        @Override
        public void onResultDataReceived(byte[] buffer, int size) {

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
        	mIsAsrEngineWorking = false;
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
			mIsAsrEngineWorking = false;
			mTtsEngine.speak("有什么可以帮您", "1024");
		}
    }

	@Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mIsRecording = false;
        stopRecorder();
        releaseRecorder();
        if(mPool != null) {
        	mPool.shutdownNow();
        }
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
	
	private void stopRecorder() {
		mIsRecording = false;
		if (mAudioRecorder != null) {
			 mAudioRecorder.stop();
	        }
	}
	
	private void releaseRecorder() {
		 if (mAudioRecorder != null) {
			 mAudioRecorder.release();
			 mAudioRecorder = null;
	        }
	}
}
