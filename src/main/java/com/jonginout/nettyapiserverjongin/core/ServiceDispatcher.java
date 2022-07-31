package com.jonginout.nettyapiserverjongin.core;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * ServiceDispatcher 클래스는 스프링을 기초로 하여
 * HTTP 요청의 URL과
 * HTTP 메서드에 해당하는 API 서비스 클래스를 생성하여
 * ApiRequest 인터페이스 형태로 돌려준다.
 */
@Component
public class ServiceDispatcher {
    private static ApplicationContext springContext;

    @Autowired
    public void init(ApplicationContext springContext) {
        ServiceDispatcher.springContext = springContext;
    }

    protected Logger logger = LogManager.getLogger(this.getClass());

    // HTTP 요청에서 추출한 값을 가진 맵 책체를 인수로
    public static ApiRequest dispatch(Map<String, String> requestMap) {
        // HTTP 요청의 URL을 보고 이 값을 기준으로 API 서비스 클래스를 생성한다.
        String serviceUri = requestMap.get("REQUEST_URI");
        String beanName = null;

        if (serviceUri == null) {
            beanName = "notFound";
        }

        if (serviceUri.startsWith("/tokens")) {
            String httpMethod = requestMap.get("REQUEST_METHOD");

            switch (httpMethod) {
                case "POST":
                    beanName = "tokenIssue";
                    break;
                case "DELETE":
                    beanName = "tokenExpier";
                    break;
                case "GET":
                    beanName = "tokenVerify";
                    break;
                default:
                    beanName = "notFound";
                    break;
            }
        } else if (serviceUri.startsWith("/users")) {
            beanName = "users";
        } else {
            beanName = "notFound";
        }

        ApiRequest service = null;
        try {
            /**
             * beanName 값을 사용하여 스프링 컨텍스트에서 API 서비스 클래스 객체를 생성한다.
             * 다른 오류 없이 이 부분
             */
            service = (ApiRequest) springContext.getBean(beanName, requestMap);
        } catch (Exception e) {
            e.printStackTrace();
            // 기본 API 서비스 생성
            service = (ApiRequest) springContext.getBean("notFound", requestMap);
        }

        return service;
    }
}
