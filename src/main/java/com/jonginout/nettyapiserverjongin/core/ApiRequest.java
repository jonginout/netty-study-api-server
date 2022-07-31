package com.jonginout.nettyapiserverjongin.core;

import com.google.gson.JsonObject;
import com.jonginout.nettyapiserverjongin.service.RequestParamException;
import com.jonginout.nettyapiserverjongin.service.ServiceException;

/**
 * API의 업무를 처리하는 클래스
 * 네티를 사용하여 HTTP 프로토콜의 처리가 완료되었다면
 * HTTP 프로토콜로 수신된 데이터를 사용하여 API 서비스 클래스를 호출해야한다.
 */
public interface ApiRequest {
    /**
     * API를 호출하는 HTTP 요청의 파라미터 값이 입렫되었는지 검증하는 메서드
     */
    public void requestParamValidation() throws RequestParamException;

    /**
     * 각 API 서비스에 따른 개별 구현 메서드
     */
    public void service() throws ServiceException;

    /**
     * 서비스 API 호출 시작 메서드
     */
    public void executeService();

    /**
     * API 서비스의 처리 결과를 조회하는 메서드
     */
    public JsonObject getApiResult();
}
