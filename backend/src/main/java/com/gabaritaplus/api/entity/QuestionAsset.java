package com.gabaritaplus.api.entity;

import com.gabaritaplus.api.entity.enums.QuestionAssetType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "question_assets")
public class QuestionAsset extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "alternative_id")
    private Alternative alternative;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private QuestionAssetType type;

    @Column(length = 1000)
    private String url;

    @Column(length = 1000)
    private String storagePath;

    @Column(length = 255)
    private String originalFileName;

    private Integer sourcePage;

    private Integer cropX;

    private Integer cropY;

    private Integer cropWidth;

    private Integer cropHeight;

    @Column(length = 500)
    private String altText;

    @Column(length = 500)
    private String caption;

    @Column(length = 128)
    private String checksum;
}
