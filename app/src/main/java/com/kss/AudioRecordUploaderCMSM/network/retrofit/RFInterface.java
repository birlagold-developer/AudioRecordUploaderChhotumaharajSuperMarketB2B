package com.kss.AudioRecordUploaderCMSM.network.retrofit;

import com.kss.AudioRecordUploaderCMSM.network.retrofit.responsemodels.RmResultResponse;
import com.kss.AudioRecordUploaderCMSM.network.retrofit.responsemodels.RmUploadFileResponse;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;

public interface RFInterface {

    @Multipart
    @POST("Save_CallRecordAgent")
    Call<RmUploadFileResponse> uploadFile(
            @Part("agent_number") RequestBody agentMobileNumber,
            @Part("email") RequestBody agentEmailID,
            @Part("customer_number") RequestBody clientMobileNumber,
            @Part("call_duration") RequestBody totalDuration,
            @Part MultipartBody.Part audioFile,
            @Part("isLast") RequestBody isLast
    );

    @FormUrlEncoded
    @POST("Missedcall-data")
    Call<RmResultResponse> uploadMissedCallLog(
            @Field("customer_number") String customer_number,
            @Field("agent_number") String agent_number,
            @Field("email") String email,
            @Field("date") String date
    );

}
