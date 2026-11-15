package com.studia.aplikacja_trenerska;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.Locale;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import com.studia.aplikacja_trenerska.workout.entity.Exercise;
import com.studia.aplikacja_trenerska.workout.entity.ExerciseType;
import com.studia.aplikacja_trenerska.workout.entity.MuscleGroup;
import com.studia.aplikacja_trenerska.workout.repository.ExerciseRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ExerciseCatalogIntegrationTests {
    @Autowired ExerciseRepository exercises;
    @Autowired Flyway flyway;
    @MockitoBean JavaMailSender mailSender;

    @Test
    void catalogHasCompleteDistinctEntriesWithoutAnAccountOwner() {
        var catalog = exercises.findAll();
        assertThat(catalog).hasSize(48);
        assertThat(catalog).allSatisfy(exercise -> {
            assertThat(exercise.getName()).isNotBlank().hasSizeLessThanOrEqualTo(160);
            assertThat(exercise.getDescription()).isNotBlank().hasSizeLessThanOrEqualTo(2000);
            assertThat(exercise.getType()).isNotNull();
            assertThat(exercise.getMuscleGroup()).isNotNull();
            assertThat(exercise.getCreatedBy()).isNull();
        });
        assertThat(catalog.stream().map(exercise -> exercise.getName().strip().toLowerCase(Locale.ROOT)))
                .doesNotHaveDuplicates();
        assertThat(catalog).extracting(Exercise::getName).contains("Przysiad z masą ciała", "Ściąganie drążka wyciągu górnego");
    }

    @Test
    void catalogCoversEveryMuscleGroupAndBothExerciseTypes() {
        var catalog = exercises.findAll();
        assertThat(catalog).extracting(Exercise::getMuscleGroup)
                .containsAll(Arrays.asList(MuscleGroup.values()));
        for (MuscleGroup group : MuscleGroup.values()) {
            for (ExerciseType type : ExerciseType.values()) {
                assertThat(catalog.stream().filter(exercise -> exercise.getMuscleGroup() == group && exercise.getType() == type))
                        .as("%s / %s", group, type).hasSize(2);
            }
        }
    }

    @Test
    void subsequentFlywayRunDoesNotDuplicateCatalog() {
        var ids = exercises.findAll().stream().map(Exercise::getId).sorted().toList();
        assertThat(ids).hasSize(48);
        assertThat(flyway.migrate().migrationsExecuted).isZero();
        assertThat(exercises.findAll().stream().map(Exercise::getId).sorted().toList()).isEqualTo(ids);
    }
}
