package com.qfree.its.iso21177poc.common.geoflow.thin_client;

import java.time.LocalDateTime;

public class LogFilePostEvent {
    private LocalDateTime date;
    private int responseCode;
    private String uploadTopic;
    private int contentLength;

    public LogFilePostEvent() {
    }

    public void setDate(LocalDateTime date) {
        this.date = date;
    }

    public void setResponseCode(int responseCode) {
        this.responseCode = responseCode;
    }

    public void setUploadTopic(String uploadTopic) {
        this.uploadTopic = uploadTopic;
    }

    public void setContentLength(int contentLength) {
        this.contentLength = contentLength;
    }

    public LocalDateTime getDate() {
        return date;
    }

    public int getResponseCode() {
        return responseCode;
    }

    public String getUploadTopic() {
        return uploadTopic;
    }

    public int getContentLength() {
        return contentLength;
    }
}
