package io.github.dawnseek.autotable.core.helper;

import io.github.dawnseek.autotable.core.utils.StringUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;

/**
 * Description: resolve target database type.
 * Priority: {@code tablecreate.dbtype} -&gt; {@code spring.jpa.database} -&gt; jdbc driver metadata -&gt; mysql.
 *
 * @author dawnseek
 * @version 2025/3/11
 */
@Service
@Scope("singleton")
@Slf4j
public class DbTypeHelper implements InitializingBean {

    public static final String DB_MYSQL = "mysql";
    public static final String DB_ORACLE = "oracle";
    public static final String DB_POSTGRESQL = "postgresql";
    public static final String DB_GUASSDB = "guassdb";
    public static final String DB_VASTBASE = "vastbase";
    public static final String DB_SQLSERVER = "sqlserver";

    private final static String LOG_TITLE = "【auto create table:db type】";

    private String dbtype;

    @Value("${tablecreate.dbtype:}")
    private String createDbType;
    @Value("${spring.jpa.database:}")
    private String jpaDbType;

    @Autowired(required = false)
    private DataSource dataSource;

    /**
     * get dialect db type
     * @return db type
     */
    public String getDialectDBType() {
        return dbtype;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        if (StringUtil.isNotBlank(createDbType)) {
            dbtype = normalize(createDbType);
            log.info("{}resolve db type from tablecreate.dbtype: {}", LOG_TITLE, dbtype);
        } else if (StringUtil.isNotBlank(jpaDbType)) {
            dbtype = normalize(jpaDbType);
            log.info("{}resolve db type from spring.jpa.database: {}", LOG_TITLE, dbtype);
        } else {
            dbtype = detectFromDriver();
            log.info("{}resolve db type from jdbc driver: {}", LOG_TITLE, dbtype);
        }
    }

    /**
     * detect db type from the jdbc driver/connection metadata, fallback to mysql.
     * @return db type
     */
    private String detectFromDriver() {
        if (null == dataSource) {
            return DB_MYSQL;
        }
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            String byUrl = normalizeByKeyword(metaData.getURL());
            if (StringUtil.isNotBlank(byUrl)) {
                return byUrl;
            }
            String byProduct = normalizeByKeyword(metaData.getDatabaseProductName());
            if (StringUtil.isNotBlank(byProduct)) {
                return byProduct;
            }
        } catch (Exception e) {
            log.error("{}detect db type from jdbc driver fail!", LOG_TITLE, e);
        }
        return DB_MYSQL;
    }

    /**
     * normalize a configured db type (alias/tag tolerant).
     * @param dbType raw db type
     * @return canonical db type
     */
    private String normalize(String dbType) {
        String value = dbType.trim().toLowerCase().replace('-', '_').replace(' ', '_');
        switch (value) {
            case "mysql":
            case "mariadb":
                return DB_MYSQL;
            case "oracle":
            case "orcl":
                return DB_ORACLE;
            case "postgres":
            case "postgresql":
            case "pgsql":
            case "pg":
                return DB_POSTGRESQL;
            case "gaussdb":
            case "guassdb":
            case "opengauss":
            case "open_gauss":
                return DB_GUASSDB;
            case "vastbase":
                return DB_VASTBASE;
            case "sqlserver":
            case "sql_server":
            case "mssql":
            case "microsoft_sql_server":
                return DB_SQLSERVER;
            default:
                return value;
        }
    }

    /**
     * match a url/product name against known dialect keywords.
     * @param keyword jdbc url or database product name
     * @return canonical db type, or blank when unknown
     */
    private String normalizeByKeyword(String keyword) {
        if (StringUtil.isBlank(keyword)) {
            return "";
        }
        String value = keyword.toLowerCase();
        if (value.contains("vastbase")) {
            return DB_VASTBASE;
        }
        if (value.contains("opengauss") || value.contains("gaussdb") || value.contains("guassdb")) {
            return DB_GUASSDB;
        }
        if (value.contains("postgres")) {
            return DB_POSTGRESQL;
        }
        if (value.contains("sqlserver") || value.contains("microsoft sql server")) {
            return DB_SQLSERVER;
        }
        if (value.contains("oracle")) {
            return DB_ORACLE;
        }
        if (value.contains("mysql") || value.contains("mariadb")) {
            return DB_MYSQL;
        }
        return "";
    }
}
