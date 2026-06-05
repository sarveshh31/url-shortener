package com.sarvesh.urlshortener.controller;

import com.sarvesh.urlshortener.dto.ClickEventMessage;
import com.sarvesh.urlshortener.dto.ShortenRequest;
import com.sarvesh.urlshortener.dto.ShortenResponse;
import com.sarvesh.urlshortener.entity.Url;
import com.sarvesh.urlshortener.entity.ClickEvent;
import com.sarvesh.urlshortener.repository.ClickEventRepository;
import com.sarvesh.urlshortener.repository.UrlRepository;
import com.sarvesh.urlshortener.service.KafkaProducerService;
import com.sarvesh.urlshortener.service.UrlService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
public class UrlController {

    private final UrlService urlService;
    private final KafkaProducerService kafkaProducerService;

    // Added repositories
    private final ClickEventRepository clickEventRepository;
    private final UrlRepository urlRepository;

    @PostMapping("/api/v1/shorten")
    public ResponseEntity<ShortenResponse> shorten(
            @RequestBody @Valid ShortenRequest request,
            Authentication auth) {
        String userId = auth.getName();
        return ResponseEntity.ok(urlService.shorten(request, userId));
    }

    @GetMapping("/{shortCode}")
    public ResponseEntity<Void> redirect(
            @PathVariable String shortCode,
            HttpServletRequest request) {

        String originalUrl = urlService.resolve(shortCode);

        kafkaProducerService.publishClickEvent(
                ClickEventMessage.builder()
                        .shortCode(shortCode)
                        .clickedAt(LocalDateTime.now())
                        .ipAddress(request.getRemoteAddr())
                        .userAgent(request.getHeader("User-Agent"))
                        .referer(request.getHeader("Referer"))
                        .build()
        );

        return ResponseEntity.status(HttpStatus.FOUND)
                .header("Location", originalUrl)
                .build();
    }

    @GetMapping("/api/v1/urls/my")
    public ResponseEntity<List<Url>> myUrls(Authentication auth) {
        return ResponseEntity.ok(urlService.getUserUrls(auth.getName()));
    }

    @GetMapping("/api/v1/analytics/{shortCode}")
    public ResponseEntity<Map<String, Object>> analytics(
            @PathVariable String shortCode) {

        List<ClickEvent> events =
                clickEventRepository.findByShortCodeOrderByClickedAtDesc(shortCode);

        Long total = clickEventRepository.countByShortCode(shortCode);

        Map<String, Object> response = new HashMap<>();
        response.put("shortCode", shortCode);
        response.put("totalClicks", total);
        response.put("recentClicks",
                events.stream().limit(10).collect(Collectors.toList()));

        return ResponseEntity.ok(response);
    }

    @GetMapping("/api/v1/analytics/top")
    public ResponseEntity<List<Map<String, Object>>> topUrls() {

        List<Map<String, Object>> result =
                urlRepository.findAll().stream()
                        .sorted((a, b) ->
                                Long.compare(b.getClickCount(), a.getClickCount()))
                        .limit(10)
                        .map(u -> {
                            Map<String, Object> map = new HashMap<>();
                            map.put("shortCode", u.getShortCode());
                            map.put("originalUrl", u.getOriginalUrl());
                            map.put("clicks", u.getClickCount());
                            return map;
                        })
                        .collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }
}