package com.example.scenic_rag_system.common;

import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Base64;

/**
 * 和风天气 JWT 令牌生成工具
 * 使用 EdDSA (Ed25519) 签名算法
 */
@Slf4j
public class QWeatherJwtUtil {

    /**
     * 生成和风天气 API 的 JWT 令牌
     *
     * @param keyId      KEY_ID（如 TDGWMN3YTV）
     * @param projectId  PROJECT_ID（如 398A5FDXVR）
     * @param privateKey BASE64 编码的 Ed25519 私钥
     * @return JWT 令牌字符串
     */
    public static String generateToken(String keyId, String projectId, String privateKey) {
        try {
            // 解析私钥
            String cleanKey = privateKey
                    .replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----END PRIVATE KEY-----", "")
                    .replaceAll("\\s", "");
            byte[] privateKeyBytes = Base64.getDecoder().decode(cleanKey);
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(privateKeyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance("EdDSA");
            PrivateKey pk = keyFactory.generatePrivate(keySpec);

            // Header
            String headerJson = "{\"alg\": \"EdDSA\", \"kid\": \"" + keyId + "\"}";

            // Payload
            long iat = ZonedDateTime.now(ZoneOffset.UTC).toEpochSecond() - 30;
            long exp = iat + 900;
            String payloadJson = "{\"sub\": \"" + projectId + "\", \"iat\": " + iat + ", \"exp\": " + exp + "}";

            // Base64url encode header + payload
            String headerEncoded = Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(headerJson.getBytes(StandardCharsets.UTF_8));
            String payloadEncoded = Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(payloadJson.getBytes(StandardCharsets.UTF_8));
            String data = headerEncoded + "." + payloadEncoded;

            // Sign
            Signature signer = Signature.getInstance("EdDSA");
            signer.initSign(pk);
            signer.update(data.getBytes(StandardCharsets.UTF_8));
            byte[] signature = signer.sign();

            String signatureEncoded = Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(signature);

            return data + "." + signatureEncoded;
        } catch (Exception e) {
            log.error("Failed to generate QWeather JWT token: {}", e.getMessage());
            return "";
        }
    }
}
