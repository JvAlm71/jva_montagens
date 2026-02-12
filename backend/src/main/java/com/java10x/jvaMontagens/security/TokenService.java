package com.java10x.jvaMontagens.security;

import com.java10x.jvaMontagens.service.DocumentUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;

@Service
public class TokenService {
    private static final String HMAC_ALGORITHM = "HmacSHA256";

    private final String secret;
    private final long expirationHours;

    public TokenService(
            @Value("${security.token.secret}") String secret,
            @Value("${security.token.expiration-hours:12}") long expirationHours
    ) {
        this.secret = secret;
        this.expirationHours = expirationHours;
    }

    public String generateToken(String cpf, String role) {
        String normalizedCpf = DocumentUtils.normalizeCpf(cpf);
        long expiresAt = Instant.now().plus(expirationHours, ChronoUnit.HOURS).getEpochSecond();
        String rawPayload = normalizedCpf + "|" + role + "|" + expiresAt;
        String encodedPayload = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(rawPayload.getBytes(StandardCharsets.UTF_8));
        String signature = sign(encodedPayload);
        return encodedPayload + "." + signature;
    }

    public TokenData parseAndValidate(String token) {
        if (token == null || token.isBlank()) {
            throw new SecurityException("Missing token.");
        }

        String[] parts = token.split("\\.");
        if (parts.length != 2) {
            throw new SecurityException("Invalid token format.");
        }

        String payloadPart = parts[0];
        String signaturePart = parts[1];
        String expectedSignature = sign(payloadPart);

        if (!MessageDigest.isEqual(signaturePart.getBytes(StandardCharsets.UTF_8), expectedSignature.getBytes(StandardCharsets.UTF_8))) {
            throw new SecurityException("Invalid token signature.");
        }

        String decodedPayload = new String(Base64.getUrlDecoder().decode(payloadPart), StandardCharsets.UTF_8);
        String[] payloadValues = decodedPayload.split("\\|");

        if (payloadValues.length != 3) {
            throw new SecurityException("Invalid token payload.");
        }

        String cpf = DocumentUtils.normalizeCpf(payloadValues[0]);
        String role = payloadValues[1];
        long expiresAtEpoch = Long.parseLong(payloadValues[2]);
        Instant expiresAt = Instant.ofEpochSecond(expiresAtEpoch);

        if (expiresAt.isBefore(Instant.now())) {
            throw new SecurityException("Token expired.");
        }

        return new TokenData(cpf, role, expiresAt);
    }

    private String sign(String payload) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            byte[] rawSignature = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(rawSignature);
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to sign token.", ex);
        }
    }

    public record TokenData(
            String cpf,
            String role,
            Instant expiresAt
    ) {}
}
