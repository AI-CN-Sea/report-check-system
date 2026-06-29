package com.reportcheck;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@MapperScan("com.reportcheck.mapper")
@SpringBootApplication
public class ReportCheckApplication {

    public static void main(String[] args) {
        SpringApplication.run(ReportCheckApplication.class, args);
    }
}
