package com.finere.scan_and_go_api.repository;

import com.finere.scan_and_go_api.domain.entity.QrMatrixToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface QrMatrixTokenRepository extends JpaRepository<QrMatrixToken, UUID> {
    Optional<QrMatrixToken> findByPublicToken(String publicToken);
}
