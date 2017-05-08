package com.douban.qa.library;

/**
 * Created by WU on 2017/5/8.
 */

import android.os.Environment;
import android.util.Log;

import com.squareup.haha.perflib.Heap;
import com.squareup.leakcanary.*;

import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class UploadService extends DisplayLeakService {

    final String TAG = "UploadService";

    @Override
    protected void afterDefaultHandling(HeapDump heapDump, AnalysisResult result, String leakInfo) {
        if (!result.leakFound || !result.excludedLeak) {
            return;
        }

        // save leak info and post to kiwi
        String className = result.className;
        String pkgName = leakInfo.trim().split(":")[0].split(" ")[1];
        String pkgVer = leakInfo.trim().split(":")[1];
        String leakDetail = leakInfo.split("\n\n")[0] + "\n\n" + leakInfo.split("\n\n")[1];

        String content = className + "\n"
                + pkgName + "\n"
                + pkgVer + "\n"
                + leakDetail + "\n";

        saveLeakInfo(content);
        sendToPastebin(content);
        sendToKiwi(content);
    }

    private void saveLeakInfo(String content) {
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            File file = new File(Environment.getExternalStorageDirectory(), "douban_leak.log");
            Log.i(TAG, "saving leak log: " + file.getAbsolutePath());

            try {
                FileOutputStream fos = new FileOutputStream(file);
                fos.write(content.getBytes());
                fos.close();
            } catch (Exception e) {
                Log.i(TAG, "saving failed: " + e.toString());
            }
        }
    }

    private void sendToPastebin(String content) {
        Log.i(TAG, "sending to pastebin");

        HttpURLConnection conn = null;
        try {
            String url = "https://pastebin.dapps.douban.com/json?method=pastes.newPaste";
            URL mUrl = new URL(url);

            conn = (HttpURLConnection) mUrl.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");

            JSONObject data = new JSONObject();
            data.put("code", content);
            data.put("language", "text");

            OutputStream outputStream = conn.getOutputStream();
            outputStream.write(data.toString().getBytes());
            outputStream.flush();
            outputStream.close();

            int resCode = conn.getResponseCode();
            if (resCode == 200) {
                InputStream inputStream = conn.getInputStream();
                String res = getStringFromInputStream(inputStream);
                Log.i(TAG, "pasetebin: " + res);
            }

        } catch (Exception e) {
            Log.i(TAG, "send failed: " + e.toString());
        }
    }

    private void sendToKiwi(String content) {
        Log.i(TAG, "sending to kiwi");

        HttpURLConnection conn = null;
        try {
            String url = "http://172.16.23.152:8090/leak_test/new";
            URL mUrl = new URL(url);

            conn = (HttpURLConnection) mUrl.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");

            JSONObject data = new JSONObject();
            data.put("leak_content", content);

            OutputStream outputStream = conn.getOutputStream();
            outputStream.write(data.toString().getBytes());
            outputStream.flush();
            outputStream.close();

            int resCode = conn.getResponseCode();
            if (resCode == 200) {
                InputStream inputStream = conn.getInputStream();
                String res = getStringFromInputStream(inputStream);
                Log.i(TAG, "kiwi: " + res);
            }

        } catch (Exception e) {
            Log.i(TAG, "send failed: " + e.toString());
        }
    }

    public String getStringFromInputStream(InputStream inputStream) throws IOException{
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int len = -1;
        while ((len = inputStream.read(buffer)) != -1) {
            baos.write(buffer, 0 ,len);
        }
        inputStream.close();

        String state = baos.toString();
        baos.close();
        return state;
    }
}
