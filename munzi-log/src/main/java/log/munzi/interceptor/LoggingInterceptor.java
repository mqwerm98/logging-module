package log.munzi.interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import log.munzi.interceptor.config.ApiLogProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.util.Enumeration;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Interceptor 단계에서 HttpServletRequest, HttpServletResponse 등을 가로채
 * API의 Request, Response log를 찍어준다.
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class LoggingInterceptor implements HandlerInterceptor {

    private final ObjectMapper objectMapper;

    private final ApiLogProperties apiLog;

    private String requestMethodUri;

    private long startTime;


    /**
     * Request API log를 찍는 부분.
     * 설정파일의 secret 여부, 길이 제한 등을 체크해 설정대로 로그를 남긴다.
     * <p>
     * Interceptor가 Request 중간에서 가로채서 작업하는 부분이기 때문에,
     * preHandle 호출 시 필요한 HttpServletRequest, HttpServletResponse, handler를 인자로 받아 사용하고 preHandle 호출에 그대로 사용한다.
     *
     * @param request  HttpServletRequest
     * @param response HttpServletResponse
     * @param handler  HttpServletResponse
     * @return HandlerInterceptor.super.preHandle
     * @throws Exception request.getReader Exception
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        startTime = System.currentTimeMillis();
        requestMethodUri = request.getMethod() + " " + request.getRequestURI();

        if (apiLog.isUse() && apiLog.getRequest() != null) {
            // inactive api '*' check
            boolean inactiveYn = this.checkEndAsterisk(apiLog.getRequest().getInactiveApi(), requestMethodUri);

            if ((!request.getClass().getName().contains("SecurityContextHolderAwareRequestWrapper") || apiLog.isIgnoreSecurityLog())
                    && !inactiveYn
                    && !apiLog.getRequest().getInactiveApi().contains(requestMethodUri)) {
                StringBuilder headersBuilder = new StringBuilder();
                Enumeration<String> headerNames = request.getHeaderNames();
                String headerName;
                while (headerNames.hasMoreElements()) {
                    headerName = headerNames.nextElement();
                    headersBuilder.append("\"");
                    headersBuilder.append(headerName);
                    headersBuilder.append("\":\"");
                    headersBuilder.append(request.getHeader(headerName).replaceAll("\"", "'"));
                    headersBuilder.append("\", ");
                }
                int headersLength = headersBuilder.length();
                if (headersLength >= 2) headersBuilder.delete(headersLength - 2, headersLength);

                StringBuilder paramsBuilder = new StringBuilder();
                Enumeration<String> paramNames = request.getParameterNames();
                String paramName;
                while (paramNames.hasMoreElements()) {
                    paramName = paramNames.nextElement();
                    paramsBuilder.append("\"");
                    paramsBuilder.append(paramName);
                    paramsBuilder.append("\":\"");
                    paramsBuilder.append(request.getParameter(paramName));
                    paramsBuilder.append("\", ");
                }
                int paramLength = paramsBuilder.length();
                if (paramLength >= 2) paramsBuilder.delete(paramLength - 2, paramLength);

                String body;
                String contentType = request.getHeader("Content-Type");

                if (contentType == null || request.getHeader("Content-Length") == null) {
                    body = "{}";
                } else {
                    int contentLength = Integer.parseInt(request.getHeader("Content-Length"));
                    if (contentType.contains("multipart/form-data")) {
                        body = "[multipart/form-data]";
                    } else if (this.checkEndAsterisk(apiLog.getRequest().getSecretApi(), requestMethodUri) || apiLog.getRequest().getSecretApi().contains(requestMethodUri)) {
                        body = "[secret! " + byteCalculation(contentLength) + "]";
                    } else {
                        if (apiLog.getRequest().getMaxBodySize().isEmpty()) apiLog.getRequest().setMaxBodySize("1KB");
                        if (contentLength > textSizeToByteSize(apiLog.getRequest().getMaxBodySize())) {
                            body = "[" + byteCalculation(contentLength) + "]";
                        } else {
                            body = request.getReader().lines().collect(Collectors.joining(System.lineSeparator()))
                                    .replaceAll("\\s", "")
                                    .replaceAll("\b", "");
                        }
                    }
                }

                String headers = "{" + headersBuilder + "}";
                String params = "{" + paramsBuilder + "}";
                if (apiLog.isJsonPretty()) {
                    headers = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(objectMapper.readValue(headers, Object.class));
                    params = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(objectMapper.readValue(params, Object.class));
                    if (body.startsWith("{") && body.endsWith("}")) {
                        body = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(objectMapper.readValue(body, Object.class));
                    }
                }

                if (this.checkEndAsterisk(apiLog.getDebugApi(), requestMethodUri) || apiLog.getDebugApi().contains(requestMethodUri)) {
                    log.debug("REQ > [{}],\nheaders={},\nparams={},\nbody={}", requestMethodUri, headers, params, body);
                } else {
                    log.info("REQ > [{}],\nheaders={},\nparams={},\nbody={}", requestMethodUri, headers, params, body);
                }
            }
        }

        return HandlerInterceptor.super.preHandle(request, response, handler);
    }

    /**
     * Response API log를 찍는 부분
     * 설정파일의 secret 여부, 길이 제한 등을 체크해 설정대로 로그를 남긴다.
     * <p>
     * Interceptor가 Response 중간에서 가로채서 작업하는 부분이기 때문에,
     * postHandle 호출 시 필요한 HttpServletRequest, HttpServletResponse, handler, ModelAndView를 인자로 받아 사용하고 postHandle 호출에 그대로 사용한다.
     *
     * @param request      HttpServletRequest
     * @param response     HttpServletResponse
     * @param handler      handler
     * @param modelAndView ModelAndView
     * @throws Exception HandlerInterceptor.super.postHandle Exception
     */
    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
        if (apiLog.isUse() && apiLog.getResponse() != null) {
            // inactive api '*' check
            boolean inactiveYn = this.checkEndAsterisk(apiLog.getResponse().getInactiveApi(), requestMethodUri);

            if ((!request.getClass().getName().contains("SecurityContextHolderAwareRequestWrapper") || apiLog.isIgnoreSecurityLog())
                    && !inactiveYn
                    && !apiLog.getResponse().getInactiveApi().contains(requestMethodUri)) {
                final MunziResponseWrapper cachingResponse = (MunziResponseWrapper) response;

                StringBuilder headersBuilder = new StringBuilder();
                Enumeration<String> headerNames = request.getHeaderNames();
                String headerName;
                while (headerNames.hasMoreElements()) {
                    headerName = headerNames.nextElement();
                    headersBuilder.append("\"");
                    headersBuilder.append(headerName);
                    headersBuilder.append("\":\"");
                    headersBuilder.append(request.getHeader(headerName).replaceAll("\"", "'"));
                    headersBuilder.append("\", ");
                }
                int headersLength = headersBuilder.length();
                if (headersLength >= 2) headersBuilder.delete(headersLength - 2, headersLength);

                String payload = "";
                String contentType = cachingResponse.getContentType();
                if (contentType != null) {
                    if (contentType.contains("application/json") && cachingResponse.getContent() != null && !cachingResponse.getContent().isEmpty()) {
                        payload = objectMapper.readTree(cachingResponse.getContent()).toString();
                    } else if (contentType.contains("text/plain")) {
                        payload = cachingResponse.getContent();
                    } else if (contentType.contains("multipart/form-data")) {
                        payload = "[multipart/form-data]";
                    }

                    int payloadSize = payload.getBytes(StandardCharsets.UTF_8).length;
                    String payloadTextSize = byteCalculation(payloadSize);

                    if (this.checkEndAsterisk(apiLog.getResponse().getSecretApi(), requestMethodUri) || apiLog.getResponse().getSecretApi().contains(requestMethodUri)) {
                        payload = "[secret! " + payloadTextSize + "]";
                    } else {
                        if (apiLog.getResponse().getMaxBodySize().isEmpty()) apiLog.getResponse().setMaxBodySize("1KB");
                        if (payloadSize > textSizeToByteSize(apiLog.getResponse().getMaxBodySize())) {
                            payload = "[" + payloadTextSize + "]";
                        }
                    }
                }

                String headers = "{" + headersBuilder + "}";
                if (apiLog.isJsonPretty() && contentType != null && contentType.contains("application/json")) {
                    headers = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(objectMapper.readValue(headers, Object.class));
                    if (payload.startsWith("{") && payload.endsWith("}")) {
                        payload = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(objectMapper.readValue(payload, Object.class));
                    }
                }

                long responseTimeMs = System.currentTimeMillis() - startTime;
                if (this.checkEndAsterisk(apiLog.getDebugApi(), requestMethodUri) || apiLog.getDebugApi().contains(requestMethodUri)) {
                    log.debug("RES > {} [{}] {}ms,\nheaders={},\npayload={}", response.getStatus(), requestMethodUri, responseTimeMs, headers, payload);
                } else {
                    log.info("RES > {} [{}] {}ms,\nheaders={},\npayload={}", response.getStatus(), requestMethodUri, responseTimeMs, headers, payload);
                }
            }
        }

        HandlerInterceptor.super.postHandle(request, response, handler, modelAndView);
    }


    /**
     * bytes 단위의 숫자를 KB, MB 단위의 문자열로 변환
     * ex) 2048 -> 2 KB
     *
     * @param bytes 문자열로 변환할 byte단위 크기
     * @return KB, MB 단위로 변환된 문자열
     */
    private String byteCalculation(int bytes) {
        String[] sArray = {"bytes", "KB", "MB", "GB", "TB", "PB"};

        if (bytes == 0) return "0 bytes";

        int idx = (int) Math.floor(Math.log(bytes) / Math.log(1024));
        DecimalFormat df = new DecimalFormat("#,###.##");
        double ret = ((bytes / Math.pow(1024, Math.floor(idx))));

        return df.format(ret) + " " + sArray[idx];
    }

    /**
     * KB, MB 등의 단위로 표현된 문자열을 byte 로 변환
     *
     * @param size 문자열로 표기된 크기
     * @return byte 단위로 변환된 값
     */
    private double textSizeToByteSize(String size) {
        String[] sArray = {"BYTES", "KB", "MB", "GB", "TB", "PB"};
        size = size.toUpperCase();
        for (int i = 0; i < sArray.length; i++) {
            if (size.contains(sArray[i])) {
                String sizeNumber = size.replaceAll(" ", "").replaceAll(sArray[i], "");
                return Double.parseDouble(sizeNumber) * Math.pow(1024, i);
            }
        }

        return 0;
    }

    private boolean checkEndAsterisk(List<String> apiList, String requestMethodUri) {
        boolean asterisk = false;
        if (apiList != null && apiList.size() > 0) {
            for (String api : apiList) {
                if (api.contains("*")) {
                    String[] split = api.split("\\*");
                    if (!split[0].isEmpty() && requestMethodUri.startsWith(split[0])) {
                        asterisk = true;
                    }
                }
            }
        }
        return asterisk;
    }

}
