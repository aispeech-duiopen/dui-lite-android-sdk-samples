/*******************************************************************************
 * Copyright 2014 AISpeech
 ******************************************************************************/
package com.aispeech.sample;

import android.app.Activity;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.aispeech.AIError;
import com.aispeech.common.AIConstant;
import com.aispeech.export.engines.AICloudTTSEngine;
import com.aispeech.export.listeners.AITTSListener;

import java.util.ArrayList;
import java.util.List;

public class CloudTTS extends Activity implements View.OnClickListener,OnItemSelectedListener {

    private static final String TAG = "CloudTTS";
    final String CN_PREVIEW = "很难想象有什么事物会像廉价、强大、无处不在的人工智能，那样拥有“改变一切”的力量。未来，我们的日常行为将被彻底改变。";

    /*final static String CN_PREVIEW = "原标题：习近平履职“满月”观察：落子开局新时代，不要人夸颜色好，只留清气满乾坤。”2017年10月25日，再次当选中共中央总书记的习近平，引用这句古诗结束同中外记者见面的讲话，自信而坚定，清醒而谦逊。履职“满月”之际，回望习近平日夜兼程的足迹：2次视察调研，5次国内重要会议，10余场国内会见会谈，首访期间5天出席40多场双多边活动……一串串数字，有力诠释着讲话背后的实干决心。口言之，身必行之。”习近平以宏大的视野和全局的高度，擘画新时代的宏伟蓝图，写就实现中华民族伟大复兴的行动指南。习近平是规划者，也是践行者、领路人，步履不停、落子开局，带领中国走向新时代的豪迈征程。重温“红船精神“10月31日，在党的十九大胜利闭幕刚刚一周，习近平带领中共中央政治局常委奔赴上海瞻仰中共一大会址和浙江嘉兴瞻仰南湖红船。新一代领导班子循着革命先辈的足迹，重温那一段波澜壮阔的岁月。习近平强调：“上海党的一大会址、嘉兴南湖红船是我们党梦想起航的地方。我们党从这里诞生，从这里出征，从这里走向全国执政。这里是我们党的根脉。秀水泱泱，红船依旧；时代变迁，精神永恒。”在上海、在嘉兴，习近平多次提及“初心”二字。举起紧握的右拳，习近平带领其他中共中央政治局常委同志一起重温入党誓词，铿锵有力的宣誓声响彻大厅。一声声誓词，是新时代中国共产党人对一代代革命先辈矢志不渝的承诺，是为改变一个国家命运持之以恒的信念，是为人民群众谋幸福鞠躬尽瘁的决心。正如习近平在十九届中共中央政治局常委同中外记者见面时所言，“我们要永葆蓬勃朝气，永远做人民公仆、时代先锋、民族脊梁。”这份承诺、信念和决心，贯穿在习近平每次重要讲话中，融汇在贯彻落实十九大精神的生动实践中。10月27日，习近平主持召开十九届中共中央政治局会议，研究部署学习宣传贯彻党的十九大精神。会议通过了《中共中央政治局关于加强和维护党中央集中统一领导的若干规定》和《中共中央政治局贯彻落实中央八项规定的实施细则》。就在同一天，十九届中共中央政治局就深入学习贯彻党的十九大精神进行第一次集体学习。习近平强调，切实学懂弄通做实党的十九大精神，努力在新时代开启新征程续写新篇章。在座不少同志是新进中央政治局的同志，位子更高了，担子更重了，站位就要更高，眼界就要更宽。大家要把学习贯彻党的十九大精神作为第一堂党课、第一堂政治必修课，努力提高自己的政治素养和思想理论水平，以更好担负起党和人民赋予的重要职责。清谈误国、实干兴邦，一分部署、九分落实。要拿出实实在在的举措，一个时间节点一个时间节点往前推进，以钉钉子精神全面抓好落实。习近平的讲话朴实直白、掷地有声，一字一句凝聚党心，一言一行聚拢民心。";*/

    final String Tag = this.getClass().getName();
    AICloudTTSEngine mEngine;
    int i=1;
    TextView tip;
    EditText content;
    Button btnStart, btnStop, btnPause, btnResume, btnClear;
    Spinner spinner_speed, spinner_res,spinner_volume;

    Toast mToast;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.cloud_tts);

        tip = (TextView) findViewById(R.id.tip);
        content = (EditText) findViewById(R.id.content);
        btnStart = (Button) findViewById(R.id.btn_start);
        btnStop = (Button) findViewById(R.id.btn_stop);
        btnPause = (Button) findViewById(R.id.btn_pause);
        btnResume = (Button) findViewById(R.id.btn_resume);
        btnClear = (Button) findViewById(R.id.btn_clear);
        spinner_res = (Spinner) findViewById(R.id.spinner_res);
        spinner_speed = (Spinner) findViewById(R.id.spinner_speed);
        spinner_volume = (Spinner) findViewById(R.id.spinner_volume);
        spinner_res.setOnItemSelectedListener(this);
        spinner_speed.setOnItemSelectedListener(this);
        spinner_volume.setOnItemSelectedListener(this);
        initSpinnerData();
        content.setText(CN_PREVIEW);
        btnStart.setOnClickListener(this);
        btnStop.setOnClickListener(this);
        btnPause.setOnClickListener(this);
        btnResume.setOnClickListener(this);
        btnClear.setOnClickListener(this);
        mToast = Toast.makeText(this, "", Toast.LENGTH_LONG);
        // 创建云端合成播放器
        mEngine = AICloudTTSEngine.createInstance();
        mEngine.setTextType("text");//设置合成的文本类型,默认为text类型
        mEngine.setServer("https://tts.dui.ai/runtime/v2/synthesize");//访问云端合成服务器地址，默认为该地址
        mEngine.setStreamType(AudioManager.STREAM_MUSIC);//设置合成音播放的音频流,默认为音乐流
        mEngine.setRealBack(true);//设置是否实时反馈,默认为实时反馈为true
        mEngine.setAudioType(AIConstant.TTS_AUDIO_TYPE_MP3);//设置合成音频类型，默认为mp3
        mEngine.setMP3Quality(AIConstant.TTS_MP3_QUALITY_LOW);//设置云端合成mp3码率，支持low和high，默认为low
        mEngine.init(new AITTSListener() {
            @Override
            public void onInit(int status) {
                Log.d(TAG, "onInit()");
                if (status == AIConstant.OPT_SUCCESS) {
                    Log.i(Tag, "初始化成功!");
                } else {
                    Log.i(Tag, "初始化失败!");
                }
            }

            @Override
            public void onError(String utteranceId, AIError error) {
//                tip.setText("onError: "+utteranceId+","+error.toString());
                Log.e(TAG, "onError: "+utteranceId+","+error.toString());
            }

            @Override
            public void onReady(String utteranceId) {
                Log.e(TAG, "onReady: "+utteranceId);
            }

            @Override
            public void onCompletion(String utteranceId) {
                Log.e(TAG, "onCompletion: "+utteranceId);
                tip.setText("合成完成");
            }

            @Override
            public void onProgress(int currentTime, int totalTime, boolean isRefTextTTSFinished) {
                Log.e(TAG, "onProgress: "+currentTime);
                showTip("当前:" + currentTime + "ms, 总计:" + totalTime + "ms, 可信度:" + isRefTextTTSFinished);
            }
        });
        mEngine.setAudioPath(Environment.getExternalStorageDirectory()+"/tts");//设置合成音的保存路径

    }

    private void initSpinnerData(){
        ArrayAdapter<String> spinnerRes = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_dropdown_item, getResources()
                .getStringArray(R.array.tts_cn_res));
        spinner_res.setAdapter(spinnerRes);
        spinner_res.setSelection(0);
        List<String> volume = new ArrayList<>();
        for(int i = 1; i < 101 ; i++){
            volume.add(i+"");
        }
        ArrayAdapter<String> spinnerVolume = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_dropdown_item, volume);
        spinner_volume.setAdapter(spinnerVolume);
        spinner_volume.setSelection(49);
        List<String> speed = new ArrayList<>();
        speed.add("0.5");
        speed.add("0.6");
        speed.add("0.7");
        speed.add("0.8");
        speed.add("0.9");
        speed.add("1.0");
        speed.add("1.1");
        speed.add("1.2");
        speed.add("1.3");
        speed.add("1.4");
        speed.add("1.5");
        speed.add("1.6");
        speed.add("1.7");
        speed.add("1.8");
        speed.add("1.9");
        speed.add("2.0");
        ArrayAdapter<String> spinnerSpeed = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_dropdown_item, speed);
        spinner_speed.setAdapter(spinnerSpeed);
        spinner_speed.setSelection(5);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mEngine.release();
    }

    @Override
    public void onClick(View v) {
        if (v == btnStart) {
            Log.e(Tag, content.getText().toString());
            mEngine.speak(content.getText().toString(), "1024");

            tip.setText("正在合成...");
        } else if (v == btnStop) {
                mEngine.stop();
                tip.setText("已停止！");
        } else if (v == btnPause) {
                mEngine.pause();
                tip.setText("已暂停!");
        } else if (v == btnResume) {
                mEngine.resume();
                tip.setText("已恢复!");
        } else if (v == btnClear) {
            content.setText("");
        }
    }

    @Override
    public void onItemSelected(AdapterView<?> view, View arg1, int arg2, long arg3) {
        if (view == spinner_res) {
            mEngine.setSpeaker(spinner_res.getSelectedItem().toString());
        } else if (view == spinner_speed){
            mEngine.setSpeed(spinner_speed.getSelectedItem().toString());
        } else if (view == spinner_volume){
            mEngine.setVolume(spinner_volume.getSelectedItem().toString());
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {

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
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if(keyCode == KeyEvent.KEYCODE_BACK){
            if (mEngine != null) {
                mEngine.stop();
                mEngine.release();
            }
            finish();
        }
        return super.onKeyDown(keyCode, event);
    }
}
