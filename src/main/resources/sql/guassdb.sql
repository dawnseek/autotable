[query_table_names]
select table_name from information_schema.tables where table_schema=? and upper(table_name) like ?

[query_table_create]
CREATE table ${targetTable} as table ${tablename}

[query_table_indexes]
SELECT i.indexname as index_name, indisunique, indisprimary, ai.attname as column_name FROM pg_tables t JOIN pg_namespace n ON t.schemaname = n.nspname LEFT JOIN pg_indexes i ON t.schemaname = i.schemaname AND t.tablename = i.tablename LEFT JOIN pg_class ci ON i.indexname = ci.relname AND n.oid = ci.relnamespace LEFT JOIN pg_index ii ON ci.oid = ii.indexrelid LEFT JOIN pg_attribute ai ON ci.oid = ai.attrelid WHERE t.schemaname = ? and upper(t.tablename) = ? ORDER BY t.tablename, i.indexname, ai.attname

[query_table_triggers]
SELECT TRIGGER_NAME FROM information_schema.triggers WHERE TRIGGER_SCHEMA = ? AND upper(EVENT_OBJECT_TABLE) = ?;

[query_table_trigger_create]
SHOW CREATE TRIGGER ${triggerName};
