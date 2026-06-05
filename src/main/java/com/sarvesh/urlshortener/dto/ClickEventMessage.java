package com.sarvesh.urlshortener.dto;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClickEventMessage {
    private String shortCode;
    private String ipAddress;
    private String userAgent;
    private String referer;
    private LocalDateTime clickedAt;
}