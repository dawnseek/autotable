package io.github.dawnseek.autotable.core.constant;

/**
 * Description:
 *
 * @author dawnseek
 * @version 2025/3/13
 */
public class CommonConstant {

    public static final String PREFIX_TRIGGER = "TGR";
    public static final String PREFIX_INDEX = "IDX";
    public static final String PREFIX_UNIQUE = "NUI";
    public static final String PREFIX_FOREIGN_KEY = "FKI";
    /**
     * column name
     */
    public static final String CNAME_INDEX_NAME = "indexName";
    public static final String CNAME_COLUMN_NAME ="columnName";
    public static final String CNAME_INDEX_SQL ="indexSql";
    public static final String CNAME_TABLE_NAME = "tableName";
    public static final String CNAME_CREATE_TABLE = "createTable";
    public static final String CNAME_TRIGGER_NAME = "triggerName";
    public static final String CNAME_TRIGGER_DEF = "triggerDef";
    public static final String CNAME_CONSTRAINT_NAME = "constraintName";
    public static final String CNAME_CONSTRAINT_DEF = "constraintDef";

    /**
     * sql resource name (under sql/{dbType}/*.sql)
     */
    public static final String SQL_QUERY_TABLE_NAMES = "query_table_names";
    public static final String SQL_QUERY_TABLE_CREATE = "query_table_create";
    public static final String SQL_QUERY_TABLE_COMMENTS = "query_table_comments";
    public static final String SQL_QUERY_TABLE_TABLE_COMMENT = "query_table_table_comment";
    public static final String SQL_QUERY_TABLE_CONSTRAINT_INDEXES = "query_table_constraint_indexes";
    public static final String SQL_QUERY_TABLE_INDEXES = "query_table_indexes";
    public static final String SQL_QUERY_TABLE_INDEX_CREATE = "query_table_index_create";
    public static final String SQL_QUERY_TABLE_TRIGGERS = "query_table_triggers";
    public static final String SQL_QUERY_TABLE_TRIGGER_CREATE = "query_table_trigger_create";
    public static final String SQL_QUERY_TABLE_SEQUENCES = "query_table_sequences";
    public static final String SQL_QUERY_TABLE_FOREIGN_KEYS = "query_table_foreign_keys";
    public static final String SQL_QUERY_TABLE_COLUMNS = "query_table_columns";
}
