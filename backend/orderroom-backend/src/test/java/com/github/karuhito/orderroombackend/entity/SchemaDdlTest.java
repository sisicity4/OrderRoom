package com.github.karuhito.orderroombackend.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.tool.schema.spi.SchemaManagementToolCoordinator;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/*
 * エンティティから PostgreSQL 用の DDL を生成し、空のDBでスキーマを作れる形になっているかを確認する（#132）。
 * DB には接続しない。実際の PostgreSQL での作成は手動で確認する
 */
class SchemaDdlTest {
    private static final Pattern CREATE_TABLE = Pattern.compile("create table (\\w+) \\((.*)\\)", Pattern.CASE_INSENSITIVE);
    private static final Pattern FOREIGN_KEY = Pattern.compile(
        "alter table if exists (\\w+) add constraint \\w+ foreign key \\(([\\w, ]+)\\)", Pattern.CASE_INSENSITIVE);
    private static final Pattern NAMED_CHECK = Pattern.compile("constraint (\\w+) check", Pattern.CASE_INSENSITIVE);

    private static StandardServiceRegistry registry;
    private static List<String> statements;

    @BeforeAll
    static void generateDdl(@TempDir Path dir) throws Exception {
        registry = new StandardServiceRegistryBuilder()
            .applySetting("hibernate.dialect", PostgreSQLDialect.class.getName())
            .applySetting("hibernate.boot.allow_jdbc_metadata_access", false)
            .build();
        Metadata metadata = new MetadataSources(registry)
            .addAnnotatedClass(Room.class)
            .addAnnotatedClass(Participant.class)
            .addAnnotatedClass(Item.class)
            .addAnnotatedClass(SettlementMember.class)
            .addAnnotatedClass(Settlement.class)
            .addAnnotatedClass(ItemStatusConverter.class)
            .buildMetadata();
        // DB には何もせず、create の DDL だけをファイルへ書き出す
        Path script = dir.resolve("create.sql");
        Map<String, Object> settings = new HashMap<>();
        settings.put("jakarta.persistence.schema-generation.database.action", "none");
        settings.put("jakarta.persistence.schema-generation.scripts.action", "create");
        settings.put("jakarta.persistence.schema-generation.scripts.create-target", script.toString());
        settings.put("hibernate.hbm2ddl.schema-generation.script.append", false);
        settings.put("hibernate.hbm2ddl.delimiter", ";");
        SchemaManagementToolCoordinator.process(metadata, registry, settings, action -> { });
        statements = Files.readAllLines(script).stream().filter(line -> !line.isBlank()).toList();
    }

    @AfterAll
    static void closeRegistry() {
        StandardServiceRegistryBuilder.destroy(registry);
    }

    /* create table の中身を、括弧の外のカンマで、列定義と制約定義に分ける */
    private static List<String> splitDefinitions(String body) {
        List<String> definitions = new ArrayList<>();
        int depth = 0;
        int start = 0;
        for (int i = 0; i < body.length(); i++) {
            char c = body.charAt(i);
            if (c == '(') depth++;
            if (c == ')') depth--;
            if (c == ',' && depth == 0) {
                definitions.add(body.substring(start, i).trim());
                start = i + 1;
            }
        }
        definitions.add(body.substring(start).trim());
        return definitions;
    }

    /* テーブル名 → 列名 → 列定義 */
    private static Map<String, Map<String, String>> columnsByTable() {
        Map<String, Map<String, String>> tables = new HashMap<>();
        for (String statement : statements) {
            Matcher matcher = CREATE_TABLE.matcher(statement.replaceAll(";$", ""));
            if (!matcher.matches()) continue;
            Map<String, String> columns = new HashMap<>();
            for (String definition : splitDefinitions(matcher.group(2))) {
                String lower = definition.toLowerCase();
                if (lower.startsWith("constraint ") || lower.startsWith("primary key")) continue;
                columns.put(definition.split("\\s+")[0], definition);
            }
            tables.put(matcher.group(1), columns);
        }
        return tables;
    }

    @Test
    void createsAllTables() {
        assertThat(columnsByTable().keySet())
            .containsExactlyInAnyOrder("rooms", "participants", "items", "settlement_members", "settlements");
    }

    @Test
    void checkConstraintNamesDoNotCollideInPostgreSql() {
        for (String statement : statements) {
            Matcher matcher = CREATE_TABLE.matcher(statement.replaceAll(";$", ""));
            if (!matcher.matches()) continue;
            String table = matcher.group(1);
            List<String> names = new ArrayList<>();
            for (String definition : splitDefinitions(matcher.group(2))) {
                Matcher named = NAMED_CHECK.matcher(definition);
                boolean hasNamedCheck = false;
                while (named.find()) {
                    names.add(named.group(1));
                    hasNamedCheck = true;
                }
                // 名前のない列の CHECK 制約には、PostgreSQL が「<テーブル>_<列>_check」と名前を付ける
                if (!hasNamedCheck && definition.toLowerCase().contains(" check ")) {
                    names.add(table + "_" + definition.split("\\s+")[0] + "_check");
                }
            }
            assertThat(names).as("%s の CHECK 制約の名前", table).doesNotHaveDuplicates();
        }
    }

    @Test
    void foreignKeyColumnsHaveNoDefault() {
        Map<String, Map<String, String>> tables = columnsByTable();
        List<String> foreignKeyColumns = new ArrayList<>();
        for (String statement : statements) {
            Matcher matcher = FOREIGN_KEY.matcher(statement);
            if (!matcher.find()) continue;
            for (String column : matcher.group(2).split(",\\s*")) {
                foreignKeyColumns.add(matcher.group(1) + "." + column);
            }
        }
        assertThat(foreignKeyColumns).as("外部キーの列が見つかること").isNotEmpty();

        // 参照先の id の既定値（gen_random_uuid()）が、外部キーの列にコピーされていないこと
        List<String> withDefault = foreignKeyColumns.stream()
            .filter(column -> {
                String[] parts = column.split("\\.");
                return tables.get(parts[0]).get(parts[1]).toLowerCase().contains("default");
            })
            .toList();
        assertThat(withDefault).as("既定値が付いている外部キーの列").isEmpty();
    }
}
