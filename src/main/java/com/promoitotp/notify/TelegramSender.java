package com.promoitotp.notify;

import com.promoitotp.util.Props;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

public class TelegramSender implements Sender {

    private static final Logger log = LoggerFactory.getLogger(TelegramSender.class);

    private final HttpClient http = HttpClient.newHttpClient();
    private final String token;
    private final String chatId;

    public TelegramSender() {
        Properties cfg = Props.load("telegram.properties");
        this.token  = Props.get(cfg, "tg.token");
        this.chatId = Props.get(cfg, "tg.chat");
    }

    @Override
    public void send(String destination, String code, String operationId) throws Exception {
        String text = "Код подтверждения для операции [" + operationId + "]: " + code;
        String url  = "https://api.telegram.org/bot" + token + "/sendMessage"
                    + "?chat_id=" + URLEncoder.encode(chatId, StandardCharsets.UTF_8)
                    + "&text="    + URLEncoder.encode(text,   StandardCharsets.UTF_8);

        HttpRequest req = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();
        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());

        if (resp.statusCode() != 200) {
            throw new RuntimeException("Telegram вернул HTTP " + resp.statusCode() + ": " + resp.body());
        }
        log.info("Telegram сообщение отправлено (операция={})", operationId);
    }
}
