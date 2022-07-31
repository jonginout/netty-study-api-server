package com.jonginout.nettyapiserverjongin.core;

import com.google.gson.JsonObject;
import com.jonginout.nettyapiserverjongin.service.RequestParamException;
import com.jonginout.nettyapiserverjongin.service.ServiceException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;

public abstract class ApiRequestTemplate implements ApiRequest {
    protected Logger logger;

    /**
     * API 요청 data
     */
    protected Map<String, String> reqData;

    /**
     * API 처리결과
     */
    protected JsonObject apiResult;

    /**
     * HTTP 요청에서 추출[한] 필드의 이름과 값을 API 서비스 클래스의 생성자로 전달한다.
     */
    public ApiRequestTemplate(Map<String, String> reqData) {
        this.logger = LogManager.getLogger(this.getClass());
        this.apiResult = new JsonObject();
        this.reqData = reqData;

        logger.info("request data : " + this.reqData);
    }

    public void executeService() {
        try {
            // 정합성 검사
            this.requestParamValidation();

            // service 메서드는 각 API 서비스 클래스가 제공할 기능을 구현해야한다
            this.service();
        } catch (RequestParamException e) {
            logger.error(e);
            this.apiResult.addProperty("resultCode", "405");
        } catch (ServiceException e) {
            logger.error(e);
            this.apiResult.addProperty("resultCode", "501");
        }
    }

    public JsonObject getApiResult() {
        return this.apiResult;
    }

    @Override
    public void requestParamValidation() throws RequestParamException {
        if (getClass().getClasses().length == 0) {
            return;
        }
    }
}
