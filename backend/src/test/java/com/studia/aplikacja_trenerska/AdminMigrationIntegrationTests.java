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
class AdminMigrationIntegrationTests {
    @Autowired DataSource dataSource;
    @MockitoBean JavaMailSender mailSender;

    @Test
    void upgradeFromV7PreservesAccountsProgressAndGroupsWithoutGrantingAdministrator() throws Exception {
        try (var connection = dataSource.getConnection()) {
            assumeTrue("PostgreSQL".equals(connection.getMetaData().getDatabaseProductName()));
        }
        String schema = "admin_upgrade_" + UUID.randomUUID().toString().replace("-", "");
        try {
            Flyway.configure().dataSource(dataSource).locations("classpath:db/migration")
                    .schemas(schema).defaultSchema(schema).target("7").load().migrate();
            try (var connection = dataSource.getConnection()) {
                connection.setSchema(schema);
                try (var statement = connection.createStatement()) {
                    statement.executeUpdate("INSERT INTO users (email,password_hash,display_name,role,enabled,token_version) "
                            + "VALUES ('existing@example.test','unchanged-hash','Existing','TRAINER',TRUE,9)");
                    statement.executeUpdate("INSERT INTO workout_plans (owner_id,name) SELECT id,'Existing plan' FROM users");
                    statement.executeUpdate("INSERT INTO body_metrics (id,time,user_id,weight_kg) "
                            + "SELECT gen_random_uuid(),NOW(),id,75.25 FROM users");
                    statement.executeUpdate("INSERT INTO training_groups (trainer_id,name) SELECT id,'Existing group' FROM users");
                    statement.executeUpdate("INSERT INTO exercise_results (id,time,user_id,exercise_id,exercise_name,plan_name,day_name,set_no,reps,weight) "
                            + "SELECT gen_random_uuid(),NOW(),u.id,e.id,e.name,'Existing plan','Day',1,10,42.5 "
                            + "FROM users u CROSS JOIN (SELECT id,name FROM exercises ORDER BY id LIMIT 1) e");
                }
            }
            Flyway latest = Flyway.configure().dataSource(dataSource).locations("classpath:db/migration")
                    .schemas(schema).defaultSchema(schema).target("8").load();
            assertThat(latest.migrate().migrationsExecuted).isEqualTo(1);
            assertThat(latest.migrate().migrationsExecuted).isZero();
            try (var connection = dataSource.getConnection()) {
                connection.setSchema(schema);
                try (var statement = connection.createStatement();
                     var rows = statement.executeQuery("SELECT role,enabled,blocked,token_version,password_hash FROM users")) {
                    assertThat(rows.next()).isTrue();
                    assertThat(rows.getString(1)).isEqualTo("TRAINER");
                    assertThat(rows.getBoolean(2)).isTrue();
                    assertThat(rows.getBoolean(3)).isFalse();
                    assertThat(rows.getLong(4)).isEqualTo(9);
                    assertThat(rows.getString(5)).isEqualTo("unchanged-hash");
                    assertThat(rows.next()).isFalse();
                }
                for (String table : new String[] {"workout_plans", "body_metrics", "exercise_results", "training_groups", "admin_control"}) {
                    try (var statement = connection.createStatement();
                         var rows = statement.executeQuery("SELECT COUNT(*) FROM " + table)) {
                        rows.next();
                        assertThat(rows.getLong(1)).as(table).isEqualTo(1);
                    }
                }
                try (var statement = connection.createStatement();
                     var rows = statement.executeQuery("SELECT bootstrap_completed FROM admin_control WHERE id=1")) {
                    rows.next();
                    assertThat(rows.getBoolean(1)).isFalse();
                }
            }
        } finally {
            try (var connection = dataSource.getConnection(); var statement = connection.createStatement()) {
                statement.execute("DROP SCHEMA IF EXISTS " + schema + " CASCADE");
            }
        }
    }
}
