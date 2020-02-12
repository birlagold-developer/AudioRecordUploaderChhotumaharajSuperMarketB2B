package com.kss.AudioRecordUploader.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.telephony.TelephonyManager;
import android.util.Base64;
import android.util.Log;
import android.view.Gravity;
import android.widget.Toast;

import com.kss.AudioRecordUploader.network.NetworkOperations;
import com.kss.AudioRecordUploader.network.WebServiceCalls;
import com.kss.AudioRecordUploader.utils.Constant;
import com.kss.AudioRecordUploader.utils.Networkstate;
import com.kss.AudioRecordUploader.utils.SharedPrefrenceObj;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Objects;

public class CallReceiver extends BroadcastReceiver {
    private static final String TAG = "CallReceiver";

    private Context context;

    @Override
    public void onReceive(Context context, Intent intent) {
        this.context = context;

        if (Networkstate.haveNetworkConnection(context)) {
            if (Objects.requireNonNull(intent.getAction()).equalsIgnoreCase("android.intent.action.PHONE_STATE")) {
                Bundle extras = intent.getExtras();
                if (extras != null) {

                    String state = extras.getString(TelephonyManager.EXTRA_STATE);
//            Log.d(TAG, "onReceive: TelephonyManager.EXTRA_STATE: " + state);
//            Log.d(TAG, "onReceive: TelephonyManager.CALL_STATE_IDLE: " + TelephonyManager.CALL_STATE_IDLE);
//            Log.d(TAG, "onReceive: TelephonyManager.CALL_STATE_RINGING: " + TelephonyManager.CALL_STATE_RINGING);
//            Log.d(TAG, "onReceive: TelephonyManager.CALL_STATE_OFFHOOK: " + TelephonyManager.CALL_STATE_OFFHOOK);
//            System.out.print("<=======================>");

                    if (state != null && state.equalsIgnoreCase(TelephonyManager.EXTRA_STATE_IDLE)) {
                        uploadFile();
                    }
                }
            } else {
                uploadFile();
            }
        } else {
            Toast toast = Toast.makeText(context, "NO INTERNET CONNECTION", Toast.LENGTH_LONG);
            toast.setGravity(Gravity.CENTER, 0, 0);
            toast.show();

            context.sendBroadcast(
                    new Intent().setAction("MANUAL_FILE_UPLOAD_COMPLETE")
            );
        }

    }

    private void uploadFile() {
        File[] datefolder = new File(Environment.getExternalStorageDirectory().getPath() + "/Call/").listFiles();

        System.out.print("A:" + Arrays.asList(datefolder.toString()));


        if (datefolder != null) {
            Arrays.sort(datefolder, (f1, f2) -> Long.compare(f2.lastModified(), f1.lastModified()));
            if (datefolder.length != 0) {
                for (int i = 0; i < datefolder.length; i++) {
                    File audioFile = datefolder[i];
                    if (audioFile.getName().contains("Call")) {
                        uploadFile(
                                SharedPrefrenceObj.getSharedValue(context, Constant.AGENT_NUMBBR),
                                SharedPrefrenceObj.getSharedValue(context, Constant.AGENT_EMAIL),
                                audioFile,
                                i < datefolder.length ? true : false
                        );
                    }
                }
            } else {
                Toast toast = Toast.makeText(context, "NO FILE AVAILABLE", Toast.LENGTH_LONG);
                toast.setGravity(Gravity.CENTER, 0, 0);
                toast.show();
                context.sendBroadcast(
                        new Intent().setAction("MANUAL_FILE_UPLOAD_COMPLETE")
                );
            }
        }


//        File[] datefolder = new File(Environment.getExternalStorageDirectory().getPath() + "/CallRecordingData/").listFiles();
//
//        System.out.print("A:" + Arrays.asList(datefolder.toString()));
//
//
//        if (datefolder != null) {
//            Arrays.sort(datefolder, (f1, f2) -> Long.compare(f2.lastModified(), f1.lastModified()));
//            if (datefolder.length == 1) {
//                for (int i = 0; i < datefolder.length - 1; i++) {
//                    File audioFile = datefolder[i];
//                    if (audioFile.getName().contains(".mp3")) {
//                        uploadFile(
//                                SharedPrefrenceObj.getSharedValue(context, Constant.AGENT_NUMBBR),
//                                SharedPrefrenceObj.getSharedValue(context, Constant.AGENT_EMAIL),
//                                audioFile,
//                                i < datefolder.length ? true : false
//                        );
//                    }
//                }
//            } else {
//                Toast toast = Toast.makeText(context, "NO FILE AVAILABLE", Toast.LENGTH_LONG);
//                toast.setGravity(Gravity.CENTER, 0, 0);
//                toast.show();
//                context.sendBroadcast(
//                        new Intent().setAction("MANUAL_FILE_UPLOAD_COMPLETE")
//                );
//            }
//        }

//        if (datefolder != null) {
//            Arrays.sort(datefolder, (f1, f2) -> Long.compare(f2.lastModified(), f1.lastModified()));
//            for (File dirFile : datefolder) {
//                if (dirFile.isDirectory()) {
//                    File[] files = Objects.requireNonNull(dirFile.listFiles());
//                    for (int i = 0; i < files.length; i++) {
//                        File audioFile = files[i];
//                        if (audioFile.getName().contains("+")) {
//                            uploadFile(
//                                    SharedPrefrenceObj.getSharedValue(context, Constant.AGENT_NUMBBR),
//                                    SharedPrefrenceObj.getSharedValue(context, Constant.AGENT_EMAIL),
//                                    audioFile,
//                                    i < files.length ? true : false
//                            );
//                        }
//                    }
//                }
//            }
//        }
    }

    private void uploadFile(String agentMobileNo, String agentEmail, final File audioFile, final boolean isLast) {

        String clientMobileNo = getClientNumber(audioFile.getName());
        //String audioStringFile = getStringFile(audioFile);
        String audioFileExtension = audioFile.getName().substring(audioFile.getName().lastIndexOf('.') + 1);
        int durationInSeconds = getDurationInSecond(audioFile);

        Log.i(TAG, "uploadFile: audioFile: " + audioFile.getAbsolutePath());
        Log.d(TAG, "uploadFile: agentMobile: " + agentMobileNo);
        Log.d(TAG, "uploadFile: agentEmail: " + agentEmail);
        Log.d(TAG, "uploadFile: clientMobileNo: " + clientMobileNo);
        //Log.d(TAG, "uploadFile: audioStringFile: " + audioStringFile);
        Log.d(TAG, "uploadFile: audioFileExtension: " + audioFileExtension);
        Log.d(TAG, "uploadFile: durationInSecond: " + durationInSeconds);


        WebServiceCalls.File.upload(context, agentMobileNo, agentEmail, clientMobileNo, String.valueOf(durationInSeconds), audioFile, new NetworkOperations() {

            @Override
            public void onSuccess(Bundle msg) {
                //TODO
                 audioFile.delete();
                if (isLast) {
                    context.sendBroadcast(
                            new Intent().setAction("MANUAL_FILE_UPLOAD_COMPLETE")
                    );
                }
            }

            @Override
            public void onFailure(Bundle msg) {
                //TODO

            }
        });
    }

    private String getClientNumber(String fileName) {
        String number = fileName.substring(fileName.lastIndexOf(" ") + 1, fileName.indexOf("_"));
        return number;
    }

//    private String getClientNumber(String fileName) {
//
//        String number = fileName.substring(0, fileName.indexOf("_"));
//
//        if (number.startsWith("+91")) {
//            return number;
//        } else if (number.length() == 10) {
//            return "+91" + number;
//        } else {
//            number = number.substring(1);
//            return "+91" + number;
//        }
//    }

    public String getStringFile(File file) {
        String base64StringFile = null;
        int size = (int) file.length();
        byte[] bytes = new byte[size];
        try {
            BufferedInputStream buf = new BufferedInputStream(new FileInputStream(file));
            buf.read(bytes, 0, bytes.length);
            buf.close();
            base64StringFile = Base64.encodeToString(bytes, Base64.DEFAULT);
            return base64StringFile;
        } catch (IOException e) {
            Log.e(TAG, "getStringFile: FileNotFoundException: ", e);
        }
        return null;
    }

    private int getDurationInSecond(File audioFile) {
        Uri uri = Uri.parse(audioFile.getAbsolutePath());
        MediaMetadataRetriever mmr = new MediaMetadataRetriever();
        mmr.setDataSource(context, uri);
        String durationStr = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
        mmr.release();
        int millSecond = Integer.parseInt(durationStr == null ? "0" : durationStr);
        return millSecond / 1000;
    }

}
