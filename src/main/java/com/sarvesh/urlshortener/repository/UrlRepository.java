package com.sarvesh.urlshortener.repository;
import com.sarvesh.urlshortener.entity.Url;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UrlRepository extends JpaRepository<Url, Long> {

    Optional<Url> findByShortCode(String shortCode);

    List<Url> findByUserIdOrderByCreatedAtDesc(String userId);

    boolean existsByShortCode(String shortCode);
}