package com.finere.scan_and_go_api.domain.entity;

import com.finere.scan_and_go_api.domain.enums.QrMatrixType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "qr_matrix_tokens")
@Getter
@Setter
public class QrMatrixToken {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lot_id")
    private ProductLot lot;

    @Column(name = "public_token", nullable = false, unique = true)
    private String publicToken;

    @Column(name = "signature_hash", nullable = false)
    private String signatureHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "matrix_type", nullable = false, length = 20)
    private QrMatrixType matrixType;
}
