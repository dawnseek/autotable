[query_table_names]
select table_name from information_schema.tables where table_schema=? and upper(table_name) like ?

[query_table_create]
show create table ${tableName}

[query_table_triggers]
SELECT TRIGGER_NAME FROM information_schema.triggers WHERE TRIGGER_SCHEMA = ? AND upper(EVENT_OBJECT_TABLE) = ?;

[query_table_trigger_create]
SHOW CREATE TRIGGER ${triggerName};
