package io.github.dawnseek.autotable.core.helper;

import io.github.dawnseek.autotable.core.utils.StringUtil;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

/**
 * Description: resolve target database type
 *
 * @author dawnseek
 * @version 2025/3/11
 */
@Service
@Scope("singleton")
public class DbTypeHelper implements InitializingBean {

    public static final String DB_MYSQL = "mysql";
    public static final String DB_ORACLE = "oracle";
    public static final String DB_POSTGRESQL = "postgresql";
    public static final String DB_GUASSDB = "guassdb";

    private String dbtype;

    @Value("${tablecreate.dbtype:}")
    private String createDbType;
    @Value("${spring.jpa.database:mysql}")
    private String jpaDbType;

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
            dbtype = createDbType;
        } else {
            dbtype = jpaDbType;
        }
    }
}
