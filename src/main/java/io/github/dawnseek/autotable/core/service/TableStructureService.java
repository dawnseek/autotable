package io.github.dawnseek.autotable.core.service;

import io.github.dawnseek.autotable.core.domain.TableInfo;

import java.util.List;

/**
 * Description: table structure service
 *
 * @author dawnseek
 * @version 2023/11/6
 */
public interface TableStructureService {

    /**
     * get exists tables by table info
     * @param schema database schema
     * @param tableInfo tableInfo
     * @return exists tables
     */
    List<String> getExistsTables(String schema, TableInfo tableInfo);

    /**
     *  get create table sql
     * @param schema db schema
     * @param structureTablename structure table name
     * @param createTableName target table name
     * @param tableSuffix  table suffix
     * @param tableInfo tableInfo
     * @return result
     */
    List<String> getCreateTableSql(String schema,String structureTablename,String createTableName,String tableSuffix,TableInfo tableInfo);

    /**
     *  get create index sql
     * @param schema db schema
     * @param structureTablename structure table name
     * @param createTableName target table name
     * @param tableSuffix  table suffix
     * @param tableInfo tableInfo
     * @return result
     */
    List<String> getCreateIndexSqls(String schema,String structureTablename,String createTableName,String tableSuffix,TableInfo tableInfo);

    /**
     *  get create trigger sql
     * @param schema db schema
     * @param structureTablename structure table name
     * @param createTableName target table name
     * @param tableSuffix  table suffix
     * @param tableInfo tableInfo
     * @return result
     */
    List<String> getCreateTriggerSqls(String schema,String structureTablename,String createTableName,String tableSuffix,TableInfo tableInfo);

    /**
     *  get create unique sql
     * @param schema db schema
     * @param structureTablename structure table name
     * @param createTableName target table name
     * @param tableSuffix  table suffix
     * @param tableInfo tableInfo
     * @return result
     */
    List<String> getCreateUniqueSqls(String schema,String structureTablename,String createTableName,String tableSuffix,TableInfo tableInfo);

    /**
     *  get create sequence sql (for dialects that clone a table but need a fresh sequence per target table)
     * @param schema db schema
     * @param structureTablename structure table name
     * @param createTableName target table name
     * @param tableSuffix  table suffix
     * @param tableInfo tableInfo
     * @return result
     */
    default List<String> getCreateSequenceSqls(String schema,String structureTablename,String createTableName,String tableSuffix,TableInfo tableInfo){
        return new java.util.ArrayList<>();
    }

    /**
     *  get create foreign key sql (for dialects whose table clone does not copy foreign keys)
     * @param schema db schema
     * @param structureTablename structure table name
     * @param createTableName target table name
     * @param tableSuffix  table suffix
     * @param tableInfo tableInfo
     * @return result
     */
    default List<String> getCreateForeignKeySqls(String schema,String structureTablename,String createTableName,String tableSuffix,TableInfo tableInfo){
        return new java.util.ArrayList<>();
    }
}
