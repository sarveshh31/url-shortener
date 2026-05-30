package com.sarvesh.urlshortener.service;

import com.sarvesh.urlshortener.repository.UrlRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AsyncService {

    private final UrlRepository urlRepository;

    @Async
    public void updateClickCount(String code) {
        urlRepository.findByShortCode(code).ifPresent(url -> {
            url.setClickCount(url.getClickCount() + 1);
            urlRepository.save(url);
        });
    }
}