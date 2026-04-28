package com.promoitotp.notify;

public interface Sender {
    void send(String destination, String code, String operationId) throws Exception;
}
