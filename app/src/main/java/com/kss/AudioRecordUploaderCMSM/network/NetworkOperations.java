package com.kss.AudioRecordUploaderCMSM.network;

import android.app.ProgressDialog;
import android.content.Context;

import com.kss.AudioRecordUploaderCMSM.network.callback.NetworkCallback;
import com.kss.AudioRecordUploaderCMSM.utils.Utility;


public abstract class NetworkOperations implements NetworkCallback {
    private ProgressDialog progressDialog;

    public void onStart(Context context, String msg) {
        progressDialog = Utility.createProgressDialog(context);
    }

    public void onComplete() {
        progressDialog.dismiss();
    }
}
