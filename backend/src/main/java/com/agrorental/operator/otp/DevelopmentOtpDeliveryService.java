package com.agrorental.operator.otp;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Development and testing implementation of OtpDeliveryService.
 * Clearly distinguishes local simulated delivery from production environments
 * where no real SMS provider is configured. Never logs raw OTP values.
 */
@Slf4j
@Service
public class DevelopmentOtpDeliveryService implements OtpDeliveryService {

    @Autowired(required = false)
    private Environment environment;

    @Override
    public void deliverOtp(String mobileNumber, String rawOtp, OtpPurpose purpose) {
        String maskedMobile = maskMobile(mobileNumber);
        if (isProductionOrStagingEnvironment()) {
            log.warn("[NO SMS PROVIDER CONFIGURED] Production/staging SMS provider is not configured. " +
                    "Real SMS delivery is not active. Suppressed dispatch for purpose: {} to mobile: {}", purpose, maskedMobile);
        } else {
            log.info("[DEV OTP MOCK] Simulated dispatch of {} OTP to {} (No external SMS sent)", purpose, maskedMobile);
        }
    }

    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    private boolean isProductionOrStagingEnvironment() {
        if (environment == null) {
            return false;
        }
        String[] activeProfilesArr = environment.getActiveProfiles();
        List<String> activeProfiles = activeProfilesArr != null ? Arrays.asList(activeProfilesArr) : Collections.emptyList();

        String[] defaultProfilesArr = environment.getDefaultProfiles();
        List<String> defaultProfiles = defaultProfilesArr != null ? Arrays.asList(defaultProfilesArr) : Collections.emptyList();

        return activeProfiles.contains("prod") || activeProfiles.contains("staging")
                || defaultProfiles.contains("prod") || defaultProfiles.contains("staging");
    }

    private String maskMobile(String mobile) {
        if (mobile == null || mobile.length() < 4) {
            return "******";
        }
        return "******" + mobile.substring(mobile.length() - 4);
    }
}
