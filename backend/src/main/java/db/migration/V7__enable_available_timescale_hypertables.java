package db.migration;

import java.sql.Connection;
import java.sql.SQLException;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

/** The same schema works on local TimescaleDB and on managed vanilla PostgreSQL. */
public class V7__enable_available_timescale_hypertables extends BaseJavaMigration {
    @Override
    public Integer getChecksum() {
        return 1;
    }

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        if (!"PostgreSQL".equals(connection.getMetaData().getDatabaseProductName())) return;

        // Supabase PG17 does not ship TimescaleDB. Do not attempt unsupported DDL there.
        if (!hasRows(connection, "SELECT 1 FROM pg_available_extensions WHERE name = 'timescaledb'")) return;
        try (var statement = connection.createStatement()) {
            statement.execute("CREATE EXTENSION IF NOT EXISTS timescaledb");
        }
        String extensionSchema;
        try (var statement = connection.createStatement();
             var rows = statement.executeQuery("SELECT n.nspname FROM pg_extension e "
                     + "JOIN pg_namespace n ON n.oid = e.extnamespace WHERE e.extname = 'timescaledb'")) {
            if (!rows.next()) throw new SQLException("TimescaleDB extension was not installed");
            extensionSchema = rows.getString(1);
        }
        String schema = context.getConfiguration().getDefaultSchema();
        if (schema == null) schema = connection.getSchema();
        for (String table : new String[] { "exercise_results", "body_metrics" }) {
            // Include the configured private application schema, never hardcode public.
            String qualifiedTable = quote(schema) + "." + quote(table);
            try (var statement = connection.prepareStatement("SELECT " + quote(extensionSchema)
                    + ".create_hypertable(CAST(? AS regclass), 'time', "
                    + "if_not_exists => TRUE, migrate_data => TRUE)")) {
                statement.setString(1, qualifiedTable);
                statement.execute();
            }
        }
    }

    private static boolean hasRows(Connection connection, String sql) throws SQLException {
        try (var statement = connection.createStatement(); var rows = statement.executeQuery(sql)) {
            return rows.next();
        }
    }

    private static String quote(String identifier) {
        return "\"" + identifier.replace("\"", "\"\"") + "\"";
    }
}
