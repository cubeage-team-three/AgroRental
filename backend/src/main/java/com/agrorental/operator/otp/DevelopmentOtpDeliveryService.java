package com.agrorental.operator.otp;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Development and testing implementation of OtpDeliveryService.
 * Protects sensitive phone numbers and OTP values from standard production logs.
 */
@Slf4j
@Service
public class DevelopmentOtpDeliveryService implements OtpDeliveryService {

    @Override
    public void deliverOtp(String mobileNumber, String rawOtp, OtpPurpose purpose) {
        String maskedMobile = maskMobile(mobileNumber);
        log.info("[OTP Dispatch] Dispatched {} OTP to {}", purpose, maskedMobile);
    }

    private String maskMobile(String mobile) {
        if (mobile == null || mobile.length() < 4) {
            return "******";
        }
        return "******" + mobile.substring(mobile.length() - 4);
    }
}
