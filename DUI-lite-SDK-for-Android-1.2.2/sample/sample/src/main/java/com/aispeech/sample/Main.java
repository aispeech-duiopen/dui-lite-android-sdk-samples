/*******************************************************************************
 * Copyright 2014 AISpeech
 ******************************************************************************/
package com.aispeech.sample;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

import com.aispeech.DUILiteSDK;
import com.aispeech.util.AIPermissionRequest;
import com.aispeech.util.ActivityBean;
import com.aispeech.util.ActivityUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;


public class Main extends Activity implements AdapterView.OnItemClickListener {
    private static final String TAG = "Main";
    private static boolean haveAuth = false;
    private AIPermissionRequest mPermissionRequest;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        //实例化动态权限申请类
        mPermissionRequest = new AIPermissionRequest();
        if ( ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestMulti();//所有权限一同申请
            Log.d(TAG, "request all needed　permissions");
        }


        ListView listView = (ListView) findViewById(R.id.activity_list);
        ArrayList<HashMap<String, Object>> listItems = new ArrayList<HashMap<String, Object>>();

        HashMap<String, Object> item = new HashMap<String, Object>();

        item = new HashMap<>();
        item.put("activity_name","授权");
        item.put("activity_class",null);
        listItems.add(item);

        //获取配置文件中需要加载的demo模块
        ArrayList<ActivityBean> activityArrayList = ActivityUtils.pullXML(getApplicationContext(), "module_cfg.xml");

        for (ActivityBean activityBean : activityArrayList) {
            item = new HashMap<>();
            try {
                item.put("activity_name", activityBean.getLabel());
                item.put("activity_class", Class.forName(getPackageName() + "." + activityBean.getName()));
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
            listItems.add(item);
        }

        SimpleAdapter adapter = new SimpleAdapter(this, listItems, R.layout.list_item,
                new String[] { "activity_name" }, new int[] { R.id.text_item });

        listView.setAdapter(adapter);
        listView.setDividerHeight(2);

        listView.setOnItemClickListener(this);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if(haveAuth || position ==  0) {
            Map<?, ?> map = (HashMap<?, ?>) parent.getAdapter().getItem(position);
            Class<?> clazz = (Class<?>) map.get("activity_class");
            if (map.get("activity_name").equals("授权")) {
                auth();
            } else {
                Intent it = new Intent(this, clazz);
                this.startActivity(it);
            }
        } else {
            Toast.makeText(this, "请先授权！", Toast.LENGTH_SHORT).show();
        }
    }

    private void auth(){
        final AlertDialog dialog = new AlertDialog.Builder(this)
                .setMessage("授权中...")
                .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                    }
                })
                .create();
        dialog.show();

        DUILiteSDK.setParameter(DUILiteSDK.KEY_AUTH_TIMEOUT, "5000");//设置授权连接超时时长，默认5000ms
//        DUILiteSDK.setParameter(DUILiteSDK.KEY_DEVICE_PROFILE_PATH, "/sdcard/speech");//自定义设置授权文件的保存路径,需要确保该路径事先存在
        boolean isAuthorized = DUILiteSDK.isAuthorized(getApplicationContext());//查询授权状态，DUILiteSDK.init之后随时可以调
        Log.d(TAG, "DUILite SDK is isAuthorized ？ " + isAuthorized);

        String core_version = DUILiteSDK.getCoreVersion();//获取内核版本号
        Log.d(TAG, "core version is: " + core_version);

//        DUILiteSDK.setParameter(DUILiteSDK.KEY_SET_THREAD_AFFINITY, 3);//绑定第三个核，降低CPU占用
        DUILiteSDK.setParameter(DUILiteSDK.KEY_UPLOAD_AUDIO_LEVEL, DUILiteSDK.UPLOAD_AUDIO_LEVEL_NONE);//默认不上传预唤醒和唤醒音频

        //设置SDK录音模式
        DUILiteSDK.setAudioRecorderType(DUILiteSDK.TYPE_COMMON_MIC);//单麦模式


        DUILiteSDK.openLog();//仅输出SDK logcat日志，须在init之前调用.
//        DUILiteSDK.openLog("/sdcard/duilite/DUILite_SDK.log");//输出SDK logcat日志，同时保存日志文件在/sdcard/duilite/DUILite_SDK.log，须在init之前调用.
        //新建产品需要填入productKey和productSecret，否则会授权不通过
        DUILiteSDK.init(getApplicationContext(),
                "d3c265662929841215092b415c257bd6",
                "278578021",
                "cfbdf9df02d199a602ed6f666a2494a3",
                "b06adce105c97ba926da3f18365a69f3",
                new DUILiteSDK.InitListener() {
            @Override
            public void success() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        dialog.setMessage("授权成功!");
                        haveAuth = true;
                    }
                });
            }

            @Override
            public void error(final String errorCode, final String errorInfo) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        dialog.setMessage("授权失败\n\nErrorCode："+errorCode+"\n\nErrorInfo："+errorInfo);
                        haveAuth = false;
                    }
                });

            }
        });

    }

    @Override
    public void onBackPressed() {
        finish();
        super.onBackPressed();
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
    }


    public void requestMulti() {
        mPermissionRequest.requestMultiPermissions(this, mPermissionGrant);
    }

    private AIPermissionRequest.PermissionGrant mPermissionGrant = new AIPermissionRequest.PermissionGrant() {
        @Override
        public void onPermissionGranted(int requestCode) {
            switch (requestCode) {
                case AIPermissionRequest.CODE_READ_PHONE_STATE:
                    Toast.makeText(Main.this, "Result Permission Grant CODE_READ_PHONE_STATE", Toast.LENGTH_SHORT).show();
                    break;
                case AIPermissionRequest.CODE_RECORD_AUDIO:
                    Toast.makeText(Main.this, "Result Permission Grant CODE_RECORD_AUDIO", Toast.LENGTH_SHORT).show();
                    break;
                case AIPermissionRequest.CODE_READ_EXTERNAL_STORAGE:
                    Toast.makeText(Main.this, "Result Permission Grant CODE_READ_EXTERNAL_STORAGE", Toast.LENGTH_SHORT).show();
                    break;
                case AIPermissionRequest.CODE_WRITE_EXTERNAL_STORAGE:
                    Toast.makeText(Main.this, "Result Permission Grant CODE_WRITE_EXTERNAL_STORAGE", Toast.LENGTH_SHORT).show();
                    break;
                case AIPermissionRequest.CODE_READ_CONTACTS:
                    Toast.makeText(Main.this, "Result Permission Grant CODE_READ_CONTACTS", Toast.LENGTH_SHORT).show();
                    break;
                default:
                    break;
            }
        }
    };

}
