package log.munzi.interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.util.ContentCachingResponseWrapper;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.util.ArrayList;
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

    @Value("${custom.log-filter.use}")
    private boolean logFilterUseYn;

    @Value("${custom.ignore-security-log}")
    private boolean ignoreSecurityLog = false;

    @Value("${custom.log-filter.request.secret.api}")
    private List<String> reqSecretApiList;

    @Value("${custom.log-filter.response.secret.api}")
    private List<String> resSecretApiList;

    @Value("${custom.log-filter.request.inactive.api}")
    private List<String> reqInactiveApiList;

    @Value("${custom.log-filter.response.inactive.api}")
    private List<String> resInactiveApiList;

    @Value("${custom.log-filter.request.max-body-size}")
    private String reqMaxSize;

    @Value("${custom.log-filter.response.max-body-size}")
    private String resMaxSize;

    private String requestMethodUri;


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
        requestMethodUri = request.getMethod() + " " + request.getRequestURI();

        // inactive api '*' check
        boolean inactiveYn = false;
        if (reqInactiveApiList.size() > 0) {
            for (String api : reqInactiveApiList) {
                if (api.contains("*")) {
                    String[] split = api.split("\\*");
                    if (!split[0].isEmpty() && requestMethodUri.startsWith(split[0])) {
                        inactiveYn = true;
                    }
                }
            }
        }


        if ((!request.getClass().getName().contains("SecurityContextHolderAwareRequestWrapper") || ignoreSecurityLog) && !inactiveYn && !reqInactiveApiList.contains(requestMethodUri)) {
            StringBuilder headers = new StringBuilder();
            Enumeration<String> headerNames = request.getHeaderNames();
            String headerName;
            while (headerNames.hasMoreElements()) {
                headerName = headerNames.nextElement();
                headers.append("\"");
                headers.append(headerName);
                headers.append("\":\"");
                headers.append(request.getHeader(headerName));
                headers.append("\", ");
            }
            int headersLength = headers.length();
            if (headersLength >= 2) headers.delete(headersLength - 2, headersLength);

            StringBuilder params = new StringBuilder();
            Enumeration<String> paramNames = request.getParameterNames();
            String paramName;
            while (paramNames.hasMoreElements()) {
                paramName = paramNames.nextElement();
                params.append("\"");
                params.append(paramName);
                params.append("\":\"");
                params.append(request.getParameter(paramName));
                params.append("\", ");
            }
            int paramLength = params.length();
            if (paramLength >= 2) params.delete(paramLength - 2, paramLength);

            String body = null;
            String contentType = request.getHeader("Content-Type");

            if (contentType == null || request.getHeader("Content-Length") == null) {
                body = "";
            } else {
                int contentLength = Integer.parseInt(request.getHeader("Content-Length"));
                if (contentType.contains("multipart/form-data")) {
                    body = "[multipart/form-data]";
                } else if (reqSecretApiList.contains(requestMethodUri)) {
                    body = "[secret! " + byteCalculation(contentLength) + "]";
                } else {
                    if (reqMaxSize.isEmpty()) reqMaxSize = "1KB";
                    if (contentLength > textSizeToByteSize(reqMaxSize)) {
                        body = "[" + byteCalculation(contentLength) + "]";
                    } else {
                        body = request.getReader().lines().collect(Collectors.joining(System.lineSeparator())).replaceAll("\\s", "");
                    }
                }
            }

            log.info("REQUEST [{}], headers={{}}, params={{}}, body={}", requestMethodUri, headers, params, body);
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
        // inactive api '*' check
        boolean inactiveYn = false;
        if (resInactiveApiList.size() > 0) {
            for (String api : resInactiveApiList) {
                if (api.contains("*")) {
                    String[] split = api.split("\\*");
                    if (!split[0].isEmpty() && requestMethodUri.startsWith(split[0])) {
                        inactiveYn = true;
                    }
                }
            }
        }

        if ((!request.getClass().getName().contains("SecurityContextHolderAwareRequestWrapper") || ignoreSecurityLog) && !inactiveYn && !resInactiveApiList.contains(requestMethodUri)) {
            final ContentCachingResponseWrapper cachingResponse = (ContentCachingResponseWrapper) response;

            StringBuilder headers = new StringBuilder();
            Enumeration<String> headerNames = request.getHeaderNames();
            String headerName;
            while (headerNames.hasMoreElements()) {
                headerName = headerNames.nextElement();
                headers.append("\"");
                headers.append(headerName);
                headers.append("\":\"");
                headers.append(request.getHeader(headerName));
                headers.append("\", ");
            }
            int headersLength = headers.length();
            if (headersLength >= 2) headers.delete(headersLength - 2, headersLength);

            String payload = "";
            String contentType = cachingResponse.getContentType();
            if (contentType != null) {
                if (contentType.contains("application/json") && cachingResponse.getContentAsByteArray().length != 0) {
                    payload = objectMapper.readTree(cachingResponse.getContentAsByteArray()).toString();
                } else if (contentType.contains("text/plain")) {
                    payload = new String(cachingResponse.getContentAsByteArray());
                } else if (contentType.contains("multipart/form-data")) {
                    payload = "[multipart/form-data]";
                }

                if (logFilterUseYn) {
                    int payloadSize = payload.getBytes(StandardCharsets.UTF_8).length;
                    String payloadTextSize = byteCalculation(payloadSize);

                    if (resSecretApiList.contains(requestMethodUri)) {
                        payload = "[secret! " + payloadTextSize + "]";
                    } else {
                        if (resMaxSize.isEmpty()) resMaxSize = "1KB";
                        if (payloadSize > textSizeToByteSize(resMaxSize)) {
                            payload = "[" + payloadTextSize + "]";
                        }
                    }
                }
            }

            log.info("RESPONSE [{}], headers={{}}, payload={}", requestMethodUri, headers, payload);
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

}
