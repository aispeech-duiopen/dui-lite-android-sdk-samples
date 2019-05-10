package com.aispeech.util;

import android.os.Environment;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * Created by wuwei on 18-5-7.
 */

public class LogUtils {

    public static boolean writeLog(String log) {
        try {
            File file = new File(Environment.getExternalStorageDirectory()+"/speech/second-vpInfo.txt");
            if(!file.exists()){
                file.createNewFile();
            }
            PrintWriter pw = new PrintWriter(new FileOutputStream(file, true));
            pw.write(log);
            pw.flush();
            pw.close();
            return true;
        } catch (IOException e){
            e.printStackTrace();
            return false;
        }
    }
}
