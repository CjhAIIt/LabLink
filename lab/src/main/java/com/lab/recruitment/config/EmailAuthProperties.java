package com.lab.recruitment.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.email-auth")
public class EmailAuthProperties {

    private String fromName = "实验室招新系统";
    private int codeExpireMinutes = 10;
    private int resendIntervalSeconds = 60;

    public String getFromName() {
        return fromName;
    }

    public void setFromName(String fromName) {
        this.fromName = fromName;
    }

    public int getCodeExpireMinutes() {
        return codeExpireMinutes;
    }

    public void setCodeExpireMinutes(int codeExpireMinutes) {
        this.codeExpireMinutes = codeExpireMinutes;
    }

    public int getResendIntervalSeconds() {
        return resendIntervalSeconds;
    }

    public void setResendIntervalSeconds(int resendIntervalSeconds) {
        this.resendIntervalSeconds = resendIntervalSeconds;
    }
}
