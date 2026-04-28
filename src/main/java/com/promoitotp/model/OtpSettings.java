package com.promoitotp.model;

public class OtpSettings {
    private int codeLength;
    private int ttlSeconds;

    public OtpSettings() {}
    public OtpSettings(int codeLength, int ttlSeconds) {
        this.codeLength = codeLength;
        this.ttlSeconds = ttlSeconds;
    }

    public int getCodeLength()            { return codeLength; }
    public void setCodeLength(int v)      { this.codeLength = v; }
    public int getTtlSeconds()            { return ttlSeconds; }
    public void setTtlSeconds(int v)      { this.ttlSeconds = v; }
}
