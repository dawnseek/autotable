# autotable

#### 介绍
基于springboot的自动月表创建工具

#### 软件架构
- 基于springboot 使用了JdbcTemplate 需要配置springboot数据源
- 独立单模块工程，无父项目依赖，可直接从 Maven Central 解析
- 各方言（mysql/oracle/postgresql/guassdb/vastbase/sqlserver）建表 SQL 以常量形式内聚在对应 Service 实现中，无外部 XML
- 单一制品同时兼容 Spring Boot 2.x 与 3.x：仅使用两版通用的 Spring API，且编译为 Java 8 字节码（可在 JRE 8 与 JRE 17 上加载）

#### 版本兼容
- 同一个 jar 同时兼容 Spring Boot 2.x 与 Spring Boot 3.x，无需按版本引入不同制品
- 组件依赖（`spring-boot-starter-data-jpa`）为 `provided`，由使用方按自身 Spring Boot 版本提供


- 基本原理：
1. 程序找到参考的表。
2. 根据参考表获取创建新表语句
3. 根据参考表获取索引语句
4. 根据参考表获取唯一键语句
5. 根据参考表获取触发器语音
6、创建新表、索引、唯一键、触发器
#### 使用说明
需要账号拥有表管理权限。需要数据库表名忽略大小写

数据库支持：
- mysql
- oracle
- postgresql
- guassdb(高斯数据库)
- vastbase(海量数据库)
- sqlserver
#### 安装教程

- 将本组件作为依赖引入原有系统，并配置 Spring 数据源即可。

#### 配置
- pom.xml
项目已经发布到maven中心仓库，可以直接进行引用

同一个版本同时兼容 springboot2.x 与 springboot3.x

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
    onstart: false # 启动程序检查并创建月表.默认 false
    month-first-day: false # 是否以每月1号为每月的第一天，默认false
  target:
    # 表名配置，支持变量： ${yyyyMM}、${yyyyMMdd}、${yyMM}、${yyyy}。
    # 参考表解析优先级：表名内联指定(xxxx_$\{yyyyMM\}:参考表名) > 本配置 reference-table > 默认以最大月表为参考表
    tables: xxxx_$\{yyyyMM\}
    # 全局参考表名，为空时按表名内联配置或自动取最大月表。参考表可取具体表名，或 [max]/[min] 取最大/最小月表
    # reference-table: xxxx_reference
    reference-table: 
  # 目标数据库类型，支持 mysql、oracle、postgresql、guassdb、vastbase、sqlserver
  # 解析优先级：tablecreate.dbtype > spring.jpa.database > 从数据库驱动自动识别 > 默认 mysql
  # 未配置时可从 JDBC 驱动/连接元数据自动识别数据库类型
  dbtype: 
```
#### 参与贡献

1.  Fork 本仓库
2.  新建 Feat_xxx 分支
3.  提交代码
4.  新建 Pull Request
