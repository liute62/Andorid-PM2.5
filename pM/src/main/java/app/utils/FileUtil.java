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

    public static final String log_path = base_path+"/Bio3Air";

    public static final String log_file_name = log_path +"/bio3AirLog";

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
