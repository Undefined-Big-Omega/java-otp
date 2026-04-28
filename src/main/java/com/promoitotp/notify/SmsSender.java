package com.promoitotp.notify;

import com.promoitotp.util.Props;
import org.jsmpp.bean.*;
import org.jsmpp.session.BindParameter;
import org.jsmpp.session.SMPPSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.Properties;

public class SmsSender implements Sender {

    private static final Logger log = LoggerFactory.getLogger(SmsSender.class);

    private final String host;
    private final int    port;
    private final String login;
    private final String pass;
    private final String systemType;
    private final String sender;
    private final TypeOfNumber          srcTon;
    private final NumberingPlanIndicator srcNpi;
    private final TypeOfNumber          dstTon;
    private final NumberingPlanIndicator dstNpi;

    public SmsSender() {
        Properties cfg = Props.load("sms.properties");
        this.host       = Props.get(cfg, "smpp.host",        "localhost");
        this.port       = Integer.parseInt(Props.get(cfg, "smpp.port", "2775"));
        this.login      = Props.get(cfg, "smpp.login",       "smppclient1");
        this.pass       = Props.get(cfg, "smpp.pass",        "password");
        this.systemType = Props.get(cfg, "smpp.system.type", "");
        this.sender     = Props.get(cfg, "smpp.sender",      "PromoOTP");
        this.srcTon     = TypeOfNumber.valueOf(Byte.parseByte(Props.get(cfg, "smpp.src.ton", "5")));
        this.srcNpi     = NumberingPlanIndicator.valueOf(Byte.parseByte(Props.get(cfg, "smpp.src.npi", "0")));
        this.dstTon     = TypeOfNumber.valueOf(Byte.parseByte(Props.get(cfg, "smpp.dst.ton", "1")));
        this.dstNpi     = NumberingPlanIndicator.valueOf(Byte.parseByte(Props.get(cfg, "smpp.dst.npi", "1")));
    }

    @Override
    public void send(String destination, String code, String operationId) throws Exception {
        SMPPSession session = new SMPPSession();
        try {
            session.connectAndBind(host, port, new BindParameter(
                    BindType.BIND_TX, login, pass, systemType,
                    TypeOfNumber.UNKNOWN, NumberingPlanIndicator.UNKNOWN, null));

            String text = "Код подтверждения [" + operationId + "]: " + code;
            session.submitShortMessage(
                    systemType,
                    srcTon, srcNpi, sender,
                    dstTon, dstNpi, destination,
                    new ESMClass(), (byte) 0, (byte) 1,
                    null, null,
                    new RegisteredDelivery(SMSCDeliveryReceipt.DEFAULT),
                    (byte) 0,
                    new GeneralDataCoding(Alphabet.ALPHA_DEFAULT, MessageClass.CLASS1, false),
                    (byte) 0,
                    text.getBytes(StandardCharsets.UTF_8));

            log.info("SMS отправлен на {} (операция={})", destination, operationId);
        } finally {
            session.unbindAndClose();
        }
    }
}
