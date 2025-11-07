package com.zephyrcicd.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * TDengine ORM Demo 应用启动类
 *
 * @author zephyr
 */
@SpringBootApplication
public class TdOrmDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(TdOrmDemoApplication.class, args);
        System.out.println("\n========================================");
        System.out.println("TDengine ORM Demo 应用启动成功！");
        System.out.println("运行测试类查看功能演示");
        System.out.println("========================================\n");
    }
}
