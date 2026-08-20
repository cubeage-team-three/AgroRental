package com.agrorental.operator.otp;

/**
 * Delivery abstraction for dispatching OTPs to Operator mobile numbers.
 */
public interface OtpDeliveryService {

    void deliverOtp(String mobileNumber, String rawOtp, OtpPurpose purpose);
}
