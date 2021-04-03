package com.kss.AudioRecordUploaderCMSM.network;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;

import com.kss.AudioRecordUploaderCMSM.network.retrofit.RFInterface;
import com.kss.AudioRecordUploaderCMSM.network.retrofit.responsemodels.RmUploadFileResponse;
import com.kss.AudioRecordUploaderCMSM.utils.Constant;
import com.kss.AudioRecordUploaderCMSM.utils.Utility;

import java.util.Objects;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class WebServiceCalls {
    private static final String TAG = "WebServiceCalls";

    private static RFInterface rfInterface = Utility.getRetrofitInterface(Constant.URL);

    private WebServiceCalls() {
    }

    public static class File {
        private static final String TAG = "File";

        private File() {
        }

        public static synchronized void upload(Context context, String agentMobileNumber, String agentEmailID, String clientMobileNumber, String totalDuration,
                                               java.io.File audioFile, boolean isLast,
                                               final NetworkOperations nwCall) {

            //nwCall.onStart(context, "");
            RequestBody agentMobileNumberRequestBody = RequestBody.create(MediaType.parse("text"), agentMobileNumber);
            RequestBody agentEmailIDRequestBody = RequestBody.create(MediaType.parse("text"), agentEmailID);
            RequestBody clientMobileNumberRequestBody = RequestBody.create(MediaType.parse("text"), clientMobileNumber);
            RequestBody totalDurationRequestBody = RequestBody.create(MediaType.parse("text"), totalDuration);
            RequestBody audioFileRequestBody = RequestBody.create(MediaType.parse("audio"), audioFile);

            RequestBody isLastRequestBody = null;
            if (isLast) {
                isLastRequestBody = RequestBody.create(MediaType.parse("text"), "1");
            } else {
                isLastRequestBody = RequestBody.create(MediaType.parse("text"), "0");
            }

            MultipartBody.Part audioMultipartBodyPart = MultipartBody.Part.createFormData("audio", audioFile.getName(), audioFileRequestBody);

            rfInterface.uploadFile(agentMobileNumberRequestBody, agentEmailIDRequestBody, clientMobileNumberRequestBody,
                    totalDurationRequestBody, audioMultipartBodyPart, isLastRequestBody
            ).enqueue(new Callback<RmUploadFileResponse>() {

                @Override
                public void onResponse(@NonNull Call<RmUploadFileResponse> call, @NonNull Response<RmUploadFileResponse> response) {

                    //nwCall.onComplete();

                    if (response.body().getSuccess()) {
                        Bundle bundle = new Bundle();
                        bundle.putSerializable("data", response.body().getData());
                        nwCall.onSuccess(bundle);
                    } else {
                        Bundle bundle = new Bundle();
                        bundle.putString(Constant.MESSAGE, response.body().getError());
                        nwCall.onFailure(bundle);
                    }
                }

                @Override
                public void onFailure(@NonNull Call<RmUploadFileResponse> call, @NonNull Throwable t) {
                    Log.e(TAG, Objects.requireNonNull(t.getMessage()));

                    //nwCall.onComplete();

                    Bundle bundle = new Bundle();
                    bundle.putString(Constant.MESSAGE, t.getMessage());
                    nwCall.onFailure(bundle);
                }

            });
        }
    }

}