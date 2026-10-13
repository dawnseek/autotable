[query_table_names]
select table_name from information_schema.tables where table_schema=? and upper(table_name) like ?

[query_table_create]
CREATE TABLE ${targetTable} (LIKE ${structureTable} INCLUDING ALL)

[query_table_sequences]
SELECT DISTINCT a.attname AS column_name FROM pg_catalog.pg_class c JOIN pg_catalog.pg_namespace n ON n.oid = c.relnamespace JOIN pg_catalog.pg_attribute a ON a.attrelid = c.oid JOIN pg_catalog.pg_attrdef ad ON ad.adrelid = a.attrelid AND ad.adnum = a.attnum WHERE upper(c.relname) = ? AND n.nspname = ? AND c.relkind = 'r' AND a.attnum > 0 AND NOT a.attisdropped AND pg_catalog.pg_get_expr(ad.adbin, ad.adrelid) LIKE 'nextval(%'

[query_table_foreign_keys]
SELECT con.conname AS constraint_name, pg_catalog.pg_get_constraintdef(con.oid) AS constraint_def FROM pg_catalog.pg_constraint con JOIN pg_catalog.pg_class c ON c.oid = con.conrelid JOIN pg_catalog.pg_namespace n ON n.oid = c.relnamespace WHERE con.contype = 'f' AND upper(c.relname) = ? AND n.nspname = ?

[query_table_triggers]
SELECT t.tgname AS trigger_name, pg_catalog.pg_get_triggerdef(t.oid) AS trigger_def FROM pg_catalog.pg_trigger t JOIN pg_catalog.pg_class c ON c.oid = t.tgrelid JOIN pg_catalog.pg_namespace n ON n.oid = c.relnamespace WHERE upper(c.relname) = ? AND n.nspname = ? AND NOT t.tgisinternal
