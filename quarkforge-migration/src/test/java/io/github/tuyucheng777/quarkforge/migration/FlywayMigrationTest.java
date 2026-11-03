package io.github.tuyucheng777.quarkforge.migration;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

import javax.sql.DataSource;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
class FlywayMigrationTest {

    @Inject
    DataSource dataSource;

    @Test
    void appliesVersionedMigrationsOnStartup() throws Exception {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(
                     "select count(*) from \"flyway_schema_history\" where \"type\" = 'SQL'")) {
            assertTrue(resultSet.next());
            assertEquals(2, resultSet.getInt(1));
        }

        // 第二版脚本新增的 note 列必须已存在
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "insert into qf_migration_demo (id, name, note) values (1, 'demo', 'ok')")) {
            assertEquals(1, statement.executeUpdate());
        }
    }
}
