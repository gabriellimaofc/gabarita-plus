package com.gabaritaplus.api.repository;

import com.gabaritaplus.api.entity.OfficialExamSource;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OfficialExamSourceRepository extends JpaRepository<OfficialExamSource, Long> {

    List<OfficialExamSource> findByExamIgnoreCaseAndYearAndDay(String exam, Integer year, Integer day);

    List<OfficialExamSource> findByExamIgnoreCaseAndYear(String exam, Integer year);
}
