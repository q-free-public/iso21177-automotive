package com.qfree.its.iso21177poc.common.geoflow.thick_client;

import java.time.LocalDateTime;

public class ThickClientPostEvent {
    private LocalDateTime date;
    private int responseCode;
    private String uploadTopic;
    private int contentLength;
    private String error;

    public LocalDateTime getDate() {
        return date;
    }

    public void setDate(LocalDateTime date) {
        this.date = date;
    }

    public int getResponseCode() {
        return responseCode;
    }

    public void setResponseCode(int responseCode) {
        this.responseCode = responseCode;
    }

    public String getUploadTopic() {
        return uploadTopic;
    }

    public void setUploadTopic(String uploadTopic) {
        this.uploadTopic = uploadTopic;
    }

    public int getContentLength() {
        return contentLength;
    }

    public void setContentLength(int contentLength) {
        this.contentLength = contentLength;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }
}
