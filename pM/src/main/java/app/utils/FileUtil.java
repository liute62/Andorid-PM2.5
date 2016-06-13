package app.utils;

import android.content.Context;
import android.os.Environment;
import android.util.Log;
import android.util.Printer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;

/**
 * Created by liuhaodong1 on 16/1/31.
 */
public class FileUtil {

    public static final String TAG = "FileUtil";

    public static final String base_path = Environment.getExternalStorageDirectory().getPath();

    public static final String log_path = base_path+File.separator+"Bio3Air"+File.separator;

    public static final String tmp_path = log_path+File.separator+"tmp"+File.separator;

    public static final String log_file_name = log_path +"bio3AirLog";

    public static final String error_file_name = log_path + "bio3AirError";

    public static void makeTmpDir(){
        File dir = new File(tmp_path);
        if(!dir.exists())
            dir.mkdirs();
    }

    public static void appendErrorToFile(int runTime,String errorMsg){
        long time = System.currentTimeMillis();
        String timeStr = ShortcutUtil.refFormatDateAndTime(time);
        String path = error_file_name + ".txt";
        File dir = new File(log_path);
        String txt = timeStr+" DBRuntime = "+runTime+" "+errorMsg;
        PrintWriter printWriter = null;
        if(!dir.exists())
            dir.mkdirs();
        File file = new File(path);
        if(!file.exists())
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
                Log.e(TAG,"file = "+path+" create failed");
            }
        try {
            OutputStream outputStream = new FileOutputStream(file,true);
            printWriter = new PrintWriter(outputStream);
            printWriter.println(txt);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            Log.e(TAG,"file = "+path+" not found");
        }finally {
            if(printWriter != null)
                printWriter.close();
        }
    }

    public static void appendErrorToFile(String TAG, String errorMsg){
        long time = System.currentTimeMillis();
        String timeStr = ShortcutUtil.refFormatDateAndTime(time);
        String path = error_file_name + ".txt";
        File dir = new File(log_path);
        String txt = timeStr+" Tag = "+TAG+" "+errorMsg;
        PrintWriter printWriter = null;
        if(!dir.exists())
            dir.mkdirs();
        File file = new File(path);
        if(!file.exists())
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
                Log.e(TAG,"file = "+path+" create failed");
            }
        try {
            OutputStream outputStream = new FileOutputStream(file,true);
            printWriter = new PrintWriter(outputStream);
            printWriter.println(txt);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            Log.e(TAG,"file = "+path+" not found");
        }finally {
            if(printWriter != null)
                printWriter.close();
        }
    }

    public static void appendStrToFile(int runTime,String content){
        long time = System.currentTimeMillis();
        String timeStr = ShortcutUtil.refFormatDateAndTime(time);
        String path = log_file_name + timeStr.substring(0,10)+".txt";
        String txt = timeStr+" DBRuntime = "+runTime+" "+content;
        //Log.e(TAG,"path = "+log_path);
        File dir = new File(log_path);
        PrintWriter printWriter = null;
        if(!dir.exists())
            dir.mkdirs();
        File file = new File(path);
        if(!file.exists())
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
                Log.e(TAG, e.toString());
                Log.e(TAG,"file = "+path+" create failed");
            }
        try {
            OutputStream outputStream = new FileOutputStream(file,true);
            printWriter = new PrintWriter(outputStream);
            printWriter.println(txt);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            Log.e(TAG,"file = "+path+" not found");
        }finally {
            if(printWriter != null)
                printWriter.close();
        }
    }

    public static void appendStrToFile(String content){
        long time = System.currentTimeMillis();
        String timeStr = ShortcutUtil.refFormatDateAndTime(time);
        String path = log_file_name + timeStr.substring(0,10)+".txt";
        String txt = timeStr+" "+content;
        //Log.e(TAG,"path = "+log_path);
        File dir = new File(log_path);
        PrintWriter printWriter = null;
        if(!dir.exists())
            dir.mkdirs();
        File file = new File(path);
        if(!file.exists())
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
                Log.e(TAG,"file = "+path+" create failed");
            }
        try {
            OutputStream outputStream = new FileOutputStream(file,true);
            printWriter = new PrintWriter(outputStream);
            printWriter.println(txt);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            Log.e(TAG,"file = "+path+" not found");
        }finally {
            if(printWriter != null)
                printWriter.close();
        }
    }

    public static void appendStrToFile(String TAG,String content){
        long time = System.currentTimeMillis();
        String timeStr = ShortcutUtil.refFormatDateAndTime(time);
        String path = log_file_name + timeStr.substring(0,10)+".txt";
        String txt = timeStr+" TAG "+TAG+" "+content;
        //Log.e(TAG,"path = "+log_path);
        File dir = new File(log_path);
        PrintWriter printWriter = null;
        if(!dir.exists())
            dir.mkdirs();
        File file = new File(path);
        if(!file.exists())
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
                Log.e(TAG,"file = "+path+" create failed");
            }
        try {
            OutputStream outputStream = new FileOutputStream(file,true);
            printWriter = new PrintWriter(outputStream);
            printWriter.println(txt);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            Log.e(TAG,"file = "+path+" not found");
        }finally {
            if(printWriter != null)
                printWriter.close();
        }
    }

}
