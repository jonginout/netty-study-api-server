package com.jonginout.nettyapiserverjongin.service;

import com.jonginout.nettyapiserverjongin.core.ApiRequestTemplate;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

@Service("users")
@Scope("prototype")
public class UserInfo extends ApiRequestTemplate {

    public UserInfo(Map<String, String> reqData) {
        super(reqData);
    }

    @Override
    public void requestParamValidation() throws RequestParamException {
        if (StringUtils.isEmpty(this.reqData.get("email"))) {
            throw new RequestParamException("email이 없습니다.");
        }
    }

    @Override
    public void service() throws ServiceException {
        // 입력 email 사용자의 이메일을 HTTP heder에 입력한다.
        // 출력 resultCode API 처리 결과코드를 돌려준다. API 처리 결과가 정상이면 결과코드는 200이다.
        // 출력 message API 처리 결과 메시지를 돌려준다. API의 처리결과가 정상일 때는 Success 메시지를 돌려주며
        // 나머지 정상이 아닐 때는 오류 메시지를 돌려준다.
        // 출력 userNo 입력된 이메일에 해당하는 사용자의 사용자 번호를 돌려준다.
        Map<String, Object> result = new HashMap<>();
        result.put("USERNO", 12312);

        if (result != null) {
            String userNo = String.valueOf(result.get("USERNO"));

            // helper.
            this.apiResult.addProperty("resultCode", "200");
            this.apiResult.addProperty("message", "Success");
            this.apiResult.addProperty("userNo", userNo);
        } else {
            // 데이터 없음.
            this.apiResult.addProperty("resultCode", "404");
        }
    }
}
