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
import java.util.Enumeration;
import java.util.List;
import java.util.stream.Collectors;

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

    @Value("${custom.log-filter.response.max-body-size}")
    private String resMaxSize;

    private String requestHeaders;
    private String requestMethodUri;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!request.getClass().getName().contains("SecurityContextHolderAwareRequestWrapper") || ignoreSecurityLog) {
            requestMethodUri = request.getMethod() + " " + request.getRequestURI();
            StringBuilder headers = new StringBuilder();
            Enumeration<String> headerNames = request.getHeaderNames();
            String headerName;
            while (headerNames.hasMoreElements()) {
                headerName = headerNames.nextElement();
                headers.append(headerName);
                headers.append(":\"");
                headers.append(request.getHeader(headerName));
                headers.append("\", ");
            }
            int headersLength = headers.length();
            if (headersLength >= 2) headers.delete(headersLength - 2, headersLength);
            requestHeaders = headers.toString();

            StringBuilder params = new StringBuilder();
            Enumeration<String> paramNames = request.getParameterNames();
            String paramName;
            while (paramNames.hasMoreElements()) {
                paramName = paramNames.nextElement();
                params.append(paramName);
                params.append(":\"");
                params.append(request.getParameter(paramName));
                params.append("\", ");
            }
            int paramLength = params.length();
            if (paramLength >= 2) params.delete(paramLength - 2, paramLength);

            String body = request.getReader().lines().collect(Collectors.joining(System.lineSeparator())).replaceAll("\\s", "");
            if (reqSecretApiList.contains(requestMethodUri)) {
                body = "[secret! " + byteCalculation(body.getBytes(StandardCharsets.UTF_8).length) + "]";
            } else if (body.length() == 0) {
                body = "";
            }

            log.info("REQUEST [{}], headers=[{}], params=[{}], body=\"{}\"", requestMethodUri, headers, params, body);
        }

        return HandlerInterceptor.super.preHandle(request, response, handler);
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
        if (!request.getClass().getName().contains("SecurityContextHolderAwareRequestWrapper") || ignoreSecurityLog) {
            final ContentCachingResponseWrapper cachingResponse = (ContentCachingResponseWrapper) response;

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

            log.info("RESPONSE [{}], headers=[{}], payload=\"{}\"", requestMethodUri, requestHeaders, payload);
        }

        HandlerInterceptor.super.postHandle(request, response, handler, modelAndView);
    }


    /**
     * bytes 단위의 숫자를 KB, MB 단위의 문자열로 변환
     * ex) 2048 -> 2 KB
     * @param bytes
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
     * @param size
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
