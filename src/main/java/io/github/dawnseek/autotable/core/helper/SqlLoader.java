package io.github.dawnseek.autotable.core.helper;

import io.github.dawnseek.autotable.core.exception.TableCreateException;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Description: load dialect sql from classpath resource files {@code sql/{dbType}.sql}.
 * Each file is a group of named sections marked by a {@code [name]} line, the following
 * lines (until the next section) are the sql of that name. Pure JDK implementation,
 * no external dependency. Falls back to {@code sql/common.sql} when the dialect file or
 * the specific section does not exist.
 *
 * @author dawnseek
 * @version 2025/3/11
 */
public class SqlLoader {

    private static final String BASE_PATH = "sql/";
    private static final String COMMON = "common";
    private static final String SUFFIX = ".sql";

    private static final Map<String, Map<String, String>> FILE_CACHE = new ConcurrentHashMap<>();

    /**
     * get sql by database type and sql name
     * @param dbType database type, e.g. mysql/oracle/postgresql/guassdb
     * @param name sql section name, e.g. query_table_names
     * @return sql text
     */
    public static String getSql(String dbType, String name) {
        String sql = getSqlFromFile(dbType, name);
        if (null == sql) {
            sql = getSqlFromFile(COMMON, name);
        }
        if (null == sql) {
            throw new TableCreateException(String.format("could not find sql: %s in sql/%s.sql", name, dbType));
        }
        return sql;
    }

    private static String getSqlFromFile(String dbType, String name) {
        Map<String, String> sections = FILE_CACHE.computeIfAbsent(dbType, SqlLoader::loadFile);
        return sections.get(name);
    }

    private static Map<String, String> loadFile(String dbType) {
        String path = BASE_PATH + dbType + SUFFIX;
        Map<String, String> sections = new LinkedHashMap<>();
        try (InputStream in = SqlLoader.class.getClassLoader().getResourceAsStream(path)) {
            if (null == in) {
                return sections;
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                StringBuilder buffer = new StringBuilder();
                String currentName = null;
                String line;
                while ((line = reader.readLine()) != null) {
                    String trimmed = line.trim();
                    if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
                        if (null != currentName) {
                            sections.put(currentName, buffer.toString());
                        }
                        currentName = trimmed.substring(1, trimmed.length() - 1).trim();
                        buffer.setLength(0);
                    } else if (null != currentName && !trimmed.isEmpty() && !trimmed.startsWith("--")) {
                        if (buffer.length() > 0) {
                            buffer.append(' ');
                        }
                        buffer.append(trimmed);
                    }
                }
                if (null != currentName) {
                    sections.put(currentName, buffer.toString());
                }
            }
        } catch (Exception e) {
            throw new TableCreateException(String.format("failed to load sql resource: %s", path));
        }
        return sections;
    }
}
