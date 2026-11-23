package com.studia.aplikacja_trenerska;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.util.UUID;
import javax.sql.DataSource;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@ActiveProfiles("test")
class Sprint4MigrationIntegrationTests {
    @Autowired DataSource dataSource;
    @MockitoBean JavaMailSender mailSender;

    @Test
    void upgradeFromV8KeepsPlansPrivateAndAddsScheduleWithCascades() throws Exception {
        try (var connection = dataSource.getConnection()) {
            assumeTrue("PostgreSQL".equals(connection.getMetaData().getDatabaseProductName()));
        }
        String schema = "sprint4_upgrade_" + UUID.randomUUID().toString().replace("-", "");
        try {
            Flyway.configure().dataSource(dataSource).locations("classpath:db/migration")
                    .schemas(schema).defaultSchema(schema).target("8").load().migrate();
            try (var connection = dataSource.getConnection()) {
                connection.setSchema(schema);
                try (var statement = connection.createStatement()) {
                    statement.executeUpdate("INSERT INTO users (email,password_hash,display_name,role,enabled,token_version) "
                            + "VALUES ('existing@example.test','unchanged-hash','Existing','USER',TRUE,4)");
                    statement.executeUpdate("INSERT INTO workout_plans (owner_id,name) SELECT id,'Existing plan' FROM users");
                    statement.executeUpdate("INSERT INTO plan_days (plan_id,day_index,name) SELECT id,0,'Existing day' FROM workout_plans");
                }
            }
            Flyway latest = Flyway.configure().dataSource(dataSource).locations("classpath:db/migration")
                    .schemas(schema).defaultSchema(schema).target("9").load();
            assertThat(latest.migrate().migrationsExecuted).isEqualTo(1);
            assertThat(latest.migrate().migrationsExecuted).isZero();
            try (var connection = dataSource.getConnection()) {
                connection.setSchema(schema);
                try (var statement = connection.createStatement();
                     var rows = statement.executeQuery("SELECT name,is_shared,share_token FROM workout_plans")) {
                    assertThat(rows.next()).isTrue();
                    assertThat(rows.getString(1)).isEqualTo("Existing plan");
                    assertThat(rows.getBoolean(2)).isFalse();
                    assertThat(rows.getString(3)).isNull();
                    assertThat(rows.next()).isFalse();
                }
                try (var statement = connection.createStatement()) {
                    statement.executeUpdate("INSERT INTO scheduled_workouts (user_id,plan_id,day_index,plan_name,day_name,scheduled_date) "
                            + "SELECT owner_id,id,0,name,'Existing day',DATE '2026-12-01' FROM workout_plans");
                    statement.executeUpdate("DELETE FROM workout_plans");
                    try (var rows = statement.executeQuery("SELECT plan_id,plan_name FROM scheduled_workouts")) {
                        assertThat(rows.next()).isTrue();
                        assertThat(rows.getObject(1)).isNull();
                        assertThat(rows.getString(2)).isEqualTo("Existing plan");
                    }
                    statement.executeUpdate("DELETE FROM users");
                    try (var rows = statement.executeQuery("SELECT COUNT(*) FROM scheduled_workouts")) {
                        rows.next();
                        assertThat(rows.getLong(1)).isZero();
                    }
                }
            }
        } finally {
            try (var connection = dataSource.getConnection(); var statement = connection.createStatement()) {
                statement.execute("DROP SCHEMA IF EXISTS " + schema + " CASCADE");
            }
        }
    }
}
