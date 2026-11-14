package com.example.pos_system;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

class MigrationIntegrationTest extends IntegrationTestBase {

    @Autowired JdbcTemplate jdbc;

    @Test
    void allFlywayMigrationsApplyAndEntitiesMatchSchema() {
    // Context validation = Hibernate validation against the actual PostgreSQL schema
        Integer applied = jdbc.queryForObject(
                "SELECT COUNT(*) FROM flyway_schema_history WHERE success = true", Integer.class);

        assertThat(applied).isGreaterThanOrEqualTo(8);   // V1 … V8
    }

    @Test
    void settingsRowAndAdminAreSeeded() {
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM settings", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM users WHERE email = 'admin@pos.com'", Integer.class)).isEqualTo(1);
    }
}
