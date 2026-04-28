package com.promoitotp.notify;

import com.promoitotp.util.Props;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Properties;

public class FileSender implements Sender {

    private static final Logger log = LoggerFactory.getLogger(FileSender.class);
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS");

    private final Path dir;

    public FileSender() {
        Properties cfg = Props.load("app.properties");
        this.dir = Path.of(Props.get(cfg, "otp.file.dir", "otp_files"));
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            throw new IllegalStateException("Не удалось создать директорию " + dir, e);
        }
    }

    @Override
    public void send(String destination, String code, String operationId) throws IOException {
        String filename = operationId.replaceAll("[^a-zA-Z0-9_-]", "_")
                        + "_" + LocalDateTime.now().format(FMT) + ".txt";
        Path target = dir.resolve(filename);

        String content = String.join("\n",
                "Операция : " + operationId,
                "Код      : " + code,
                "Время    : " + LocalDateTime.now(),
                "");

        Files.writeString(target, content, StandardOpenOption.CREATE_NEW);
        log.info("OTP сохранён в файл {} (операция={})", target.toAbsolutePath(), operationId);
    }
}
