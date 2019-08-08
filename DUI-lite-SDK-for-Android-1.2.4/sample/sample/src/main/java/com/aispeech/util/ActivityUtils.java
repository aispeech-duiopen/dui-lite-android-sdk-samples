package com.aispeech.util;

import android.content.Context;
import android.util.Log;
import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

/**
 * @auther wuwei
 */
public class ActivityUtils {
    public static final String TAG = "ActivityUtils";
    public static final String KEY_XML_ROOT = "activityList";
    public static final String KEY_LABEL = "label";
    public static final String KEY_NAME = "name";

    public static ArrayList<ActivityBean> pullXML(Context context, String fileName) {
        ArrayList<ActivityBean> activityBeanList = null;
        ActivityBean activityBean = null;
        try {
            InputStream inputStream = context.getAssets().open(fileName);
            //创建xmlPull解析器
            XmlPullParser parser = Xml.newPullParser();
            //初始化xmlPull解析器
            parser.setInput(inputStream, "utf-8");
            //读取文件的类型
            int type = parser.getEventType();
            //无限判断文件类型进行读取
            while (type != XmlPullParser.END_DOCUMENT) {
                switch (type) {
                    //开始标签
                    case XmlPullParser.START_TAG:
                        if (KEY_XML_ROOT.equals(parser.getName())) {
                            activityBeanList = new ArrayList<>();
                        } else if (KEY_LABEL.equals(parser.getName())) {
                            activityBean = new ActivityBean();
                            //获取name属性
                            String name = parser.getAttributeValue(null,KEY_NAME);
                            Log.d(TAG, "name = " + name);
                            activityBean.setName(name);
                            //获取label值
                            String label = parser.nextText();
                            Log.d(TAG, "label = " + label);
                            activityBean.setLabel(label);
                            activityBeanList.add(activityBean);
                        }
                        break;
                    //结束标签
                    case XmlPullParser.END_TAG:
                        break;
                }
                //继续往下读取标签类型
                type = parser.next();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (XmlPullParserException e) {
            e.printStackTrace();
        }
        Log.d(TAG, activityBeanList.toString());
        return activityBeanList;
    }
}
