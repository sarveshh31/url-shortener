package com.sarvesh.urlshortener.repository;

import com.sarvesh.urlshortener.entity.ClickEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ClickEventRepository extends JpaRepository<ClickEvent, Long> {
    List<ClickEvent> findByShortCodeOrderByClickedAtDesc(String shortCode);
    Long countByShortCode(String shortCode);
}