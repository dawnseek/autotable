package io.github.dawnseek.autotable.core.service.impl;

import io.github.dawnseek.autotable.core.constant.CommonConstant;
import io.github.dawnseek.autotable.core.domain.TableInfo;
import io.github.dawnseek.autotable.core.exception.TableCreateException;
import io.github.dawnseek.autotable.core.helper.DbTypeHelper;
import io.github.dawnseek.autotable.core.helper.SqlLoader;
import io.github.dawnseek.autotable.core.service.TableStructureService;
import io.github.dawnseek.autotable.core.utils.DbUtil;
import io.github.dawnseek.autotable.core.utils.MapUtil;
import io.github.dawnseek.autotable.core.utils.StringUtil;
import io.github.dawnseek.autotable.core.utils.UuidUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.Reader;
import java.sql.Clob;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Description: oracle table structure service
 *
 * @author dawnseek
 * @version 2023/11/6
 */
@Slf4j
@Service("table-structure-oracle")
public class TableStructureServiceOracleImpl implements TableStructureService {
    private final static String LOG_TITLE = "【auto create table:ORACLE】";
    private final static Pattern CONSTRAINT_PATTERN = Pattern.compile("CONSTRAINT\\s+(\"[^\"]+\"|\\S+)", Pattern.CASE_INSENSITIVE);
    private final static Pattern INDEX_CLAUSE_PATTERN = Pattern.compile("USING INDEX\\b(?:(?!\\bCONSTRAINT\\b).)*?\\b(ENABLE|DISABLE)\\b", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private String sql(String name) {
        return SqlLoader.getSql(DbTypeHelper.DB_ORACLE, name);
    }

    /**
     * get exists tables by table info
     *
     * @param schema    database schema (owner)
     * @param tableInfo tableInfo
     * @return exists tables
     */
    @Override
    public List<String> getExistsTables(String schema, TableInfo tableInfo) {
        String tableName = StringUtil.replaceStance(tableInfo.getTableNameExtension(), "%").toUpperCase();
        //替换sql式内
        Pattern pattern = Pattern.compile(String.format("^%s$", StringUtil.replaceStance(tableInfo.getTableNameExtension(), tableInfo.getSuffixPattern()).toUpperCase()));
        List<String> tableList = jdbcTemplate.queryForList(sql(CommonConstant.SQL_QUERY_TABLE_NAMES), owner(schema), tableName).stream().map((a) -> {
            Map node = MapUtil.toCamelCaseMap(a);
            return node.get(CommonConstant.CNAME_TABLE_NAME) + "";
        }).filter((a) -> {
            return pattern.matcher(a).find();
        }).collect(Collectors.toList());
        return tableList;
    }

    /**
     * get create table sql
     *
     * @param schema             db schema (owner)
     * @param structureTablename structure table name
     * @param createTableName    target table name
     * @param tableSuffix        table suffix
     * @param tableInfo          tableInfo
     * @return result
     */
    @Override
    public List<String> getCreateTableSql(String schema, String structureTablename, String createTableName, String tableSuffix, TableInfo tableInfo) {
        String createQuerySql = StringUtil.replaceStance(sql(CommonConstant.SQL_QUERY_TABLE_CREATE), structureTablename.toUpperCase(), owner(schema));
        String createTableSql = toText(querySingle(getDdl(createQuerySql), "table", structureTablename));
        //替换表名为目标表名
        createTableSql = createTableSql.replaceAll("(?i)" + Pattern.quote(structureTablename), createTableName);
        //重命名约束, 避免与参考表的约束名冲突
        Matcher constraintMatcher = CONSTRAINT_PATTERN.matcher(createTableSql);
        StringBuffer createTableSqlTemp = new StringBuffer();
        while (constraintMatcher.find()) {
            constraintMatcher.appendReplacement(createTableSqlTemp, Matcher.quoteReplacement(String.format("CONSTRAINT \"%s_%s\"", CommonConstant.PREFIX_UNIQUE, UuidUtil.getUuid())));
        }
        constraintMatcher.appendTail(createTableSqlTemp);
        createTableSql = createTableSqlTemp.toString();
        //移除约束内的 USING INDEX 子句(约束索引会随约束自动重建, 保留会引用旧索引名)
        createTableSql = INDEX_CLAUSE_PATTERN.matcher(createTableSql).replaceAll("");
        List<String> result = new ArrayList<>();
        result.add(DbUtil.safeSql(createTableSql));
        //表描述
        List<Map<String, Object>> tableCommentList = jdbcTemplate.queryForList(sql(CommonConstant.SQL_QUERY_TABLE_TABLE_COMMENT), owner(schema), structureTablename.toUpperCase());
        tableCommentList.forEach((Map a) -> {
            Map node = MapUtil.toCamelCaseMap(a);
            String comment = node.get("comments") + "";
            result.add(DbUtil.safeSql(String.format("COMMENT ON TABLE %s IS '%s'", createTableName, escape(comment))));
        });
        //字段描述
        List<Map<String, Object>> columCommentList = jdbcTemplate.queryForList(sql(CommonConstant.SQL_QUERY_TABLE_COMMENTS), owner(schema), structureTablename.toUpperCase());
        columCommentList.forEach((Map a) -> {
            Map node = MapUtil.toCamelCaseMap(a);
            String comment = node.get("comments") + "";
            //增加描述
            String addCommentSql = String.format("COMMENT ON COLUMN %s.%s IS '%s'", createTableName, node.get(CommonConstant.CNAME_COLUMN_NAME), escape(comment));
            result.add(DbUtil.safeSql(addCommentSql));
        });
        return result;
    }

    /**
     * get create index sql
     *
     * @param schema             db schema (owner)
     * @param structureTablename structure table name
     * @param createTableName    target table name
     * @param tableSuffix        table suffix
     * @param tableInfo          tableInfo
     * @return result
     */
    @Override
    public List<String> getCreateIndexSqls(String schema, String structureTablename, String createTableName, String tableSuffix, TableInfo tableInfo) {
        String owner = owner(schema);
        String upperTable = structureTablename.toUpperCase();
        //查询由 主键/唯一约束 隐式创建的索引, 这些索引随约束一并创建, 不可再单独创建(否则 ORA-01408)
        Set<String> constraintIndexNames = jdbcTemplate.queryForList(sql(CommonConstant.SQL_QUERY_TABLE_CONSTRAINT_INDEXES), owner, upperTable).stream()
                .map((a) -> {
                    Map node = MapUtil.toCamelCaseMap(a);
                    return (node.get(CommonConstant.CNAME_INDEX_NAME) + "").toUpperCase();
                }).collect(Collectors.toSet());
        List<String> indexNameList = jdbcTemplate.queryForList(sql(CommonConstant.SQL_QUERY_TABLE_INDEXES), owner, upperTable).stream()
                .map((a) -> {
                    Map node = MapUtil.toCamelCaseMap(a);
                    return node.get(CommonConstant.CNAME_INDEX_NAME) + "";
                }).filter((indexName) -> {
                    return StringUtil.isNotBlank(indexName) && !constraintIndexNames.contains(indexName.toUpperCase());
                }).collect(Collectors.toList());
        Pattern suffixPattern = Pattern.compile("");
        if (StringUtil.isNotBlank(tableSuffix) && null != tableInfo) {
            suffixPattern = Pattern.compile(tableInfo.getSuffixPattern());
        }
        List<String> tableIndexSql = new ArrayList<>();
        for (String indexName : indexNameList) {
            String newIndexName;
            if (suffixPattern.matcher(indexName).find()) {
                newIndexName = indexName.replaceAll(tableInfo.getSuffixPattern(), tableSuffix);
            } else {
                newIndexName = String.format("%s_%s", CommonConstant.PREFIX_INDEX, UuidUtil.getUuid());
            }
            String indexDdl = toText(querySingle(getDdl(StringUtil.replaceStance(sql(CommonConstant.SQL_QUERY_TABLE_INDEX_CREATE), indexName.toUpperCase(), owner)), "index", indexName));
            //替换索引名与表名
            indexDdl = indexDdl.replaceAll("(?i)" + Pattern.quote(indexName), Matcher.quoteReplacement(newIndexName));
            indexDdl = indexDdl.replaceAll("(?i)" + Pattern.quote(structureTablename), Matcher.quoteReplacement(createTableName));
            tableIndexSql.add(DbUtil.safeSql(indexDdl));
        }
        return tableIndexSql;
    }

    /**
     * get create trigger sql
     *
     * @param schema             db schema (owner)
     * @param structureTablename structure table name
     * @param createTableName    target table name
     * @param tableSuffix        table suffix
     * @param tableInfo          tableInfo
     * @return result
     */
    public List<String> getCreateTriggerSqls(String schema, String structureTablename, String createTableName, String tableSuffix, TableInfo tableInfo) {
        String owner = owner(schema);
        String upperTable = structureTablename.toUpperCase();
        List<String> triggerNameList = jdbcTemplate.queryForList(sql(CommonConstant.SQL_QUERY_TABLE_TRIGGERS), owner, upperTable).stream().map((a) -> {
            Map node = MapUtil.toCamelCaseMap(a);
            return node.get(CommonConstant.CNAME_TRIGGER_NAME) + "";
        }).filter(StringUtil::isNotBlank).collect(Collectors.toList());
        List<String> createTriggerSqlList = new ArrayList<>();
        Pattern suffixPattern = Pattern.compile("");
        if (StringUtil.isNotBlank(tableSuffix) && null != tableInfo) {
            suffixPattern = Pattern.compile(tableInfo.getSuffixPattern());
        }
        for (String triggerName : triggerNameList) {
            List<String> createTriggerSqlRowList = jdbcTemplate.queryForList(sql(CommonConstant.SQL_QUERY_TABLE_TRIGGER_CREATE), owner, triggerName.toUpperCase()).stream().map((a) -> {
                Map node = MapUtil.toCamelCaseMap(a);
                return node.get("text") + "";
            }).collect(Collectors.toList());
            StringBuilder stringBuilder = new StringBuilder("CREATE ");
            for (String createTriggerSqlRow : createTriggerSqlRowList) {
                stringBuilder.append(createTriggerSqlRow);
            }
            String triggerSql = stringBuilder.toString().replaceAll("(?i)" + Pattern.quote(structureTablename), Matcher.quoteReplacement(createTableName));
            if (suffixPattern.matcher(triggerName).find()) {
                String newTriggerName = triggerName.replaceAll(tableInfo.getSuffixPattern(), tableSuffix);
                triggerSql = triggerSql.replaceAll("(?i)" + Pattern.quote(triggerName), Matcher.quoteReplacement(newTriggerName));
            } else {
                triggerSql = triggerSql.replaceAll("(?i)" + Pattern.quote(triggerName), Matcher.quoteReplacement(String.format("%s_%s", CommonConstant.PREFIX_TRIGGER, UuidUtil.getUuid())));
            }
            createTriggerSqlList.add(DbUtil.safeSql(triggerSql));
        }
        return createTriggerSqlList;
    }

    /**
     * get create unique sql
     *
     * @param schema             db schema (owner)
     * @param structureTablename structure table name
     * @param createTableName    target table name
     * @param tableSuffix        table suffix
     * @param tableInfo          tableInfo
     * @return result
     */
    public List<String> getCreateUniqueSqls(String schema, String structureTablename, String createTableName, String tableSuffix, TableInfo tableInfo) {
        //create table sql already has unique sql
        return new ArrayList<>();
    }

    /**
     * normalize schema/owner, oracle owner is upper case.
     * @param schema raw schema
     * @return owner
     */
    private String owner(String schema) {
        return StringUtil.isBlank(schema) ? null : schema.toUpperCase();
    }

    /**
     * execute a ddl query and return rows.
     * @param querySql query sql
     * @return rows
     */
    private List<Map<String, Object>> getDdl(String querySql) {
        return jdbcTemplate.queryForList(querySql);
    }

    /**
     * get the single result value, with a clear error when dbms_metadata returns no row.
     * @param rows query rows
     * @param type object type (for logging)
     * @param name object name (for logging)
     * @return result value
     */
    private Object querySingle(List<Map<String, Object>> rows, String type, String name) {
        if (null == rows || rows.isEmpty() || rows.get(0).isEmpty()) {
            throw new TableCreateException(String.format("dbms_metadata.get_ddl returned no ddl for %s %s", type, name));
        }
        return rows.get(0).values().iterator().next();
    }

    /**
     * convert a query result value (possibly a CLOB) to text.
     * dbms_metadata.get_ddl returns CLOB and oracle drivers may not render it via toString().
     * @param value query result value
     * @return text
     */
    private String toText(Object value) {
        if (null == value) {
            return "";
        }
        if (value instanceof Clob) {
            return readClob((Clob) value);
        }
        return value.toString();
    }

    /**
     * read a clob fully via character stream (safer than substring for large clobs).
     * @param clob clob
     * @return text
     */
    private String readClob(Clob clob) {
        StringBuilder builder = new StringBuilder();
        try (Reader reader = clob.getCharacterStream()) {
            if (null == reader) {
                return clob.getSubString(1, (int) clob.length());
            }
            BufferedReader bufferedReader = new BufferedReader(reader);
            char[] buffer = new char[4096];
            int length;
            while ((length = bufferedReader.read(buffer)) != -1) {
                builder.append(buffer, 0, length);
            }
        } catch (Exception e) {
            throw new TableCreateException("read clob fail", e);
        }
        return builder.toString();
    }

    /**
     * escape a value used inside an oracle string literal.
     * @param value raw value
     * @return escaped value
     */
    private String escape(String value) {
        return null == value ? "" : value.replace("'", "''");
    }
}
