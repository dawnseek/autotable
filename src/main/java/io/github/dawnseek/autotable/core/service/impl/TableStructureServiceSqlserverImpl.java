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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Description: about sqlserver table structure service
 *
 * @author dawnseek
 * @version 2025/10/13
 */
@Slf4j
@Service("table-structure-sqlserver")
public class TableStructureServiceSqlserverImpl implements TableStructureService {
    private final static String LOG_TITLE="【auto create table:SQLSERVER】";
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private String sql(String name) {
        return SqlLoader.getSql(DbTypeHelper.DB_SQLSERVER, name);
    }
    /**
     * get exists tables by table info
     * @param schema database schema
     * @param tableInfo tableInfo
     * @return exists tables
     */
    @Override
    public List<String> getExistsTables(String schema, TableInfo tableInfo) {
        String tableName = StringUtil.replaceStance(tableInfo.getTableNameExtension(),"%").toUpperCase();
        //替换sql式内
        Pattern pattern = Pattern.compile(String.format("^%s$",StringUtil.replaceStance(tableInfo.getTableNameExtension(),tableInfo.getSuffixPattern())));
        List<String> tableList = jdbcTemplate.queryForList(sql(CommonConstant.SQL_QUERY_TABLE_NAMES),schema, tableName).stream().map((a) -> {
            Map node= MapUtil.toCamelCaseMap(a);
            return node.get(CommonConstant.CNAME_TABLE_NAME) + "";
        }).filter((a) -> {
            return pattern.matcher(a.toUpperCase()).find();
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
        List<Map<String, Object>> columnRows = jdbcTemplate.queryForList(sql(CommonConstant.SQL_QUERY_TABLE_COLUMNS), schema, structureTablename.toUpperCase());
        List<String> columnDefs = new ArrayList<>();
        for (Map<String, Object> columnRow : columnRows) {
            Map<String, Object> node = MapUtil.toCamelCaseMap(columnRow);
            String columnName = node.get(CommonConstant.CNAME_COLUMN_NAME) + "";
            String computedDefinition = node.get("computedDefinition") == null ? null : node.get("computedDefinition").toString();
            StringBuilder columnDef = new StringBuilder();
            if (StringUtil.isNotBlank(computedDefinition)) {
                columnDef.append(String.format("[%s] AS %s", columnName, computedDefinition));
                if (Boolean.TRUE.equals(node.get("computedPersisted"))) {
                    columnDef.append(" PERSISTED");
                }
            } else {
                columnDef.append(String.format("[%s] %s", columnName, formatSqlServerType(node)));
                if (Boolean.TRUE.equals(node.get("isRowguidcol"))) {
                    columnDef.append(" ROWGUIDCOL");
                }
                if (Boolean.TRUE.equals(node.get("isIdentity"))) {
                    columnDef.append(String.format(" IDENTITY(%s,%s)", node.get("identitySeed"), node.get("identityIncrement")));
                }
                if (Boolean.TRUE.equals(node.get("isSparse"))) {
                    columnDef.append(" SPARSE");
                }
                if (Boolean.TRUE.equals(node.get("isFilestream"))) {
                    columnDef.append(" FILESTREAM");
                }
                columnDef.append(Boolean.TRUE.equals(node.get("isNullable")) ? " NULL" : " NOT NULL");
                Object defaultDefinition = node.get("defaultDefinition");
                if (null != defaultDefinition && StringUtil.isNotBlank(defaultDefinition.toString())) {
                    columnDef.append(String.format(" CONSTRAINT [DF_%s] DEFAULT %s", UuidUtil.getUuid(), defaultDefinition));
                }
            }
            columnDefs.add(columnDef.toString());
        }
        String createTableSql = String.format("CREATE TABLE [%s].[%s] (%s)", schema, createTableName, String.join(", ", columnDefs));
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
    public List<String> getCreateIndexSqls(String schema,String structureTablename,String createTableName,String tableSuffix,TableInfo tableInfo){
        List<Map<String, Object>> indexRows = jdbcTemplate.queryForList(sql(CommonConstant.SQL_QUERY_TABLE_INDEXES), schema, structureTablename.toUpperCase());
        Pattern suffixPattern = Pattern.compile("");
        if(StringUtil.isNotBlank(tableSuffix)&&null!=tableInfo){
            suffixPattern=Pattern.compile(tableInfo.getSuffixPattern());
        }
        List<String> result=new ArrayList<>();
        for (Map<String, Object> indexRow : indexRows) {
            Map<String, Object> node = MapUtil.toCamelCaseMap(indexRow);
            String indexName = node.get(CommonConstant.CNAME_INDEX_NAME) + "";
            String columns = node.get("indexColumns") + "";
            String includedColumns = null == node.get("includedColumns") ? "" : node.get("includedColumns").toString();
            String typeDesc = null == node.get("typeDesc") ? "NONCLUSTERED" : node.get("typeDesc").toString();
            String newIndexName;
            if (suffixPattern.matcher(indexName).find()) {
                newIndexName = indexName.replaceAll(tableInfo.getSuffixPattern(), tableSuffix);
            } else {
                newIndexName = String.format("%s_%s", CommonConstant.PREFIX_INDEX, UuidUtil.getUuid());
            }
            String includeClause = StringUtil.isBlank(includedColumns) ? "" : String.format(" INCLUDE (%s)", includedColumns);
            String filterClause = Boolean.TRUE.equals(node.get("hasFilter")) && null != node.get("filterDefinition")
                    ? String.format(" WHERE %s", node.get("filterDefinition")) : "";
            String indexSql;
            if (Boolean.TRUE.equals(node.get("isPrimaryKey"))) {
                indexSql = String.format("ALTER TABLE [%s].[%s] ADD CONSTRAINT [%s] PRIMARY KEY %s (%s)", schema, createTableName, newIndexName, typeDesc, columns);
            } else if (Boolean.TRUE.equals(node.get("isUniqueConstraint"))) {
                indexSql = String.format("ALTER TABLE [%s].[%s] ADD CONSTRAINT [%s] UNIQUE %s (%s)", schema, createTableName, newIndexName, typeDesc, columns);
            } else {
                String unique = Boolean.TRUE.equals(node.get("isUnique")) ? "UNIQUE " : "";
                indexSql = String.format("CREATE %s%s INDEX [%s] ON [%s].[%s] (%s)%s%s", unique, typeDesc, newIndexName, schema, createTableName, columns, includeClause, filterClause);
            }
            result.add(DbUtil.safeSql(indexSql));
        }
        return result;
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
    public List<String> getCreateTriggerSqls(String schema,String structureTablename,String createTableName,String tableSuffix,TableInfo tableInfo) {
        List<Map<String, Object>> triggerRows =jdbcTemplate.queryForList(sql(CommonConstant.SQL_QUERY_TABLE_TRIGGERS), schema, structureTablename.toUpperCase());
        List<String> createTriggerSqlList = new ArrayList<>();
        Pattern suffixPattern = Pattern.compile("");
        if(StringUtil.isNotBlank(tableSuffix)&&null!=tableInfo){
            suffixPattern=Pattern.compile(tableInfo.getSuffixPattern());
        }
        for (Map<String, Object> triggerRow : triggerRows) {
            Map<String, Object> node=MapUtil.toCamelCaseMap(triggerRow);
            String triggerName=node.get(CommonConstant.CNAME_TRIGGER_NAME)+"";
            String triggerSql=node.get(CommonConstant.CNAME_TRIGGER_DEF)+"";
            if(StringUtil.isBlank(triggerSql)){
                continue;
            }
            String newTriggerName;
            if(suffixPattern.matcher(triggerName).find()){
                newTriggerName=triggerName.replaceAll(tableInfo.getSuffixPattern(),tableSuffix);
            }else{
                newTriggerName=String.format("%s_%s",CommonConstant.PREFIX_TRIGGER,UuidUtil.getUuid());
            }
            triggerSql=triggerSql.replaceAll("\\["+Pattern.quote(triggerName)+"\\]",Matcher.quoteReplacement("["+newTriggerName+"]"));
            triggerSql=triggerSql.replaceAll(Pattern.quote(triggerName),Matcher.quoteReplacement(newTriggerName));
            triggerSql=triggerSql.replaceAll("(?i)"+Pattern.quote(structureTablename),Matcher.quoteReplacement(createTableName));
            createTriggerSqlList.add(DbUtil.safeSql(triggerSql));
            if(Boolean.TRUE.equals(node.get("isDisabled"))){
                createTriggerSqlList.add(DbUtil.safeSql(String.format("DISABLE TRIGGER [%s] ON [%s].[%s]", newTriggerName, schema, createTableName)));
            }
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
    public List<String> getCreateUniqueSqls(String schema,String structureTablename,String createTableName,String tableSuffix,TableInfo tableInfo) {
        // unique constraints are created together with indexes
        return new ArrayList<>();
    }

    /**
     *  get create foreign key sql. Rebuild foreign keys with a fresh constraint name.
     * @param schema db schema
     * @param structureTablename structure table name
     * @param createTableName target table name
     * @param tableSuffix  table suffix
     * @param tableInfo tableInfo
     * @return result
     */
    public List<String> getCreateForeignKeySqls(String schema,String structureTablename,String createTableName,String tableSuffix,TableInfo tableInfo) {
        List<Map<String, Object>> foreignKeyRows = jdbcTemplate.queryForList(sql(CommonConstant.SQL_QUERY_TABLE_FOREIGN_KEYS), schema, structureTablename.toUpperCase());
        List<String> result = new ArrayList<>();
        for (Map<String, Object> foreignKeyRow : foreignKeyRows) {
            Map<String, Object> node = MapUtil.toCamelCaseMap(foreignKeyRow);
            String constraintDef = node.get(CommonConstant.CNAME_CONSTRAINT_DEF) + "";
            String constraintName = String.format("%s_%s", CommonConstant.PREFIX_FOREIGN_KEY, UuidUtil.getUuid());
            result.add(DbUtil.safeSql(String.format("ALTER TABLE [%s].[%s] ADD CONSTRAINT [%s] %s", schema, createTableName, constraintName, constraintDef)));
        }
        return result;
    }

    /**
     * format sqlserver column type with length/precision/scale
     * @param node camelCase column metadata
     * @return formatted type
     */
    private String formatSqlServerType(Map<String, Object> node) {
        String typeName = node.get("typeName") + "";
        Integer maxLength = node.get("maxLength") == null ? null : Integer.valueOf(node.get("maxLength").toString());
        Integer precision = node.get("precision") == null ? null : Integer.valueOf(node.get("precision").toString());
        Integer scale = node.get("scale") == null ? null : Integer.valueOf(node.get("scale").toString());
        switch (typeName.toLowerCase()) {
            case "char":
            case "varchar":
            case "binary":
            case "varbinary":
                return -1 == maxLength ? typeName + "(max)" : String.format("%s(%s)", typeName, maxLength);
            case "nchar":
            case "nvarchar":
                return -1 == maxLength ? typeName + "(max)" : String.format("%s(%s)", typeName, maxLength / 2);
            case "decimal":
            case "numeric":
                return String.format("%s(%s,%s)", typeName, precision, scale);
            case "datetime2":
            case "time":
            case "datetimeoffset":
                return String.format("%s(%s)", typeName, scale);
            default:
                return typeName;
        }
    }
}
