package com.studia.aplikacja_trenerska.workout.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.studia.aplikacja_trenerska.workout.dto.ExerciseResponse;
import com.studia.aplikacja_trenerska.workout.entity.Exercise;
import com.studia.aplikacja_trenerska.workout.entity.ExerciseType;
import com.studia.aplikacja_trenerska.workout.entity.MuscleGroup;
import com.studia.aplikacja_trenerska.workout.mapper.ExerciseMapper;
import com.studia.aplikacja_trenerska.workout.repository.ExerciseRepository;

import jakarta.persistence.criteria.Predicate;

@Service
@Transactional(readOnly = true)
public class ExerciseService {
    private final ExerciseRepository exercises;
    private final ExerciseMapper mapper;

    public ExerciseService(ExerciseRepository exercises, ExerciseMapper mapper) {
        this.exercises = exercises;
        this.mapper = mapper;
    }

    public List<ExerciseResponse> findExercises(MuscleGroup muscleGroup, ExerciseType type, String q) {
        String query = q == null ? "" : q.strip();
        if (query.length() > 160) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Wyszukiwana nazwa może mieć maksymalnie 160 znaków");
        }
        // LIKE metacharacters are literal user input, not wildcard operators.
        String pattern = "%" + query.toLowerCase(Locale.ROOT)
                .replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
        Specification<Exercise> filters = (root, criteria, builder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (muscleGroup != null) predicates.add(builder.equal(root.get("muscleGroup"), muscleGroup));
            if (type != null) predicates.add(builder.equal(root.get("type"), type));
            if (!query.isEmpty()) predicates.add(builder.like(builder.lower(root.get("name")), pattern, '\\'));
            return builder.and(predicates.toArray(Predicate[]::new));
        };
        Sort order = Sort.by(Sort.Order.asc("name").ignoreCase(), Sort.Order.asc("id"));
        return exercises.findAll(filters, order).stream().map(mapper::toResponse).toList();
    }
}
