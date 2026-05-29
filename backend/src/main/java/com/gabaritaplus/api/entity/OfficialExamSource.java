package com.gabaritaplus.api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "official_exam_sources")
public class OfficialExamSource extends BaseEntity {

    @Column(nullable = false, length = 120)
    private String exam;

    @Column(nullable = false)
    private Integer year;

    @Column(name = "exam_day")
    private Integer day;

    @Column(name = "book_color", length = 40)
    private String bookColor;

    @Column(name = "pdf_url", nullable = false, length = 1000)
    private String pdfUrl;

    @Column(name = "answer_key_url", length = 1000)
    private String answerKeyUrl;

    @Column(name = "source_url", nullable = false, length = 1000)
    private String sourceUrl;

    @Column(name = "local_pdf_path", length = 1000)
    private String localPdfPath;

    @Column(name = "answer_key_map_json", columnDefinition = "TEXT")
    private String answerKeyMapJson;
}
