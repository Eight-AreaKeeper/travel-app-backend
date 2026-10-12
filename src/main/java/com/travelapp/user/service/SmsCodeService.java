package com.travelapp.user.service;

public interface SmsCodeService {

    /** Generates a code and "sends" it (no real SMS provider is integrated in the MVP stage, see the impl class for details); limits resends for the same phone number to once every 60 seconds */
    void sendCode(String phone);

    /** Checks whether the code is correct and not expired; invalidates it immediately once verified (one-time code) */
    boolean verifyCode(String phone, String code);
}
