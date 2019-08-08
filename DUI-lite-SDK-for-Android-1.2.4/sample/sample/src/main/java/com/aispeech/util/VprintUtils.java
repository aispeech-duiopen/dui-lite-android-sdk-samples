package com.aispeech.util;

import android.text.TextUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by wuwei on 2018/7/16.
 */

public class VprintUtils {

    /**
     * 获取已经注册人列表
     * @param registersStr
     * @return
     */
    public static List<String> getRegisterList(String registersStr) {
        if (TextUtils.isEmpty(registersStr)) {
            return null;
        }
        List<String> list = new ArrayList<>();
        try {
            JSONObject registerJsonObject = new JSONObject(registersStr);
            JSONArray jsonArray = registerJsonObject.getJSONArray("registers");
            if (jsonArray != null && jsonArray.length() > 0) {
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject jsonObject = jsonArray.getJSONObject(i);
                    list.add(jsonObject.optString("name"));
                }
            } else {
                return null;
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return list;
    }


    /**
     {"registers":
     [
        {
            "name":"dss",
            "wordlist":[
                {
                    "gender":"male",
                    "word":"xiao ou xiao ou",
                    "thresh":-17
                }
            ]
        }
     ]
     }
     */

    /**
     * 获取指定注册人注册的唤醒词列表
     * @param registerInfo
     * @param registerName
     * @return
     */
    public static List<String> getWordList(String registerInfo, String registerName) {
        if (TextUtils.isEmpty(registerInfo) && TextUtils.isEmpty(registerName)) {
            return null;
        }
        List<String> wordList = new ArrayList<>();
        JSONArray wordListJsonArray = null;
        try {
            JSONObject registerJsonObject = new JSONObject(registerInfo);
            JSONArray jsonArray = registerJsonObject.getJSONArray("registers");
            if (jsonArray != null && jsonArray.length() > 0) {
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject jsonObject = jsonArray.getJSONObject(i);
                    if (TextUtils.equals(registerName, jsonObject.optString("name"))) {
                        wordListJsonArray = jsonObject.getJSONArray("wordlist");
                        break;
                    }
                }
                if (wordListJsonArray.length() > 0) {
                    for (int i = 0; i < wordListJsonArray.length(); i++) {
                        JSONObject jsonObject = wordListJsonArray.getJSONObject(i);
                        wordList.add(jsonObject.optString("word"));
                    }
                } else {
                    return null;
                }
            } else {
                return null;
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return wordList;
    }
}
