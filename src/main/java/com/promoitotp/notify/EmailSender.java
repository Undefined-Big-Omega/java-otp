package com.promoitotp.notify;

import com.promoitotp.util.Props;
import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

public class EmailSender implements Sender {

    private static final Logger log = LoggerFactory.getLogger(EmailSender.class);

    private final Session mailSession;
    private final String from;

    public EmailSender() {
        Properties cfg = Props.load("email.properties");

        String host     = Props.get(cfg, "smtp.host", "localhost");
        String port     = Props.get(cfg, "smtp.port", "1025");
        boolean auth    = Boolean.parseBoolean(Props.get(cfg, "smtp.auth", "false"));
        boolean starttls = Boolean.parseBoolean(Props.get(cfg, "smtp.starttls", "false"));
        String user     = Props.get(cfg, "smtp.user", "");
        String pass     = Props.get(cfg, "smtp.pass", "");
        this.from       = Props.get(cfg, "smtp.from", "otp@localhost");

        Properties mailProps = new Properties();
        mailProps.put("mail.smtp.host", host);
        mailProps.put("mail.smtp.port", port);
        mailProps.put("mail.smtp.auth", String.valueOf(auth));
        mailProps.put("mail.smtp.starttls.enable", String.valueOf(starttls));

        if (auth && !user.isBlank()) {
            String finalUser = user;
            String finalPass = pass;
            this.mailSession = Session.getInstance(mailProps, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(finalUser, finalPass);
                }
            });
        } else {
            this.mailSession = Session.getInstance(mailProps);
        }
    }

    @Override
    public void send(String destination, String code, String operationId) throws MessagingException {
        Message msg = new MimeMessage(mailSession);
        msg.setFrom(new InternetAddress(from));
        msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(destination));
        msg.setSubject("Код подтверждения операции");
        msg.setText("Операция: " + operationId + "\nВаш код: " + code);
        Transport.send(msg);
        log.info("Email отправлен на {} (операция={})", destination, operationId);
    }
}
