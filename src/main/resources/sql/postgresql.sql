[query_table_names]
select table_name from information_schema.tables where table_schema=? and upper(table_name) like ?

[query_table_create]
show create table ${tableName}

[query_table_indexes]
select index_name,column_name from INFORMATION_SCHEMA.statistics where table_schema = ? and upper(table_name) = ? and index_name not in (select constraint_name from information_schema.table_constraints tc where tc.constraint_type = 'PRIMARY KEY' and tc.table_schema = ? and upper(tc.table_name) = ?)

[query_table_index_create]
SELECT indexname as index_name, indexdef as index_sql FROM pg_indexes where schemaname = ? AND upper(tablename) = ? and indexname =?

[query_table_triggers]
SELECT TRIGGER_NAME FROM information_schema.triggers WHERE TRIGGER_SCHEMA = ? AND upper(EVENT_OBJECT_TABLE) = ?;

[query_table_trigger_create]
SHOW CREATE TRIGGER ${triggerName};
