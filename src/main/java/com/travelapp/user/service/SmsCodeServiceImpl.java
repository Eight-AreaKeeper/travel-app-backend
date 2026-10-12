package com.travelapp.user.service;

import com.travelapp.common.BusinessException;
import com.travelapp.common.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;

/**
 * MVP-stage verification code implementation: no real SMS provider is integrated yet (Alibaba Cloud/Tencent Cloud
 * integration pending); the code is printed to the logs instead, so for local/integration testing you can just
 * read the code from the backend logs. When integrating a real SMS provider later, just replace the "send" line
 * in sendCode -- the code generation/Redis storage/60-second rate limiting/one-time verification logic can stay as-is.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SmsCodeServiceImpl implements SmsCodeService {

    private static final String CODE_KEY_PREFIX = "sms:code:";
    private static final String LIMIT_KEY_PREFIX = "sms:limit:";
    private static final Duration CODE_TTL = Duration.ofMinutes(5);
    private static final Duration RESEND_LIMIT_TTL = Duration.ofSeconds(60);

    private final StringRedisTemplate redisTemplate;
    private final SecureRandom random = new SecureRandom();

    @Override
    public void sendCode(String phone) {
        String limitKey = LIMIT_KEY_PREFIX + phone;
        Boolean firstRequest = redisTemplate.opsForValue().setIfAbsent(limitKey, "1", RESEND_LIMIT_TTL);
        if (Boolean.FALSE.equals(firstRequest)) {
            throw new BusinessException(ErrorCode.TOO_MANY_REQUESTS, "Verification code sent too frequently, please try again in 60 seconds");
        }

        String code = String.format("%06d", random.nextInt(1_000_000));
        redisTemplate.opsForValue().set(CODE_KEY_PREFIX + phone, code, CODE_TTL);

        // TODO: once a real SMS provider is integrated, replace this log line with the actual SMS send call
        log.info("[MVP stage - code is not sent via SMS, logged only] phone={} code={} (valid for 5 minutes)", phone, code);
    }

    @Override
    public boolean verifyCode(String phone, String code) {
        String key = CODE_KEY_PREFIX + phone;
        String cachedCode = redisTemplate.opsForValue().get(key);
        boolean matched = cachedCode != null && cachedCode.equals(code);
        if (matched) {
            redisTemplate.delete(key);
        }
        return matched;
    }
}
