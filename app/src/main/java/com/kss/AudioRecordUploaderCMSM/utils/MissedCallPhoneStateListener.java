package com.kss.AudioRecordUploaderCMSM.utils;

import android.content.Context;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;

public class MissedCallPhoneStateListener extends PhoneStateListener {
    private static final String TAG = "CustomPhoneStateListene";

    private Context context;
    private String incoming_nr;
    private int prev_state;

    public MissedCallPhoneStateListener(Context context, String incoming_nr) {
        this.context = context;
        this.incoming_nr = incoming_nr;
    }

    @Override
    public void onCallStateChanged(int state, String incomingNumber) {

        if (incomingNumber != null && incomingNumber.length() > 0) incoming_nr = incomingNumber;
        switch (state) {
            case TelephonyManager.CALL_STATE_RINGING:
                Log.d(TAG, "CALL_STATE_RINGING");
                prev_state = state;
                break;
            case TelephonyManager.CALL_STATE_OFFHOOK:
                Log.d(TAG, "CALL_STATE_OFFHOOK");
                prev_state = state;
                break;
            case TelephonyManager.CALL_STATE_IDLE:
                Log.d(TAG, "CALL_STATE_IDLE==>" + incoming_nr);
                if ((prev_state == TelephonyManager.CALL_STATE_OFFHOOK)) {
                    prev_state = state;
                    //Answered Call which is ended
                }
                if ((prev_state == TelephonyManager.CALL_STATE_RINGING)) {
                    prev_state = state;
                    //Rejected or Missed call
                    //TODO
                    Toast.makeText(context, "Missed Call: " + incomingNumber, Toast.LENGTH_LONG).show();
                }
                break;
        }
    }
}
