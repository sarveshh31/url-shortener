package com.sarvesh.urlshortener.controller;

import com.sarvesh.urlshortener.dto.ShortenRequest;
import com.sarvesh.urlshortener.dto.ShortenResponse;
import com.sarvesh.urlshortener.entity.Url;
import com.sarvesh.urlshortener.service.UrlService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class UrlController {

    private final UrlService urlService;

    @PostMapping("/api/v1/shorten")
    public ResponseEntity<ShortenResponse> shorten(
            @RequestBody @Valid ShortenRequest request,
            Authentication auth) {
        String userId = auth.getName();
        return ResponseEntity.ok(urlService.shorten(request, userId));
    }

    @GetMapping("/{shortCode}")
    public ResponseEntity<Void> redirect(@PathVariable String shortCode) {
        String originalUrl = urlService.resolve(shortCode);
        return ResponseEntity.status(HttpStatus.FOUND)
                .header("Location", originalUrl)
                .build();
    }

    @GetMapping("/api/v1/urls/my")
    public ResponseEntity<List<Url>> myUrls(Authentication auth) {
        return ResponseEntity.ok(urlService.getUserUrls(auth.getName()));
    }
}