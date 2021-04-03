package com.kss.AudioRecordUploaderCMSM.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Gravity;
import android.widget.Toast;

import com.kss.AudioRecordUploaderCMSM.database.DataBaseAdapter;
import com.kss.AudioRecordUploaderCMSM.model.Data;
import com.kss.AudioRecordUploaderCMSM.model.MissedCallLog;
import com.kss.AudioRecordUploaderCMSM.network.retrofit.RFInterface;
import com.kss.AudioRecordUploaderCMSM.network.retrofit.responsemodels.RmResultResponse;
import com.kss.AudioRecordUploaderCMSM.network.retrofit.responsemodels.RmUploadFileResponse;
import com.kss.AudioRecordUploaderCMSM.utils.Constant;
import com.kss.AudioRecordUploaderCMSM.utils.Networkstate;
import com.kss.AudioRecordUploaderCMSM.utils.SharedPrefrenceObj;
import com.kss.AudioRecordUploaderCMSM.utils.Utility;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Objects;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Response;

public class CallReceiver extends BroadcastReceiver {
    private static final String TAG = "CallReceiver";

    private Context context;

    static boolean RINGING = false;

    private String callerPhoneNumber;

    @Override
    public void onReceive(Context context, Intent intent) {
        this.context = context;

        if (Objects.requireNonNull(intent.getAction()).equalsIgnoreCase("android.intent.action.PHONE_STATE")) {
            Bundle extras = intent.getExtras();
            String state = extras.getString(TelephonyManager.EXTRA_STATE);
            if (Networkstate.haveNetworkConnection(context)) {
                if (state != null && state.equalsIgnoreCase(TelephonyManager.EXTRA_STATE_IDLE)) {
                    checkFolderAndUploadFile();
                }
            } else {
                Toast toast = Toast.makeText(context, "NO INTERNET CONNECTION", Toast.LENGTH_LONG);
                toast.setGravity(Gravity.CENTER, 0, 0);
                toast.show();

                context.sendBroadcast(
                        new Intent().setAction("MANUAL_FILE_UPLOAD_COMPLETE")
                );
            }
            //TODO
//            Bundle bundle = intent.getExtras();
//            String phoneNumber = bundle.getString("incoming_number");
//            TelephonyManager telephony = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
//            MissedCallPhoneStateListener missedCallPhoneStateListener = new MissedCallPhoneStateListener(context, phoneNumber);
//            telephony.listen(missedCallPhoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);

            if (state == null) {
                return;
            }

            callerPhoneNumber = extras.getString(TelephonyManager.EXTRA_INCOMING_NUMBER);

            if (callerPhoneNumber == null) {
                return;
            }

            // If phone state "Rininging"
            if (state.equals(TelephonyManager.EXTRA_STATE_RINGING)) {
                RINGING = true;
            }

            // If incoming call is received
            if (state.equals(TelephonyManager.EXTRA_STATE_OFFHOOK)) {
                RINGING = false;
            }

            // If phone is Idle
            if (state.equals(TelephonyManager.EXTRA_STATE_IDLE)) {
                if (RINGING) {
                    //Toast.makeText(context, "It was A MISSED CALL from : " + callerPhoneNumber, Toast.LENGTH_LONG).show();
                    DataBaseAdapter dataBaseAdapter = new DataBaseAdapter(context).open();
                    dataBaseAdapter.insertMissedCallLog(callerPhoneNumber, Constant.DATE_FORMAT.format(new Date()));
                    dataBaseAdapter.close();
                }
            }

        } else if (Objects.requireNonNull(intent.getAction()).equalsIgnoreCase("MANUAL_FILE_UPLOAD")) {
            if (Networkstate.haveNetworkConnection(context)) {
                checkFolderAndUploadFile();
            } else {
                Toast toast = Toast.makeText(context, "NO INTERNET CONNECTION", Toast.LENGTH_LONG);
                toast.setGravity(Gravity.CENTER, 0, 0);
                toast.show();

                context.sendBroadcast(
                        new Intent().setAction("MANUAL_FILE_UPLOAD_COMPLETE")
                );
            }
        }

        if (Networkstate.haveNetworkConnection(context)) {
            new UploadMissedCall(context).execute("");
        }
    }

    private void checkFolderAndUploadFile() {

        File[] datefolder = Constant.getCallRecordingDir().listFiles();

        if (datefolder != null) {
            Arrays.sort(datefolder, (f1, f2) -> Long.compare(f2.lastModified(), f1.lastModified()));
            if (datefolder.length != 0) {
                new uploadFile(datefolder).execute("");
            } else {
                Toast toast = Toast.makeText(context, "NO FILE AVAILABLE", Toast.LENGTH_LONG);
                toast.setGravity(Gravity.CENTER, 0, 0);
                toast.show();
                context.sendBroadcast(
                        new Intent().setAction("MANUAL_FILE_UPLOAD_COMPLETE")
                );
            }
        }
    }

    class uploadFile extends AsyncTask<String, String, String> {

        private File[] datefolder = null;
        private RFInterface rfInterface;

        public uploadFile(File[] datefolder) {
            this.datefolder = datefolder;
            rfInterface = Utility.getRetrofitInterface(Constant.URL);
        }

        @Override
        protected String doInBackground(String... strings) {

            for (int i = 0; i < datefolder.length; i++) {
                File audioFile = datefolder[i];
                if (audioFile.getName().contains("Call")) {

                    String agentMobileNumber = SharedPrefrenceObj.getSharedValue(context, Constant.AGENT_NUMBBR);
                    String agentEmailID = SharedPrefrenceObj.getSharedValue(context, Constant.AGENT_EMAIL);

                    String clientMobileNo = Constant.getClientNumber(context, audioFile.getName());
                    String audioFileExtension = audioFile.getName().substring(audioFile.getName().lastIndexOf('.') + 1);
                    int durationInSeconds = Constant.getDurationInSecond(context, audioFile);

                    RequestBody agentMobileNumberRequestBody = RequestBody.create(MediaType.parse("text"), agentMobileNumber);
                    RequestBody agentEmailIDRequestBody = RequestBody.create(MediaType.parse("text"), agentEmailID);
                    RequestBody clientMobileNumberRequestBody = RequestBody.create(MediaType.parse("text"), clientMobileNo);
                    RequestBody totalDurationRequestBody = RequestBody.create(MediaType.parse("text"), "" + durationInSeconds);
                    RequestBody audioFileRequestBody = RequestBody.create(MediaType.parse("audio"), audioFile);

                    RequestBody isLastRequestBody = null;
                    if (i == datefolder.length - 1) {
                        isLastRequestBody = RequestBody.create(MediaType.parse("text"), "1");
                    } else {
                        isLastRequestBody = RequestBody.create(MediaType.parse("text"), "0");
                    }

                    MultipartBody.Part audioMultipartBodyPart = MultipartBody.Part.createFormData("audio", audioFile.getName(), audioFileRequestBody);

                    upload(agentMobileNumberRequestBody, agentEmailIDRequestBody, clientMobileNumberRequestBody, totalDurationRequestBody, audioMultipartBodyPart, isLastRequestBody, i);
                }
            }

            return null;
        }

        private void upload(RequestBody agentMobileNumberRequestBody, RequestBody agentEmailIDRequestBody, RequestBody clientMobileNumberRequestBody,
                            RequestBody totalDurationRequestBody, MultipartBody.Part audioMultipartBodyPart, RequestBody isLastRequestBody, int fileCounter) {
            try {
                Response<RmUploadFileResponse> executeUploadFileResponse = rfInterface.uploadFile(
                        agentMobileNumberRequestBody, agentEmailIDRequestBody, clientMobileNumberRequestBody,
                        totalDurationRequestBody, audioMultipartBodyPart, isLastRequestBody
                ).execute();

                if (executeUploadFileResponse.isSuccessful()) {
                    if (executeUploadFileResponse.body().getSuccess()) {

                        Data data = executeUploadFileResponse.body().getData();

                        File uploadedAudioFile = new File(Constant.getCallRecordingDir(), data.getFileName().substring(data.getFileName().indexOf("/")));

                        Log.d(TAG, "onSuccess: uploadedAudioFile: exists:" + uploadedAudioFile.exists());

                        if (uploadedAudioFile.exists()) uploadedAudioFile.delete();

                        if (data.getIsLast().equalsIgnoreCase("1")) {
                            context.sendBroadcast(
                                    new Intent().setAction("MANUAL_FILE_UPLOAD_COMPLETE")
                            );
                        }

                    } else {
                        publishProgress((fileCounter + 1) + " file found error while uploading");
                    }
                } else {
                    publishProgress((fileCounter + 1) + " file found http connection fails while uploading");
                }

            } catch (Exception e) {
                System.err.println(e);
                publishProgress(e.getLocalizedMessage());
            }
        }

        @Override
        protected void onProgressUpdate(String... values) {
            super.onProgressUpdate(values);
            //TODO
            Toast toast = Toast.makeText(context, values[0], Toast.LENGTH_LONG);
            toast.setGravity(Gravity.CENTER, 0, 0);
            toast.show();
        }
    }

    class UploadMissedCall extends AsyncTask<String, String, String> {

        private Context context;
        private RFInterface rfInterface;

        public UploadMissedCall(Context context) {
            this.context = context;
            rfInterface = Utility.getRetrofitInterface(Constant.URL);
        }

        @Override
        protected String doInBackground(String... strings) {

            String agentMobileNumber = SharedPrefrenceObj.getSharedValue(context, Constant.AGENT_NUMBBR);
            String agentEmailID = SharedPrefrenceObj.getSharedValue(context, Constant.AGENT_EMAIL);

            DataBaseAdapter dataBaseAdapter = new DataBaseAdapter(context).open();
            Log.d(TAG, "agentMobileNumber: uploadedAudioFile: exists:" + agentMobileNumber);

            ArrayList<MissedCallLog> allMissedCallLog = dataBaseAdapter.getAllMissedCallLog();

            if (allMissedCallLog != null) {
                for (MissedCallLog missedCallLog : allMissedCallLog) {
                    try {
                        Response<RmResultResponse> response = rfInterface.uploadMissedCallLog(
                                missedCallLog.getMobileNumber(), agentMobileNumber,
                                agentEmailID, missedCallLog.getDateTime()
                        ).execute();

                        if (response.isSuccessful()) {
                            if (response.body().getSuccess()) {
                                dataBaseAdapter.deleteMissedCallLog(String.valueOf(missedCallLog.getId()));
                            }
                        }

                    } catch (Exception e) {
                        System.err.println(e);
                    }
                }
            }

            dataBaseAdapter.close();

            return null;
        }
    }

}
