package log.munzi.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * API Log 설정
 */
@Data
@Component
@ConfigurationProperties(prefix = "api-log")
public class ApiLogProperties {

    // required(*)
    // server명 (구분자 '-')
    // "server명-profile-timestamp"로 로그 수집됐을 때 index명으로 사용
    private String serverName;

    // true일 경우에만 security여도 로그 찍음
    private boolean ignoreSecurityLog = false;

    // filter 사용 여부
    private boolean use = false;

    // request, response 로그 log level 설정
//    private LogLevel defaultLevel = LogLevel.INFO;

    // request log에 대한 설정
    private LogRequestResponse request;

    // response log에 대한 설정
    private LogRequestResponse response;

    // defaultLevel이 아닌 debug로 찍을 api 설정
    private List<String> debugApi = new ArrayList<>();

    private boolean jsonPretty = false;

    // requestId를 직접 정해줄 경우, header에서 사용할 key값
    private String requestIdHeaderKey;

    // 500 에러가 났을 경우, ErrorAspect에서 stackTrace가 포함된 에러를 추가로 찍을 지에 대한 여부
    private boolean stackTracePrintYn = false;

    /**
     * API Log 설정 Request, Response DTO
     */
    @Data
    public static class LogRequestResponse {

        // max body size. body size가 이를 넘어갈 경우 body 내용 대신 크기만 찍음
        private String maxBodySize;

        // 해당 api의 body 내용 대신 크기만 찍음
        private List<String> secretApi = new ArrayList<>();

        // 해당 api의 내용을 안찍음
        private List<String> inactiveApi = new ArrayList<>();

    }
}
