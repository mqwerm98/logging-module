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
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Component
public class LoggingInterceptor implements HandlerInterceptor {

    private final ObjectMapper objectMapper;

    @Value("${custom.log-filter.use}")
    private boolean logFilterUseYn;

    @Value("${custom.log-filter.response.encrypt.columns}")
    private String resEncryptColumns;

    @Value("${custom.log-filter.response.exclude.columns}")
    private String resExcludeColumns;

    @Value("${custom.log-filter.response.secret.columns}")
    private String resSecretColumns;

    @Value("${custom.log-filter.response.max-body-size}")
    private String resMaxSize;

    private String requestHeaders = null;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!request.getClass().getName().contains("SecurityContextHolderAwareRequestWrapper")) {

            StringBuilder headers = new StringBuilder();
            Enumeration<String> headerNames = request.getHeaderNames();
            String headerName = null;
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
            String paramName = null;
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
            if (body.length() == 0) body = "";
            log.info("REQUEST [{} {}], headers=[{}], params=[{}], body=\"{}\"", request.getMethod(), request.getRequestURI(), headers, params, body);
        }

        return HandlerInterceptor.super.preHandle(request, response, handler);
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
        final ContentCachingResponseWrapper cachingResponse = (ContentCachingResponseWrapper) response;
//        if (logFilterUseYn) {
//            log.debug("encryptColumns : {}", resEncryptColumns);
//            log.debug("excludeColumns : {}", resExcludeColumns);
//            log.debug("secretColumns : {}", resSecretColumns);
//        }
//
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

            int payloadSize = payload.getBytes(StandardCharsets.UTF_8).length;
            if (resMaxSize.isEmpty()) resMaxSize = "1KB";
            if (payloadSize > textSizeToByteSize(resMaxSize)) {
                payload = "[" + byteCalculation(payloadSize) + "]";
            }

        }
        log.info("RESPONSE [{} {}], headers=[{}], payload=\"{}\"", request.getMethod(), request.getRequestURI(), requestHeaders, payload);



        HandlerInterceptor.super.postHandle(request, response, handler, modelAndView);
    }


    private String byteCalculation(int bytes) {
        String[] sArray = {"bytes", "KB", "MB", "GB", "TB", "PB"};

        if (bytes == 0) return "0 bytes";

        int idx = (int) Math.floor(Math.log(bytes) / Math.log(1024));
        DecimalFormat df = new DecimalFormat("#,###.##");
        double ret = ((bytes / Math.pow(1024, Math.floor(idx))));

        return df.format(ret) + " " + sArray[idx];
    }

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
