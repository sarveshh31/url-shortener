package com.sarvesh.urlshortener.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.hibernate.validator.constraints.URL;

@Data
public class ShortenRequest {
    @NotBlank
    @URL
    private String originalUrl;
    private String customAlias;       // optional
    private Integer expiryDays;        // optional
}