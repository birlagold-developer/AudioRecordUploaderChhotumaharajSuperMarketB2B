package com.kss.AudioRecordUploaderCMSM.model;

public class MissedCallLog {

    private int id;
    private String mobileNumber, dateTime;

    public MissedCallLog(int id, String mobileNumber,String dateTime) {
        this.id = id;
        this.mobileNumber = mobileNumber;
        this.dateTime = dateTime;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getMobileNumber() {
        return mobileNumber;
    }

    public void setMobileNumber(String mobileNumber) {
        this.mobileNumber = mobileNumber;
    }

    public String getDateTime() {
        return dateTime;
    }

    public void setDateTime(String dateTime) {
        this.dateTime = dateTime;
    }
}
