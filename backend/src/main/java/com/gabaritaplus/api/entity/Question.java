package com.gabaritaplus.api.entity;

import com.gabaritaplus.api.entity.enums.DifficultyLevel;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "questions")
public class Question extends BaseEntity {

    @Column(nullable = false, length = 180)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String statement;

    @Column(length = 500)
    private String imageUrl;

    @Column(nullable = false, length = 80)
    private String subject;

    @Column(nullable = false, length = 120)
    private String topic;

    @Column(length = 120)
    private String subtopic;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DifficultyLevel difficulty;

    @Column(nullable = false)
    private Integer year;

    @Column(nullable = false, length = 120)
    private String exam;

    @Column(length = 120)
    private String competency;

    @Column(length = 120)
    private String ability;

    @Column(columnDefinition = "TEXT")
    private String explanation;

    @Column(nullable = false, length = 1)
    private String correctAlternative;

    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Alternative> alternatives = new ArrayList<>();
}
