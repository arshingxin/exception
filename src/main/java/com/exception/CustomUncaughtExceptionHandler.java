package com.exception;

import android.util.Log;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;

/**
 * Exception 處理
 * 20150623[star] create
 */
public class CustomUncaughtExceptionHandler implements Thread.UncaughtExceptionHandler {
	private static final String TAG = CustomUncaughtExceptionHandler.class.getSimpleName();
	private static final boolean DEBUG = CustomExceptionHandler.DEBUG;

	private Thread.UncaughtExceptionHandler customUncaughtExceptionHandler;

	public CustomUncaughtExceptionHandler(Thread.UncaughtExceptionHandler var1) {
		this.customUncaughtExceptionHandler = var1;
	}

	public void uncaughtException(Thread t, Throwable s) {
		StringWriter stringWriter = new StringWriter();
		PrintWriter printWriter = new PrintWriter(stringWriter);
		s.printStackTrace(printWriter);
		try{
			SimpleDateFormat sDateFormat = new SimpleDateFormat("yyyyMMddhhmmss");
			String timestamp = sDateFormat.format(new java.util.Date());
			if(DEBUG) {
				Log.d(TAG, "Writing unhandled exception to: " + G.FILES_PATH + "/" + timestamp + ".stacktrace");
			}
			/**
			 * 20150713[star] 出錯前先刪除前一天檔案
			 */
			CustomExceptionHandler.delteBeforeDayFile();
			/**
			 * 寫入檔案的路徑/檔名
			 */
			BufferedWriter var8 = new BufferedWriter(new FileWriter(G.FILES_PATH + "/" + timestamp + ".stacktrace.txt"));
			/********TODO　寫入檔案的欄位/格式*****************/
			var8.write("version:"+G.ANDROID_VERSION + "\n");
			var8.write("model:"+G.PHONE_MODEL + "\n");
			var8.write("pn:"+G.APP_PACKAGE + "\n");
			var8.write(stringWriter.toString());
			/**************************************/
			var8.flush();
			var8.close();
		} catch (Exception var9) {
			var9.printStackTrace();
		}
		this.customUncaughtExceptionHandler.uncaughtException(t, s);
	}
}
