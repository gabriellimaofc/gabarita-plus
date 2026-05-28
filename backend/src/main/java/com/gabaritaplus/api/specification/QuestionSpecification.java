package com.gabaritaplus.api.specification;

import com.gabaritaplus.api.dto.question.QuestionFilterRequest;
import com.gabaritaplus.api.entity.Question;
import com.gabaritaplus.api.entity.enums.QuestionImportStatus;
import org.springframework.data.jpa.domain.Specification;

import java.util.Collection;
import java.util.List;

public final class QuestionSpecification {

    private QuestionSpecification() {
    }

    public static Specification<Question> byFilters(QuestionFilterRequest filter,
                                                    List<Long> answeredIds,
                                                    List<Long> incorrectIds,
                                                    List<Long> favoriteIds) {
        return Specification.where(equalValue("importStatus", QuestionImportStatus.PUBLISHED))
                .and(likeIgnoreCase("title", filter.search())
                        .or(likeIgnoreCase("statement", filter.search())))
                .and(equalIgnoreCase("subject", filter.subject()))
                .and(equalIgnoreCase("topic", filter.topic()))
                .and(equalIgnoreCase("subtopic", filter.subtopic()))
                .and(equalValue("difficulty", filter.difficulty()))
                .and(equalValue("year", filter.year()))
                .and(equalIgnoreCase("exam", filter.exam()))
                .and(filterByIdSet(Boolean.TRUE.equals(filter.answered()), answeredIds))
                .and(filterByIdSet(Boolean.TRUE.equals(filter.incorrectOnly()), incorrectIds))
                .and(filterByIdSet(Boolean.TRUE.equals(filter.favoritesOnly()), favoriteIds));
    }

    public static Specification<Question> reviewQuestions(
            Collection<QuestionImportStatus> statuses,
            String source,
            Integer year,
            String subject
    ) {
        return Specification.where(inValues("importStatus", statuses))
                .and(equalIgnoreCase("source", source))
                .and(equalValue("sourceYear", year))
                .and(equalIgnoreCase("subject", subject));
    }

    private static Specification<Question> likeIgnoreCase(String field, String value) {
        return (root, query, builder) -> {
            if (value == null || value.isBlank()) {
                return builder.conjunction();
            }
            return builder.like(builder.lower(root.get(field)), "%" + value.toLowerCase() + "%");
        };
    }

    private static Specification<Question> equalIgnoreCase(String field, String value) {
        return (root, query, builder) -> {
            if (value == null || value.isBlank()) {
                return builder.conjunction();
            }
            return builder.equal(builder.lower(root.get(field)), value.toLowerCase());
        };
    }

    private static Specification<Question> equalValue(String field, Object value) {
        return (root, query, builder) -> value == null ? builder.conjunction() : builder.equal(root.get(field), value);
    }

    private static Specification<Question> inValues(String field, Collection<?> values) {
        return (root, query, builder) -> {
            if (values == null || values.isEmpty()) {
                return builder.conjunction();
            }
            return root.get(field).in(values);
        };
    }

    private static Specification<Question> filterByIdSet(boolean enabled, List<Long> ids) {
        return (root, query, builder) -> {
            if (!enabled) {
                return builder.conjunction();
            }
            if (ids == null || ids.isEmpty()) {
                return builder.disjunction();
            }
            return root.get("id").in(ids);
        };
    }
}
