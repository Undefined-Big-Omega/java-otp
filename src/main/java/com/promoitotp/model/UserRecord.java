package com.promoitotp.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.OffsetDateTime;

public class UserRecord {
    private Long id;
    private String login;
    @JsonIgnore
    private String passwordHash;
    private String role;
    @JsonProperty("createdAt")
    private OffsetDateTime createdAt;

    public UserRecord() {}

    public UserRecord(String login, String passwordHash, String role) {
        this.login        = login;
        this.passwordHash = passwordHash;
        this.role         = role;
    }

    public Long getId()                    { return id; }
    public void setId(Long id)             { this.id = id; }
    public String getLogin()               { return login; }
    public void setLogin(String v)         { this.login = v; }
    public String getPasswordHash()        { return passwordHash; }
    public void setPasswordHash(String v)  { this.passwordHash = v; }
    public String getRole()                { return role; }
    public void setRole(String v)          { this.role = v; }
    public OffsetDateTime getCreatedAt()   { return createdAt; }
    public void setCreatedAt(OffsetDateTime v) { this.createdAt = v; }
}
