package com.jonginout.nettyapiserverjongin;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.support.AbstractApplicationContext;

public class ApiServerMain {
    public static void main(String[] args) {
        AbstractApplicationContext springContext = null;
        try {
            // AnnotationConfigApplicationContext 등록
            springContext = new AnnotationConfigApplicationContext(ApiServerConfig.class);
            springContext.registerShutdownHook();

            // server 실행
            // ApiServer 클래스는 네티의 부트스트랩 설정을 포함한 클래스
            ApiServer server = springContext.getBean(ApiServer.class);
            server.start();
        } finally {
            springContext.close();
        }
    }
}
