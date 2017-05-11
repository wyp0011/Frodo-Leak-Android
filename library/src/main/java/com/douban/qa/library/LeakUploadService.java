package com.douban.qa.library;

import android.os.Build;
import android.os.Environment;
import android.util.Log;

import com.squareup.leakcanary.AnalysisResult;
import com.squareup.leakcanary.DisplayLeakService;
import com.squareup.leakcanary.HeapDump;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class LeakUploadService extends DisplayLeakService {

    final String TAG = "LeakUploadService";

    @Override
    protected void afterDefaultHandling(HeapDump heapDump, AnalysisResult result, String leakInfo) {
        Log.i(TAG, "found leak");

        if (!result.leakFound || result.excludedLeak) {
            return;
        }

        // save leak info and post to kiwi
        String className = result.className;
        String pkgName = leakInfo.trim().split(":")[0].split(" ")[1];
        String pkgVer = leakInfo.trim().split(":")[1];
        String leakDetail = leakInfo.split("\n\n")[0] + "\n\n" + leakInfo.split("\n\n")[1];

        JSONObject content = new JSONObject();
        try {
            content.put("brand", Build.BRAND);
            content.put("device_id", Build.SERIAL);
            content.put("os_version", Build.VERSION.RELEASE);
            content.put("manufacturer", Build.MANUFACTURER);
            content.put("model", Build.MODEL);

            content.put("leak_activity", className);
            content.put("leak_package_name", pkgName);
            content.put("leak_package_version", pkgVer);
            content.put("leak_detail", leakDetail);
        } catch (JSONException e) {
            Log.e(TAG, "init json failed: " + e.toString());
        }

        saveLeakInfo(content.toString());
        sendToPastebin(content.toString());
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
                Log.e(TAG, "saving failed: " + e.toString());
            }
        }
    }

    private void sendToPastebin(String content) {
        Log.i(TAG, "sending to pastebin");

        HttpURLConnection conn = null;
        try {
            String url = "http://pastebin.dapps.douban.com/json/?method=pastes.newPaste";
            URL mUrl = new URL(url);

            conn = (HttpURLConnection) mUrl.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");

            JSONObject data = new JSONObject();
            data.put("code", (String) content);
            data.put("language", "text");

            OutputStream outputStream = conn.getOutputStream();
            outputStream.write(data.toString().getBytes());
            outputStream.flush();
            outputStream.close();

            Log.i(TAG, "data: " + data.toString());
            int resCode = conn.getResponseCode();
            if (resCode == 200) {
                InputStream inputStream = conn.getInputStream();
                String res = getStringFromInputStream(inputStream);
                Log.i(TAG, "pasetebin: " + res);
            }

        } catch (Exception e) {
            Log.e(TAG, "send failed: " + e.toString());
        }
    }

    private void sendToKiwi(JSONObject content) {
        Log.i(TAG, "sending to kiwi");

        HttpURLConnection conn = null;
        try {
//            String url = "http://kiwi.dapps.doubab.com/leak_test/new";
            String url = "http://172.16.23.152:8090/leak_test/new";
            URL mUrl = new URL(url);

            conn = (HttpURLConnection) mUrl.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");

            OutputStream outputStream = conn.getOutputStream();
            outputStream.write(content.toString().getBytes());
            outputStream.flush();
            outputStream.close();

            int resCode = conn.getResponseCode();
            if (resCode == 200) {
                InputStream inputStream = conn.getInputStream();
                String res = getStringFromInputStream(inputStream);
                Log.i(TAG, "kiwi: " + res);
            }

        } catch (Exception e) {
            Log.e(TAG, "send failed: " + e.toString());
        }
    }

    private String getStringFromInputStream(InputStream inputStream) throws IOException{
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
