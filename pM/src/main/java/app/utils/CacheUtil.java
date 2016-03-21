package app.utils;

import android.content.Context;
import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

/**
 * Created by Hadong on 3/21/2016.
 *
 * A light weight util for caching params with unlimited time.
 */
public class CacheUtil {

    public static final String TAG = "CacheUtil";
    public Context mContext;
    public static CacheUtil instance = null;
    public String baseDir = "";

    public static CacheUtil getInstance(Context context){
        if(instance == null){
            instance = new CacheUtil(context);
        }
        return instance;
    }

    private CacheUtil(Context context){
        mContext = context.getApplicationContext();
        baseDir = context.getCacheDir().getAbsolutePath();
        Log.e(TAG,baseDir);
    }

    public void put(String key, String value) {
        File file = createAFile(key);
        BufferedWriter out = null;
        try {
            out = new BufferedWriter(new FileWriter(file), 1024);
            out.write(value);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (out != null) {
                try {
                    out.flush();
                    out.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void put(String key,Integer value){
        put(key,String.valueOf(value));
    }

    public void put(String key,Serializable object){
        File file = createAFile(key);
        if (file.exists()) {
            file.delete();
        }
        FileOutputStream os = null;
        ObjectOutputStream oos = null;
        try {
            os = new FileOutputStream(file);
            oos = new ObjectOutputStream(os);
            oos.writeObject(object);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                oos.close();
            } catch (IOException e) {
            }
            try {
                os.close();
            } catch (IOException e) {
            }
        }
    }

    public String getAsString(String key) {
        String readString = null;
        File file = getAFile(key);
        if (!file.exists())
            return null;
        BufferedReader in = null;
        try {
            in = new BufferedReader(new FileReader(file));
            readString = "";
            String currentLine;
            while ((currentLine = in.readLine()) != null) {
                readString += currentLine;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return readString;
    }

    public Object getAsObject(String key) {
        File file = getAFile(key);
        if (!file.exists())
            return null;
        InputStream is = null;
        ObjectInputStream ois = null;
        try {
            is = new FileInputStream(file);
            ois = new ObjectInputStream(is);
            return ois.readObject();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            try {
                if (is != null)
                    is.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                if (ois != null)
                    ois.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public boolean remove(String key){
        File file = getAFile(key);
        return file.delete();
    }

    private File createAFile(String name){
        return new File(baseDir, name.hashCode() + "");
    }

    private File getAFile(String name){
        File file = createAFile(name);
        return file;
    }
}
