package com.studia.aplikacja_trenerska.admin.repository;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Qualifies internal JDBC queries consistently with Hibernate's private schema. */
@Component
public class AdminSql {
    private final String schema;

    public AdminSql(@Value("${spring.jpa.properties.hibernate.default_schema:}") String schema) {
        this.schema = schema;
    }

    public String table(String name) {
        return (schema.isBlank() ? "" : quote(schema) + ".") + quote(name);
    }

    private String quote(String identifier) {
        return "\"" + identifier.replace("\"", "\"\"") + "\"";
    }
}
