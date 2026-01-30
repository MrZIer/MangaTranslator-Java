package com.example.mangaTrans.service;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.stereotype.Service;

/**
 * 浏览器指纹生成服务
 */
@Slf4j
@Service
public class FingerprintService {
    
    /**
     * 生成浏览器指纹（基于IP和User-Agent）
     */
    public String generateFingerprint(HttpServletRequest request) {
        String ip = getClientIpAddress(request);
        String userAgent = request.getHeader("User-Agent");
        
        String rawFingerprint = ip + "|" + userAgent;
        String fingerprint = DigestUtils.sha256Hex(rawFingerprint);
        
        log.debug("Generated fingerprint for IP: {}", ip);
        return fingerprint;
    }
    
    /**
     * 获取客户端真实IP地址（考虑代理情况）
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String[] headers = {
            "X-Forwarded-For",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP",
            "HTTP_X_FORWARDED_FOR",
            "HTTP_X_FORWARDED",
            "HTTP_X_CLUSTER_CLIENT_IP",
            "HTTP_CLIENT_IP",
            "HTTP_FORWARDED_FOR",
            "HTTP_FORWARDED",
            "HTTP_VIA",
            "REMOTE_ADDR"
        };
        
        for (String header : headers) {
            String ip = request.getHeader(header);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                // 取第一个IP（如果有多个）
                return ip.split(",")[0].trim();
            }
        }
        
        return request.getRemoteAddr();
    }
}
