# autotable

#### Description
Automatic monthly table creation tool based on springboot

#### Software Architecture
- Based on Spring Boot, JdbcTemplate is used, and the Spring Boot data source needs to be configured.
- Standalone single-module project with no parent dependency, resolvable directly from Maven Central.
- Dialect-specific SQL (mysql/oracle/postgresql/guassdb/vastbase/sqlserver) is kept inline in the corresponding Service implementation, with no external XML.
- A single artifact is compatible with both Spring Boot 2.x and 3.x: it only uses Spring APIs common to both, and is compiled to Java 8 bytecode (loadable on JRE 8 and JRE 17).

#### Version compatibility
- The same jar works with both Spring Boot 2.x and Spring Boot 3.x; no version-specific artifact is needed.
- The dependency (`spring-boot-starter-data-jpa`) is `provided`, supplied by the consumer according to its own Spring Boot version.

- Basic principle:
1. The program finds the reference table.
2. Obtain the statement for creating a new table based on the reference table.
3. Obtain the index statement based on the reference table.
4. Obtain the unique key statement based on the reference table.
5. Obtain the trigger statement based on the reference table.
6. Create a new table, index, unique key, and trigger.
#### Instructions
The account must have table management rights. Database table names need to be case-insensitive
Database support:
- mysql
- oracle
- postgresql
- guassdb
- vastbase
- sqlserver

#### Installation
- Add this component as a dependency to your existing system and configure the Spring datasource.

#### configuration
- pom.xml

The project has been published to the Maven Central Repository and can be directly referenced.

The same version is compatible with both springboot2.x and springboot3.x

```xml
<dependency>
    <groupId>io.github.dawnseek</groupId>
    <artifactId>autotable</artifactId>
    <version>1.6</version>
</dependency>

```

- Appliction.class
```java
@ComponentScan(basePackages = {"io.github.dawnseek"})
public class Appliction {
}
```

- application.yml
```yaml
tablecreate:
  enable:
    onstart: false # Whether to create a table when the system starts. default false
    month-first-day: false # Whether to create a table on the first day of the month. default false
  target:
    # The table name to be created. can choose the table name with ${yyyyMM}、${yyyyMMdd}、${yyMM}、${yyyy}.
    # reference table resolution priority: inline in table name (xxxx_$\{yyyyMM\}:referenceTableName) > this reference-table > default max month table
    tables: xxxx_$\{yyyyMM\}
    # global reference table name, empty means use the inline config or auto max month table. value may be a concrete table name, or [max]/[min] for max/min month table
    # reference-table: xxxx_reference
    reference-table:
    # target database type support mysql、oracle、postgresql、guassdb、vastbase、sqlserver
    # resolution priority: tablecreate.dbtype > spring.jpa.database > auto detect from jdbc driver > default mysql
    # when not configured, the database type is auto-detected from the jdbc driver/connection metadata
  dbtype: 
 
```


#### Contribution

1.  Fork the repository
2.  Create Feat_xxx branch
3.  Commit your code
4.  Create Pull Request
