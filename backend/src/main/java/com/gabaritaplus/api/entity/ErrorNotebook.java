package com.gabaritaplus.api.entity;

import com.gabaritaplus.api.entity.enums.MasteryStatus;
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

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "error_notebook")
public class ErrorNotebook extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    @Column(nullable = false)
    private Integer errorCount = 1;

    private LocalDate lastReviewedAt;

    private LocalDate nextReviewAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MasteryStatus masteryStatus = MasteryStatus.NEW;
}
