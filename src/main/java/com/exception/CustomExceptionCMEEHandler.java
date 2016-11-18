package com.exception;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Environment;
import android.util.Log;

import com.eventpluscemm.trxservice.CreateExceptionTrx;
import com.log.Logger;
import com.variable.utility.CommonUtility;

import org.apache.http.HttpResponse;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.greenrobot.eventbus.EventBus;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

/**
 * Created by star on 2016/6/7.
 */
public class CustomExceptionCMEEHandler {
    public static boolean DEBUG = false;
    private static final String TAG = CustomExceptionCMEEHandler.class.getSimpleName();
    private static String fixPath;
    private static final String fixURL = null;

    private static File dirFile = null;
    private static String[] stackTraceFileList = null;
    private static String URL = null;

    public CustomExceptionCMEEHandler() {

    }

    /**
     * 是否開啟debug
     * 預設:false關閉, true:開啟
     * @param open
     */
    public static void setDebugMode(boolean open){
        DEBUG = open;
    }

    /**
     * register
     * @param var0 Context
     * @return
     */
    public static boolean register(Context var0) {
        if(DEBUG) {
            Log.i(TAG, "Registering default exceptions handler");
        }

        PackageManager var1 = var0.getPackageManager();
        try {
            PackageInfo var2 = var1.getPackageInfo(var0.getPackageName(), 0);
            G.APP_VERSION = var2.versionName;
            G.APP_PACKAGE = var2.packageName;
            fixPath = "/"+ G.APP_PACKAGE+"Exception";

//            if(true== Environment.getExternalStorageState().equals(Environment.MEDIA_REMOVED)){
//                //do nothing
//            } else {
//                // 取得SD卡儲存路徑
                dirFile = Environment.getExternalStorageDirectory();
//            }
            //建立文件檔儲存路徑
//            if(dirFile==null){
//                G.FILES_PATH = var0.getFilesDir().getAbsolutePath()+fixPath;
//            }else{
                G.FILES_PATH = dirFile + fixPath;
//            }

            G.PHONE_MODEL = Build.MODEL;
            G.ANDROID_VERSION = Build.VERSION.RELEASE;
            if(URL==null){
                G.URL = fixURL;
            }else{
                G.URL = URL;
            }
        } catch (PackageManager.NameNotFoundException var3) {
            var3.printStackTrace();
        }

        if(DEBUG) {
            Log.i(TAG, "TRACE_VERSION: " + G.TraceVersion);
            Log.d(TAG, "APP_VERSION: " + G.APP_VERSION);
            Log.d(TAG, "APP_PACKAGE: " + G.APP_PACKAGE);
            Log.d(TAG, "FILES_PATH: " + G.FILES_PATH);
            Log.d(TAG, "URL: " + G.URL);
        }

        boolean var4 = false;
        if(searchForStackTraces()!=null && searchForStackTraces().length > 0) {
            var4 = true;
        }

        (new Thread() {
            public void run() {
                submitStackTraces();
                UncaughtExceptionHandler var1 = Thread.getDefaultUncaughtExceptionHandler();

                if(var1 != null) {
                    Log.d(TAG, "current handler class=" + var1.getClass().getName());
                }

                /**
                 * 20150801[star] 出錯的時候會進到裡面執行
                 */
                if(!(var1 instanceof CustomUncaughtExceptionHandler)) {
                    Thread.setDefaultUncaughtExceptionHandler(new CustomUncaughtExceptionHandler(var1));
                }
            }
        }).start();
        return var4;
    }

    /**
     * 設定POST server URL
     * @param url
     */
    public static void setServerURL(String url){
        URL = url;
    }

    /**
     * 刪除前一天檔案
     */
    public static void delteBeforeDayFile() {
        SimpleDateFormat sDateFormat = new SimpleDateFormat("yyyyMMddhhmmss");
        String timestamp = sDateFormat.format(new java.util.Date());
        File file = new File(G.FILES_PATH);
        File[] files = file.listFiles();
        int count = files.length;
        if(DEBUG){
            Log.d(TAG, "delteBeforeDayFile count=" + count);
        }
        for(int i=0;i<count;i++){
            if(DEBUG){
                Log.d(TAG, "delteBeforeDayFile file name length=" + files[i].getName().length() + "=timestamp length=" + timestamp.length());
                Log.d(TAG, "delteBeforeDayFile file=" + files[i].getName().substring(0, 8) + "=timestamp=" + timestamp.substring(0, 8) + "="+files[i].getName().substring(0, 8).equals(timestamp.substring(0, 8)));
            }
            //比較年/月/日，與今天年/月/日不相同則刪除檔案
            if((files[i].getName().length()>8 && timestamp.length()>8)
                    && false==files[i].getName().substring(0, 8).equals(timestamp.substring(0, 8))){
                File f = new File(files[i].getAbsolutePath());
                f.delete();
            }
        }
    }

    /**
     * register
     * @param context
     * @param url
     */
    public static void register(Context context, String url) {
        if(DEBUG) {
            Log.d(TAG, "Registering default exceptions handler, URL: " + url);
        }
        URL = url;
        register(context);
    }

    private static String[] searchForStackTraces() {
        if(DEBUG) {
            Log.d(TAG, "searchForStackTraces: " + stackTraceFileList);
        }
        if(stackTraceFileList != null) {
            return stackTraceFileList;
        } else {
            File var0 = new File(G.FILES_PATH + "/");
            var0.mkdir();
            FilenameFilter var1 = new FilenameFilter() {
                public boolean accept(File var1, String var2) {
                    return false;
                }
            };
            return stackTraceFileList = var0.list(var1);
        }
    }

    /**
     * 傳送到Server
     */
    public static void submitStackTraces() {
        boolean var22 = false;

        String[] var0;
        int var1;
        File var28;
        label166: {
            try {
                var22 = true;
                if(DEBUG) {
                    Log.d(TAG, "Looking for exceptions in: " + G.FILES_PATH);
                }
                var0 = searchForStackTraces();
                if(DEBUG) {
                    Log.d(TAG, "submitStackTraces var0: " + var0);
                }
                if(var0 != null) {
                    if(var0.length <= 0) {
                        var22 = false;
                        break label166;
                    }

                    if(DEBUG) {
                        Log.d(TAG, "Found " + var0.length + " stacktrace(s)");
                    }

                    for(var1 = 0; var1 < var0.length; ++var1) {
                        String var2 = G.FILES_PATH + "/" + var0[var1];
                        String var3 = var0[var1].split("-")[0];

                        if(DEBUG) {
                            Log.d(TAG, "Stacktrace in file \'" + var2 + "\' belongs to version " + var3);
                        }

                        StringBuilder var4 = new StringBuilder();
                        BufferedReader var5 = new BufferedReader(new FileReader(var2));
                        String var6;
                        String var7 = null;
                        String var8 = null;

                        while((var6 = var5.readLine()) != null) {
                            if(var7 == null) {
                                var7 = var6;
                            } else if(var8 == null) {
                                var8 = var6;
                            } else {
                                var4.append(var6);
                                var4.append(System.getProperty("line.separator"));
                            }
                        }

                        var5.close();
                        String var9 = var4.toString();

                        if(DEBUG) {
                            Log.d(TAG, "Transmitting stack trace: " + var9);
                        }

                        /**
                         * 用CMEE傳送錯誤訊息到Server
                         */
                        SimpleDateFormat sDateFormat = new SimpleDateFormat("yyyyMMddhhmmss");
                        String timestamp = sDateFormat.format(new java.util.Date());

                        CreateExceptionTrx.createExceptionReqPut(timestamp
                                , G.ANDROID_VERSION
                                , G.APP_PACKAGE
                                , G.PHONE_MODEL
                                , var8
                                , var7
                                , URLEncoder.encode(var9));
                    }

                    var22 = false;
                    break label166;
                }

                var22 = false;
                break label166;
            } catch (Exception var26) {
                var26.printStackTrace();
                var22 = false;
            } finally {
                if(var22) {
                    try {
                        String[] var14 = searchForStackTraces();

                        for(int var15 = 0; var15 < var14.length; ++var15) {
                            File var16 = new File(G.FILES_PATH + "/" + var14[var15]);
                            var16.delete();
                        }
                    } catch (Exception var23) {
                        var23.printStackTrace();
                    }
                }
            }

            try {
                var0 = searchForStackTraces();
                for(var1 = 0; var1 < var0.length; ++var1) {
                    var28 = new File(G.FILES_PATH + "/" + var0[var1]);
                    var28.delete();
                }

                return;
            } catch (Exception var24) {
                var24.printStackTrace();
                return;
            }
        }

        try {
            var0 = searchForStackTraces();

            for(var1 = 0; var1 < var0.length; ++var1) {
                var28 = new File(G.FILES_PATH + "/" + var0[var1]);
                var28.delete();
            }
        } catch (Exception var25) {
            var25.printStackTrace();
        }
    }
}
