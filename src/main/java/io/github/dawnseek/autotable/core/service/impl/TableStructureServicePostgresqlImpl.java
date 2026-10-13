package io.github.dawnseek.autotable.core.service.impl;

import io.github.dawnseek.autotable.core.constant.CommonConstant;
import io.github.dawnseek.autotable.core.domain.TableInfo;
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

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Description: table structure service for postgresql
 *
 * @author dawnseek
 * @version 2023/11/6
 */
@Slf4j
@Service("table-structure-postgresql")
public class TableStructureServicePostgresqlImpl implements TableStructureService {
    private final static String LOG_TITLE="【auto create table:POSTGRESQL】";
    private final static String TABLE_SUFFIX_RGX="(_)([1-2][0-9]{5})";
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private String sql(String name) {
        return SqlLoader.getSql(DbTypeHelper.DB_POSTGRESQL, name);
    }

    /**
     * get exists tables by table info
     * @param schema database schema
     * @param tableInfo tableInfo
     * @return exists tables
     */
    @Override
    public List<String> getExistsTables(String schema, TableInfo tableInfo) {
        String tableName = StringUtil.replaceStance(tableInfo.getTableNameExtension(),"%").toUpperCase() ;
        //替换sql式内
        Pattern pattern = Pattern.compile(String.format("^%s$",StringUtil.replaceStance(tableInfo.getTableNameExtension(),tableInfo.getSuffixPattern())));
        List<String> tableList = jdbcTemplate.queryForList(sql(CommonConstant.SQL_QUERY_TABLE_NAMES),schema, tableName).stream().map((a) -> {
            Map node= MapUtil.toCamelCaseMap(a);
            return node.get(CommonConstant.CNAME_TABLE_NAME) + "";
        }).filter((a) -> {
            return pattern.matcher(a).find();
        }).collect(Collectors.toList());
        return tableList;
    }
    /**
     *  get create table sql
     * @param schema db schema
     * @param structureTablename structure table name
     * @param createTableName target table name
     * @param tableSuffix  table suffix
     * @param tableInfo tableInfo
     * @return result
     */
    @Override
    public List<String> getCreateTableSql(String schema,String structureTablename,String createTableName,String tableSuffix,TableInfo tableInfo) {
        String targetTable = schema + "." + createTableName;
        String structureTable = schema + "." + structureTablename;
        String createTableSql = StringUtil.replaceStance(sql(CommonConstant.SQL_QUERY_TABLE_CREATE), targetTable, structureTable);
        List<String> result=new ArrayList<>();
        result.add(DbUtil.safeSql(createTableSql));
        return result;
    }
    /**
     *  get create index sql
     * @param schema db schema
     * @param structureTablename structure table name
     * @param createTableName target table name
     * @param tableSuffix  table suffix
     * @param tableInfo tableInfo
     * @return result
     */
    @Override
    public List<String> getCreateIndexSqls(String schema,String structureTablename,String createTableName,String tableSuffix,TableInfo tableInfo) {
        // postgresql CREATE TABLE (LIKE ... INCLUDING ALL) already copies indexes and constraints
        return new ArrayList<>();
    }

    /**
     *  get create trigger sql
     * @param schema db schema
     * @param structureTablename structure table name
     * @param createTableName target table name
     * @param tableSuffix  table suffix
     * @param tableInfo tableInfo
     * @return result
     */
    @Override
    public List<String> getCreateTriggerSqls(String schema,String structureTablename,String createTableName,String tableSuffix,TableInfo tableInfo) {
        List<Map<String, Object>> triggerRows = jdbcTemplate.queryForList(sql(CommonConstant.SQL_QUERY_TABLE_TRIGGERS), structureTablename.toUpperCase(), schema);
        List<String> createTriggerSqlList = new ArrayList<>();
        Pattern suffixPattern =null;
        if(StringUtil.isNotBlank(tableSuffix)&&null!=tableInfo){
            suffixPattern=Pattern.compile(tableInfo.getSuffixPattern());
        }else{
            suffixPattern=Pattern.compile("");
        }
        for (Map<String, Object> triggerRow : triggerRows) {
            Map<String, Object> node = MapUtil.toCamelCaseMap(triggerRow);
            String triggerName = node.get(CommonConstant.CNAME_TRIGGER_NAME) + "";
            String triggerSql = node.get(CommonConstant.CNAME_TRIGGER_DEF) + "";
            if (suffixPattern.matcher(triggerName).find()) {
                String newTriggerName = triggerName.replaceAll(tableInfo.getSuffixPattern(), tableSuffix);
                triggerSql = triggerSql.replaceAll(Pattern.quote(triggerName), Matcher.quoteReplacement(newTriggerName));
            } else {
                triggerSql = triggerSql.replaceAll(Pattern.quote(triggerName), Matcher.quoteReplacement(String.format("%s_%s", CommonConstant.PREFIX_TRIGGER, UuidUtil.getUuid())));
            }
            triggerSql = triggerSql.replaceAll("(?i)" + Pattern.quote(structureTablename), Matcher.quoteReplacement(createTableName));
            createTriggerSqlList.add(DbUtil.safeSql(triggerSql));
        }
        return createTriggerSqlList;
    }
    /**
     *  get create unique sql
     * @param schema db schema
     * @param structureTablename structure table name
     * @param createTableName target table name
     * @param tableSuffix  table suffix
     * @param tableInfo tableInfo
     * @return result
     */
    @Override
    public List<String> getCreateUniqueSqls(String schema,String structureTablename,String createTableName,String tableSuffix,TableInfo tableInfo) {
        // index has already created
        return new ArrayList<>();
    }

    /**
     *  get create sequence sql. CREATE TABLE (LIKE ...) copies the reference nextval default,
     *  which makes the new table share the reference sequence. Recreate a dedicated sequence
     *  per target table and rebind the column default.
     * @param schema db schema
     * @param structureTablename structure table name
     * @param createTableName target table name
     * @param tableSuffix  table suffix
     * @param tableInfo tableInfo
     * @return result
     */
    @Override
    public List<String> getCreateSequenceSqls(String schema,String structureTablename,String createTableName,String tableSuffix,TableInfo tableInfo) {
        List<Map<String, Object>> sequenceRows = jdbcTemplate.queryForList(sql(CommonConstant.SQL_QUERY_TABLE_SEQUENCES), structureTablename.toUpperCase(), schema);
        List<String> result = new ArrayList<>();
        String targetTable = (schema + "." + createTableName).toLowerCase();
        for (Map<String, Object> sequenceRow : sequenceRows) {
            Map<String, Object> node = MapUtil.toCamelCaseMap(sequenceRow);
            String columnName = node.get(CommonConstant.CNAME_COLUMN_NAME) + "";
            String sequenceName = (schema + "." + createTableName + "_" + columnName + "_seq").toLowerCase();
            result.add(DbUtil.safeSql(String.format("CREATE SEQUENCE %s", sequenceName)));
            result.add(DbUtil.safeSql(String.format("ALTER TABLE %s ALTER COLUMN %s SET DEFAULT nextval('%s')", targetTable, columnName, sequenceName)));
            result.add(DbUtil.safeSql(String.format("ALTER SEQUENCE %s OWNED BY %s.%s", sequenceName, targetTable, columnName)));
        }
        return result;
    }

    /**
     *  get create foreign key sql. CREATE TABLE (LIKE ...) does not copy foreign keys,
     *  so they are rebuilt from pg_constraint with a fresh constraint name.
     * @param schema db schema
     * @param structureTablename structure table name
     * @param createTableName target table name
     * @param tableSuffix  table suffix
     * @param tableInfo tableInfo
     * @return result
     */
    @Override
    public List<String> getCreateForeignKeySqls(String schema,String structureTablename,String createTableName,String tableSuffix,TableInfo tableInfo) {
        List<Map<String, Object>> foreignKeyRows = jdbcTemplate.queryForList(sql(CommonConstant.SQL_QUERY_TABLE_FOREIGN_KEYS), structureTablename.toUpperCase(), schema);
        List<String> result = new ArrayList<>();
        String targetTable = schema + "." + createTableName;
        for (Map<String, Object> foreignKeyRow : foreignKeyRows) {
            Map<String, Object> node = MapUtil.toCamelCaseMap(foreignKeyRow);
            String constraintDef = node.get(CommonConstant.CNAME_CONSTRAINT_DEF) + "";
            String constraintName = String.format("%s_%s", CommonConstant.PREFIX_FOREIGN_KEY, UuidUtil.getUuid());
            result.add(DbUtil.safeSql(String.format("ALTER TABLE %s ADD CONSTRAINT %s %s", targetTable, constraintName, constraintDef)));
        }
        return result;
    }
}
