[query_table_names]
SELECT TABLE_NAME FROM USER_TABLES WHERE UPPER(TABLE_NAME) LIKE ? ORDER BY TABLE_NAME DESC

[query_table_create]
SELECT dbms_metadata.get_ddl('TABLE', '${tableName}' ) AS TABLE_NAME FROM DUAL

[query_table_comments]
SELECT TABLE_NAME,COLUMN_NAME,COMMENTS from user_col_comments where table_name = ? and COMMENTS IS NOT NULL

[query_table_constraint]
select au.constraint_name,cu.column_name from user_constraints au LEFT JOIN user_cons_columns cu on cu.constraint_name = au.constraint_name where au.constraint_type in ('U','P') and au.table_name = ?

[query_table_indexes]
SELECT ui.INDEX_NAME,uic.column_name FROM ALL_INDEXES ui LEFT JOIN ALL_IND_COLUMNS uic ON ui.INDEX_NAME=uic.INDEX_NAME WHERE ui.table_name = ? and ui.uniqueness !='UNIQUE'

[query_table_index_create]
CREATE INDEX ${indexName} ON ${tableName}(${columnName})

[query_table_triggers]
select trigger_name from all_triggers where table_name=?

[query_table_trigger_create]
select a.text as a from all_source a where type='TRIGGER' AND name=? ORDER BY line
