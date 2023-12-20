package log.munzi.interceptor;

import io.micrometer.common.util.StringUtils;
import jakarta.servlet.http.HttpServletRequest;
import log.munzi.interceptor.config.ApiLogProperties;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.MDC;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * request, error 로그를 찍어주는 Util
 */
@Slf4j
public class LoggingUtil {

    private final LoggingInterceptor loggingInterceptor;

    private final ApiLogProperties apiLog;

    private final String profile;

    /**
     * loggingInterceptor의 preHandle 기능을 그대로 사용하기 위함으로
     * LoggingInterceptor와 interceptor에 오기 전 GlobalRequestWrappingFilter에서 실행하는
     * wrapping 및 MDC 설정을 해주기 위해 Filter에서 받는 값을 그대로 가져온다.
     *
     * @param loggingInterceptor loggingInterception
     * @param apiLog             apiLogProperties
     * @param profile            profile
     */
    public LoggingUtil(LoggingInterceptor loggingInterceptor, ApiLogProperties apiLog, String profile) {
        this.loggingInterceptor = loggingInterceptor;
        this.apiLog = apiLog;
        this.profile = profile;
    }


    /**
     * Request log를 찍는다
     *
     * @param request   request log 찍을 httpServletRequest
     * @param requestId request log에 찍을 requestId
     * @throws Exception request.getReader Exception
     */
    public void recordRequestLog(HttpServletRequest request, String requestId) throws Exception {
        List<String> secretApiList = new ArrayList<>();
        String maxSize = "";
        if (apiLog.getRequest() != null) {
            secretApiList = apiLog.getRequest().getSecretApi();
            maxSize = apiLog.getRequest().getMaxBodySize();
        }

        // MDC 등록
        MDC.put("requestId", requestId);
        String applicationName = (!StringUtils.isBlank(apiLog.getServerName()) ? apiLog.getServerName() + "-" : "") + profile + " " + InetAddress.getLocalHost().getHostAddress();
        MDC.put("applicationName", applicationName);


        // request wrapping
        HttpServletRequest wrappingRequest = new ReadableRequestWrapper(request, secretApiList, maxSize);

        // log를 찍는 부분
        loggingInterceptor.preHandle(wrappingRequest, null, null);

        MDC.remove("requestId");
        MDC.remove("applicationName");
    }

    /**
     * Request log를 찍는다
     *
     * @param request           request log 찍을 httpServletRequest
     * @param createRequestIdYn requestId 생성 여부
     * @return request log 찍는 데 사용된 requestId
     * @throws Exception request.getReader Exception
     */
    public String recordRequestLog(HttpServletRequest request, boolean createRequestIdYn) throws Exception {
        List<String> secretApiList = new ArrayList<>();
        String maxSize = "";
        if (apiLog.getRequest() != null) {
            secretApiList = apiLog.getRequest().getSecretApi();
            maxSize = apiLog.getRequest().getMaxBodySize();
        }

        // request wrapping
        HttpServletRequest wrappingRequest = new ReadableRequestWrapper(request, secretApiList, maxSize);

        String requestId;
        if (createRequestIdYn) {
            requestId = StringUtils.isNotBlank(apiLog.getRequestIdHeaderKey()) && wrappingRequest.getHeader(apiLog.getRequestIdHeaderKey()) != null ?
                    wrappingRequest.getHeader(apiLog.getRequestIdHeaderKey()) : UUID.randomUUID().toString();
            MDC.put("requestId", requestId);
            MDC.put("applicationName", (!StringUtils.isBlank(apiLog.getServerName()) ? apiLog.getServerName() + "-" : "") + profile + " " + InetAddress.getLocalHost().getHostAddress());
        } else {
            requestId = MDC.get("requestId");
        }

        // log를 찍는 부분
        loggingInterceptor.preHandle(wrappingRequest, null, null);

        if (createRequestIdYn) {
            // MDC 등록 해제
            MDC.remove("requestId");
            MDC.remove("applicationName");
        }

        return requestId;
    }

    /**
     * Error log를 찍는다
     *
     * @param exception   Exception
     * @param returnValue error 났을 떄 return 값
     * @param requestId   error log에 찍을 requestId
     * @throws UnknownHostException ip 조회 오류
     * @throws JSONException        returnValue JSON parsing error
     */
    public void recordErrorLog(Exception exception, Object returnValue, String requestId) throws UnknownHostException, JSONException {
        JSONObject jsonObject = new JSONObject(returnValue);
        if (jsonObject.isEmpty()) {
            return;
        }

        // MDC 등록
        MDC.put("requestId", requestId);
        String applicationName = (!StringUtils.isBlank(apiLog.getServerName()) ? apiLog.getServerName() + "-" : "") + profile + " " + InetAddress.getLocalHost().getHostAddress();
        MDC.put("applicationName", applicationName);

        this.recordErrorLog(jsonObject, exception);

        // MDC 등록 해제
        MDC.remove("requestId");
        MDC.remove("applicationName");
    }

    /**
     * Error log를 찍는다
     *
     * @param exception         Exception
     * @param returnValue       error 났을 떄 return 값
     * @param createRequestIdYn requestId 생성 여부
     * @return error log 찍는 데 사용된 requestId
     * @throws UnknownHostException     ip 조회 오류
     * @throws JSONException            returnValue JSON parsing error
     * @throws IllegalArgumentException createRequestYn이 false인데 requestId가 MDC에 등록 안돼있는 경우
     */
    public String recordErrorLog(Exception exception, Object returnValue, boolean createRequestIdYn) throws UnknownHostException, JSONException, IllegalArgumentException {
        JSONObject jsonObject = new JSONObject(returnValue);
        if (jsonObject.isEmpty()) {
            return null;
        }

        String requestId;
        if (createRequestIdYn) {
            requestId = UUID.randomUUID().toString();
            MDC.put("requestId", requestId);
            MDC.put("applicationName", (!StringUtils.isBlank(apiLog.getServerName()) ? apiLog.getServerName() + "-" : "") + profile + " " + InetAddress.getLocalHost().getHostAddress());
        } else {
            requestId = MDC.get("requestId");
        }

        this.recordErrorLog(jsonObject, exception);

        if (createRequestIdYn) {
            // MDC 등록 해제
            MDC.remove("requestId");
            MDC.remove("applicationName");
        }

        return requestId;
    }

    /**
     * Error log 찍는부분
     *
     * @param jsonObject returnValue JsonObject
     * @param exception  error exception
     */
    private void recordErrorLog(JSONObject jsonObject, Exception exception) {
        Integer httpStatus = null;
        if (jsonObject.has("httpStatus")) {
            httpStatus = jsonObject.getInt("httpStatus");
        } else if (jsonObject.has("status")) {
            httpStatus = jsonObject.getInt("status");
        } else if (jsonObject.has("statusCodeValue")) {
            httpStatus = jsonObject.getInt("statusCodeValue");
        }

        String errorCode = "";
        if (jsonObject.has("errorCode")) {
            errorCode = jsonObject.getString("errorCode");
        } else if (jsonObject.has("code")) {
            errorCode = jsonObject.getString("code");
        } else if (jsonObject.has("properties")) {
            JSONObject properties = jsonObject.getJSONObject("properties");
            if (properties.has("errorCode")) {
                errorCode = properties.getString("errorCode");
            }
        } else if (jsonObject.has("body")) {
            JSONObject body = jsonObject.getJSONObject("body");
            if (body.has("code")) {
                errorCode = body.getString("code");
            } else if (body.has("errorCode")) {
                errorCode = body.getString("errorCode");
            }
        }

        String message = "";
        if (jsonObject.has("message")) {
            message = jsonObject.getString("message");
        } else if (jsonObject.has("detail")) {
            message = jsonObject.getString("detail");
        } else if (jsonObject.has("body")) {
            JSONObject body = jsonObject.getJSONObject("body");
            if (body.has("message")) {
                message = body.getString("message");
            } else if (body.has("errorMessage")) {
                message = body.getString("errorMessage");
            }
        }

        String errorType = exception.getClass().getName();
        String stackTrace;
        if (errorType.equals("org.springframework.web.bind.MethodArgumentNotValidException")) {
            MethodArgumentNotValidException e = (MethodArgumentNotValidException) exception;
            stackTrace = String.format("[%s] %s", Objects.requireNonNull(e.getBindingResult().getFieldError()).getField(),
                    e.getBindingResult().getAllErrors().get(0).getDefaultMessage());
        } else {
            stackTrace = exception.getMessage();
        }

        log.error("ERR > httpStatus={}, errorCode=\"{}\", errorType=\"{}\", message=\"{}\",\nstackTrace=\"{}\"", httpStatus, errorCode, errorType, message, stackTrace);
    }

}
