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

    @Column(name = "storage_path", length = 1000)
    private String storagePath;

    @Column(name = "original_file_name", length = 255)
    private String originalFileName;

    @Column(name = "source_page")
    private Integer sourcePage;

    @Column(name = "crop_x")
    private Integer cropX;

    @Column(name = "crop_y")
    private Integer cropY;

    @Column(name = "crop_width")
    private Integer cropWidth;

    @Column(name = "crop_height")
    private Integer cropHeight;

    @Column(name = "alt_text", length = 500)
    private String altText;

    @Column(length = 500)
    private String caption;

    @Column(length = 128)
    private String checksum;
}
