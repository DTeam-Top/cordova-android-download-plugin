package org.jiuren.cordova.download;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Random;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

public class DownloadPlugin extends CordovaPlugin {

    @Override
    public boolean execute(String action, final JSONArray args, final CallbackContext callbackContext) {
        if (action.equals("get")) {
            cordova.getThreadPool().execute(new Runnable() {
                public void run() {
                    try {
                        JSONObject params = args.getJSONObject(0);
                        String fileUrl    = params.getString("url");
                        Boolean overwrite = params.getBoolean("overwrite");
                        String fileName   = params.getString("name");
                        String path       = params.getString("absPath");
                        Boolean install   = params.getBoolean("install");
                        String dirName = Environment.getExternalStorageDirectory().getAbsolutePath()
                                + path;
                        downloadUrl(fileUrl, dirName, fileName, overwrite, install, callbackContext);
                    } catch (JSONException e) {
                        e.printStackTrace();
                        Log.e("Downloader Plugin", "Error: " + PluginResult.Status.JSON_EXCEPTION);
                        callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.JSON_EXCEPTION));
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        Log.e("Downloader Plugin", "Error: " + PluginResult.Status.ERROR);
                        callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.ERROR));
                    }
                }
            });

            return true;
        } else {
            Log.e("Downloader Plugin", "Error: " + PluginResult.Status.INVALID_ACTION);
            callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.INVALID_ACTION));
            return false;
        }
    }

    private Boolean downloadUrl(String fileUrl, String dirName,
                                String fileName, Boolean overwrite, Boolean install, CallbackContext callbackContext)
            throws InterruptedException, JSONException {
        try {
            File dir = new File(dirName);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            File file = new File(dirName, fileName);
            if (overwrite == true || !file.exists()) {
                Intent intent = new Intent();
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                PendingIntent pend = PendingIntent.getActivity(cordova.getActivity(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
                NotificationManager mNotifyManager = (NotificationManager) cordova.getActivity().getSystemService(Activity.NOTIFICATION_SERVICE);
                NotificationCompat.Builder mBuilder =
                        new NotificationCompat.Builder(cordova.getActivity())
                                .setSmallIcon(cordova.getActivity().getResources().getIdentifier("icon", "mipmap", cordova.getActivity().getPackageName()))
                                .setContentTitle(cordova.getActivity().getString(cordova.getActivity().getResources().getIdentifier("app_name", "string", cordova.getActivity().getPackageName())))
                                .setContentText("文件: " + fileName + " - 0%");
                int mNotificationId = new Random().nextInt(10000);
                URL url = new URL(fileUrl);
                HttpURLConnection ucon = (HttpURLConnection) url.openConnection();
                ucon.setRequestMethod("GET");
                ucon.connect();
                InputStream is = ucon.getInputStream();
                byte[] buffer = new byte[1024];
                int readed = 0, progress = 0, totalReaded = 0, fileSize = ucon.getContentLength();
                FileOutputStream fos = new FileOutputStream(file);
                showToast("开始下载.", "short");
                int step = 0;
                while ((readed = is.read(buffer)) > 0) {
                    fos.write(buffer, 0, readed);
                    totalReaded += readed;
                    int newProgress = (int) (((float)totalReaded / fileSize) * 100);
                    if (newProgress != progress & newProgress > step) {
                        mBuilder.setProgress(100, newProgress, false);
                        mBuilder.setContentText("文件: " + fileName + " - " + step + "%");
                        mBuilder.setContentIntent(pend);
                        mNotifyManager.notify(mNotificationId, mBuilder.build());
                        step = step + 1;
                    }
                }
                fos.flush();
                fos.close();
                is.close();
                ucon.disconnect();
                mBuilder.setContentText("下载 \"" + fileName + "\" 完成").setProgress(0, 0, false);
                mNotifyManager.notify(mNotificationId, mBuilder.build());
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Log.d("Downloader Plugin", "Thread sleep error: " + e);
                }
                mNotifyManager.cancel(mNotificationId);
                showToast("下载完成.", "short");
                if(install){
                    install(dirName, fileName);
                }
            } else if (overwrite == false) {
                showToast("文件正在下载.", "short");
            }
            if (!file.exists()) {
                showToast("下载失败.", "long");
                Log.e("Downloader Plugin", "Error: 下载失败.");
            }
            callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK));
            return true;
        } catch (FileNotFoundException e) {
            showToast("下载失败.", "long");
            Log.e("Downloader Plugin", "Error: " + PluginResult.Status.ERROR);
            e.printStackTrace();
            callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.ERROR));
            return false;
        } catch (IOException e) {
            showToast("下载失败.", "long");
            Log.e("Downloader Plugin", "Error: " + PluginResult.Status.ERROR);
            e.printStackTrace();
            callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.ERROR));
            return false;
        }
    }

    private void install (final String dirName, final String fileName) {
        Intent in = new Intent(Intent.ACTION_VIEW);
        in.setDataAndType(Uri.fromFile(new File(dirName+fileName)),
                "application/vnd.android.package-archive");
        // in.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        cordova.getActivity().startActivity(in);
    }


    private void showToast(final String message, final String duration) {
        cordova.getActivity().runOnUiThread(new Runnable() {
            public void run() {
                Toast toast;
                if (duration.equals("long")) {
                    toast = Toast.makeText(cordova.getActivity(), message, Toast.LENGTH_LONG);
                } else {
                    toast = Toast.makeText(cordova.getActivity(), message, Toast.LENGTH_SHORT);
                }
                toast.show();
            }
        });
    }
}