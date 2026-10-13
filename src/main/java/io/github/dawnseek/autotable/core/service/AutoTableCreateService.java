package io.github.dawnseek.autotable.core.service;

import io.github.dawnseek.autotable.core.utils.StringUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Description: auto create table
 *
 * @author dawnseek
 * @version 2023/11/21
 */
@Component
@Slf4j
public class AutoTableCreateService implements CommandLineRunner {
    private static final String LOG_TITLE="【auto create month table】";

    @Value("${tablecreate.enable.onstart:false}")
    private boolean enableOnStart;

    @Value("${tablecreate.enable.month-first-day:false}")
    private boolean enableMonth;

    @Value("${tablecreate.target.tables:}")
    private String targetTableName;

    @Value("${tablecreate.target.reference-table:}")
    private String referenceTableName;

    @Autowired
    private  TableCreateService tableCreateService;
    @Override
    public void run(String... args) throws Exception {
        if(enableOnStart){
            createTables();
        }else{
            log.info("{} closed onstart create table",LOG_TITLE);
        }
    }

    /**
     * auto create month table(every month 1st 00:00:00,auto check create current month and next 3 month table)
     */
    @Scheduled(cron = "0 0 0 1 * ?")
    public void autoCreateTable(){
        if(enableMonth){
           createTables();
        }else{
            log.info("{} closed auto create table",LOG_TITLE);
        }
    }

    private List<String> createTables(){
        String split=":";
        LocalDate now = LocalDate.now();
        //当月第一天
        LocalDateTime startDate= now.withDayOfMonth(1).atStartOfDay();
        //三月后的最后一天
        LocalDateTime endDate= now.plusMonths(3).withDayOfMonth(1).minusDays(1).atStartOfDay();
        if(StringUtil.isBlank(targetTableName)){
            log.error("{}cannot Find config table",LOG_TITLE);
            return new ArrayList<>();
        }
        targetTableName=targetTableName.replace("\\{","{").replace("\\}", "}");
        List<String> tableNames= Arrays.asList(targetTableName.split(","));
        String globalReferenceTableName=StringUtil.isBlank(referenceTableName)?"":referenceTableName.trim();
        List<String> result=new ArrayList<>();
        tableNames.forEach((tableNameConfig)->{
            String tableName=tableNameConfig.trim();
            String referenceTable="";
            if(tableName.contains(split)){
                String[] parts=tableName.split(split,2);
                tableName=parts[0].trim();
                referenceTable=parts[1].trim();
            }
            // 表名内联指定优先, 其次使用全局配置 tablecreate.target.reference-table, 都为空则自动取最大月表
            if(StringUtil.isBlank(referenceTable)){
                referenceTable=globalReferenceTableName;
            }
            result.addAll(tableCreateService.createTable(tableName,startDate,endDate,referenceTable));
        });
        log.info("{} auto create tables:{}",LOG_TITLE, StringUtil.join(result.iterator(),","));
        return result;
    }


}
