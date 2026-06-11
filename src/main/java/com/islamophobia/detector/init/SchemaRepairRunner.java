package com.islamophobia.detector.init;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Repairs legacy Docker schema drift created by earlier init.sql versions.
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
public class SchemaRepairRunner implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        if (!tableExists("content_analysis")) {
            return;
        }

        repairLegacyContentSchema();
    }

    private void repairLegacyContentSchema() {
        boolean currentTableExists = tableExists("content_item");
        boolean legacyTableExists = tableExists("content_items");

        if (!currentTableExists) {
            log.warn("Skipping legacy schema repair because table content_item does not exist yet.");
            return;
        }

        if (legacyTableExists) {
            int migratedRows = jdbcTemplate.update("""
                INSERT INTO content_item (
                    id, content, title, source_platform, source, source_url,
                    source_type, author, created_at, detected_at
                )
                SELECT
                    legacy.id,
                    COALESCE(legacy.content, legacy.title, legacy.source_id, 'Legacy imported content'),
                    legacy.title,
                    legacy.platform,
                    COALESCE(legacy.url, legacy.source_id),
                    legacy.url,
                    legacy.source_type,
                    legacy.author,
                    COALESCE(legacy.published_at, legacy.collected_at, CURRENT_TIMESTAMP),
                    COALESCE(legacy.collected_at, legacy.published_at, CURRENT_TIMESTAMP)
                FROM content_items legacy
                WHERE NOT EXISTS (
                    SELECT 1
                    FROM content_item current
                    WHERE current.id = legacy.id
                )
                """);

            if (migratedRows > 0) {
                log.info("Migrated {} legacy content rows from content_items to content_item.", migratedRows);
            }
        }

        dropConstraintIfExists("content_analysis", "content_analysis_content_item_id_fkey");
        ensureConstraintExists(
            "fk_content_analysis_content_item",
            "ALTER TABLE content_analysis ADD CONSTRAINT fk_content_analysis_content_item " +
                "FOREIGN KEY (content_item_id) REFERENCES content_item(id)"
        );

        if (legacyTableExists && tableRowCount("content_items") == 0) {
            jdbcTemplate.execute("DROP TABLE IF EXISTS content_items");
            log.info("Dropped empty legacy table content_items after schema repair.");
        }
    }

    private void dropConstraintIfExists(String tableName, String constraintName) {
        if (constraintExists(constraintName)) {
            jdbcTemplate.execute("ALTER TABLE " + tableName + " DROP CONSTRAINT " + constraintName);
            log.info("Dropped legacy constraint {} on {}.", constraintName, tableName);
        }
    }

    private void ensureConstraintExists(String constraintName, String sql) {
        if (!constraintExists(constraintName)) {
            jdbcTemplate.execute(sql);
            log.info("Created missing constraint {}.", constraintName);
        }
    }

    private boolean tableExists(String tableName) {
        Integer count = jdbcTemplate.queryForObject(
            """
            SELECT COUNT(*)
            FROM information_schema.tables
            WHERE table_schema = 'public' AND table_name = ?
            """,
            Integer.class,
            tableName
        );
        return count != null && count > 0;
    }

    private boolean constraintExists(String constraintName) {
        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM pg_constraint WHERE conname = ?",
            Integer.class,
            constraintName
        );
        return count != null && count > 0;
    }

    private int tableRowCount(String tableName) {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + tableName, Integer.class);
        return count == null ? 0 : count;
    }
}

