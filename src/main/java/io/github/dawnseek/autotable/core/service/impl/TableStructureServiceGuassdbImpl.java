package io.github.dawnseek.autotable.core.service.impl;

import io.github.dawnseek.autotable.core.constant.CommonConstant;
import io.github.dawnseek.autotable.core.domain.TableInfo;
import io.github.dawnseek.autotable.core.helper.DbTypeHelper;
import io.github.dawnseek.autotable.core.helper.SqlLoader;
import io.github.dawnseek.autotable.core.service.TableStructureService;
import io.github.dawnseek.autotable.core.utils.MapUtil;
import io.github.dawnseek.autotable.core.utils.StringUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Description: about huawei guassdb table structure service
 *
 * @author dawnseek
 * @version 2023/11/6
 */
@Slf4j
@Service("table-structure-guassdb")
public class TableStructureServiceGuassdbImpl implements TableStructureService {
    private final static String LOG_TITLE="【auto create table:GUASSDB】";
    private final static String TABLE_SUFFIX_RGX="(_)([1-2][0-9]{5})";
    private final static Pattern TABLE_SUFFIX_PATTERN=Pattern.compile(TABLE_SUFFIX_RGX);
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private String sql(String name) {
        return SqlLoader.getSql(DbTypeHelper.DB_GUASSDB, name);
    }
    /**
     * get exists tables by table info
     * @param schema database schema
     * @param tableInfo tableInfo
     * @return exists tables
     */
    @Override
    public List<String> getExistsTables(String schema, TableInfo tableInfo) {

        String tableName = StringUtil.replaceStance(tableInfo.getTableNameExtension(),"%") ;
        //替换sql式内
        Pattern pattern = Pattern.compile(String.format("^%s$",StringUtil.replaceStance(tableInfo.getTableNameExtension(),tableInfo.getSuffixPattern())));
        List<String> tableList = jdbcTemplate.queryForList(sql(CommonConstant.SQL_QUERY_TABLE_NAMES),schema, tableName).stream().map((a) -> {
            Map node= MapUtil.toCamelCaseMap(a);
            return node.get("tableName") + "";
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
        String createSql = StringUtil.replaceStance(sql(CommonConstant.SQL_QUERY_TABLE_CREATE),createTableName, structureTablename);
        List<String> result=new ArrayList<>();
        result.add(createSql);
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
        List<Map<String, Object>> indexs=jdbcTemplate.queryForList(sql(CommonConstant.SQL_QUERY_TABLE_INDEXES),schema,structureTablename);
        return indexs.stream().map((Map a)->{
            Map node=MapUtil.toCamelCaseMap(a);
            String indexName=node.get("indexName")+"";
            //处理月表后缀
            indexName=indexName.replaceAll(TABLE_SUFFIX_RGX,"_"+tableSuffix);
            if(!TABLE_SUFFIX_PATTERN.matcher(indexName).find()){
                indexName=indexName+"_"+tableSuffix;
            }
            String columnName=node.get("columnName")+"";
            Boolean indisPrimary= (Boolean) Optional.ofNullable(node.get("indisprimary")).orElse(false);
            Boolean indisunique= (Boolean) Optional.ofNullable(node.get("indisunique")).orElse(false);
            if(indisPrimary){
                return String.format("ALTER TABLE %s ADD CONSTRAINT %s PRIMARY KEY (%s)",createTableName,indexName,columnName);
            }else if(indisunique){
                return String.format("ALTER TABLE %s ADD CONSTRAINT %s UNIQUE (%s)",createTableName,indexName,columnName);
            }else{
                return String.format(" CREATE INDEX %s ON %s (%s);",indexName,createTableName,columnName);
            }
        }).collect(Collectors.toList());
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
        List<String> triggerNameList =jdbcTemplate.queryForList(sql(CommonConstant.SQL_QUERY_TABLE_TRIGGERS),schema, structureTablename).stream().map((a) -> {
            Map node=MapUtil.toCamelCaseMap(a);
            return a.get("triggerName") + "";
        }).collect(Collectors.toList());
        List<String> createTriggerSqlList = new ArrayList<>();
        for (String triggerName : triggerNameList) {
            List<String> createTriggerSqlRowList =jdbcTemplate.queryForList(StringUtil.replaceStance(sql(CommonConstant.SQL_QUERY_TABLE_TRIGGER_CREATE), triggerName)).stream().map((a) -> {
                return a.get("sql original statement") + "";
            }).collect(Collectors.toList());
            createTriggerSqlList.addAll(createTriggerSqlRowList.stream().map((a)->{return a.replaceAll(TABLE_SUFFIX_RGX,""+tableSuffix);}).collect(Collectors.toList()));
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
        return new ArrayList<>();
    }
}
