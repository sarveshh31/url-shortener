package com.sarvesh.urlshortener.service;

import com.sarvesh.urlshortener.dto.ShortenRequest;
import com.sarvesh.urlshortener.dto.ShortenResponse;
import com.sarvesh.urlshortener.entity.Url;
import com.sarvesh.urlshortener.repository.UrlRepository;
import com.sarvesh.urlshortener.util.Base62Encoder;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UrlService {

    private final UrlRepository urlRepository;
    private final Base62Encoder encoder;

    @Value("${app.base-url}")
    private String baseUrl;
    public List<Url> getUserUrls(String userId) {
        return urlRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }
    public ShortenResponse shorten(ShortenRequest request, String userId) {
        String code = resolveCode(request.getCustomAlias());
        Url url = Url.builder()
                .shortCode(code)
                .originalUrl(request.getOriginalUrl())
                .userId(userId)
                .createdAt(LocalDateTime.now())
                .expiresAt(request.getExpiryDays() != null
                        ? LocalDateTime.now().plusDays(request.getExpiryDays()) : null)
                .active(true)
                .build();
        urlRepository.save(url);
        return ShortenResponse.builder()
                .shortCode(code)
                .shortUrl(baseUrl + "/" + code)
                .originalUrl(request.getOriginalUrl())
                .expiresAt(url.getExpiresAt())
                .build();
    }

    public String resolve(String code) {
        Url url = urlRepository.findByShortCode(code)
                .orElseThrow(() -> new RuntimeException("Short URL not found"));
        if (!url.isActive()) throw new RuntimeException("URL is inactive");
        if (url.getExpiresAt() != null && url.getExpiresAt().isBefore(LocalDateTime.now()))
            throw new RuntimeException("URL has expired");
        url.setClickCount(url.getClickCount() + 1);
        urlRepository.save(url);
        return url.getOriginalUrl();
    }

    private String resolveCode(String alias) {
        if (alias != null && !alias.isBlank()) {
            if (urlRepository.existsByShortCode(alias))
                throw new RuntimeException("Alias already taken");
            return alias;
        }
        String code;
        do { code = encoder.generateCode(); }
        while (urlRepository.existsByShortCode(code));
        return code;
    }
}