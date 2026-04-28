package com.promoitotp.model;

import java.time.OffsetDateTime;

public class OtpEntry {
    private Long         id;
    private String       operationId;
    private String       code;
    private OtpStatus    status;
    private Long         userId;
    private OffsetDateTime createdAt;
    private OffsetDateTime expiresAt;

    public OtpEntry() {}

    public Long getId()                       { return id; }
    public void setId(Long id)                { this.id = id; }
    public String getOperationId()            { return operationId; }
    public void setOperationId(String v)      { this.operationId = v; }
    public String getCode()                   { return code; }
    public void setCode(String v)             { this.code = v; }
    public OtpStatus getStatus()              { return status; }
    public void setStatus(OtpStatus v)        { this.status = v; }
    public Long getUserId()                   { return userId; }
    public void setUserId(Long v)             { this.userId = v; }
    public OffsetDateTime getCreatedAt()      { return createdAt; }
    public void setCreatedAt(OffsetDateTime v){ this.createdAt = v; }
    public OffsetDateTime getExpiresAt()      { return expiresAt; }
    public void setExpiresAt(OffsetDateTime v){ this.expiresAt = v; }
}
