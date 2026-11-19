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
class Sprint3MigrationIntegrationTests {
    @Autowired DataSource dataSource;
    @MockitoBean JavaMailSender mailSender;

    @Test
    void upgradeFromSprint2PreservesAccountsAndPlansAndUsesAvailableTimescale() throws Exception {
        try (var connection = dataSource.getConnection()) {
            assumeTrue("PostgreSQL".equals(connection.getMetaData().getDatabaseProductName()),
                    "Upgrade and hypertable checks run against real PostgreSQL / TimescaleDB");
        }
        String schema = "sprint3_upgrade_" + UUID.randomUUID().toString().replace("-", "");
        Flyway old = Flyway.configure().dataSource(dataSource).locations("classpath:db/migration")
                .defaultSchema(schema).schemas(schema).target("4").load();
        try {
            old.migrate();
            try (var connection = dataSource.getConnection()) {
                connection.setSchema(schema);
                try (var statement = connection.createStatement()) {
                    statement.executeUpdate("INSERT INTO users (email,password_hash,display_name,role,token_version) "
                            + "VALUES ('upgrade@example.test','unused','Existing user','USER',7)");
                    statement.executeUpdate("INSERT INTO workout_plans (owner_id,name) SELECT id,'Existing plan' FROM users");
                    statement.executeUpdate("INSERT INTO plan_days (plan_id,day_index,name) SELECT id,0,'Existing day' FROM workout_plans");
                    statement.executeUpdate("INSERT INTO plan_exercises (plan_day_id,exercise_id,sets,reps,weight,order_index) "
                            + "SELECT d.id,e.id,3,10,42.75,0 FROM plan_days d CROSS JOIN "
                            + "(SELECT MIN(id) id FROM exercises) e");
                }
            }
            Flyway latest = Flyway.configure().dataSource(dataSource).locations("classpath:db/migration")
                    .defaultSchema(schema).schemas(schema).load();
            assertThat(latest.migrate().migrationsExecuted).isEqualTo(3);
            assertThat(latest.migrate().migrationsExecuted).isZero();
            try (var connection = dataSource.getConnection()) {
                connection.setSchema(schema);
                try (var statement = connection.createStatement();
                     var rows = statement.executeQuery("SELECT u.email,u.token_version,p.name,pe.weight FROM users u "
                             + "JOIN workout_plans p ON p.owner_id=u.id JOIN plan_days d ON d.plan_id=p.id "
                             + "JOIN plan_exercises pe ON pe.plan_day_id=d.id")) {
                    assertThat(rows.next()).isTrue();
                    assertThat(rows.getString(1)).isEqualTo("upgrade@example.test");
                    assertThat(rows.getLong(2)).isEqualTo(7);
                    assertThat(rows.getString(3)).isEqualTo("Existing plan");
                    assertThat(rows.getBigDecimal(4)).isEqualByComparingTo("42.75");
                    assertThat(rows.next()).isFalse();
                }
                if ("PostgreSQL".equals(connection.getMetaData().getDatabaseProductName())) {
                    boolean installed;
                    try (var statement = connection.createStatement();
                         var rows = statement.executeQuery("SELECT EXISTS (SELECT 1 FROM pg_extension WHERE extname='timescaledb')")) {
                        rows.next();
                        installed = rows.getBoolean(1);
                    }
                    if (installed) {
                        try (var statement = connection.prepareStatement("SELECT count(*) FROM timescaledb_information.hypertables "
                                + "WHERE hypertable_schema=? AND hypertable_name IN ('exercise_results','body_metrics')")) {
                            statement.setString(1, schema);
                            try (var rows = statement.executeQuery()) {
                                rows.next();
                                assertThat(rows.getInt(1)).isEqualTo(2);
                            }
                        }
                    }
                }
            }
        } finally {
            // This generated test schema contains only this test's data.
            try (var connection = dataSource.getConnection(); var statement = connection.createStatement()) {
                statement.execute("DROP SCHEMA IF EXISTS " + schema + " CASCADE");
            }
        }
    }
}
