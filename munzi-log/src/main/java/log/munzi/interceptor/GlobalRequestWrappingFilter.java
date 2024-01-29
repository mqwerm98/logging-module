package log.munzi.interceptor;

import io.micrometer.common.util.StringUtils;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import log.munzi.config.ApiLogProperties;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Request Servlet에 담긴 내용을 열어서 Request, Response 로그를 남겨야 하지만
 * Request Servlet은 휘발성이기 때문에, 해당 내용을 response body에 담도록 설정하는 Filter 역할.
 */
@Component
@Order(Ordered.LOWEST_PRECEDENCE)
@RequiredArgsConstructor
public class GlobalRequestWrappingFilter implements Filter {

    private final ApiLogProperties apiLog;

    private final String profile;

    @Override
    public void init(FilterConfig filterConfig) {

    }

    @Override
    public void destroy() {

    }

    /**
     * Request Servlet 에 담긴 내용을 열어보면 휘발되기 때문에, 로그로 남기기 위해 response body 에 담는 과정
     *
     * @param request  ServletRequest
     * @param response ServletResponse
     * @param chain    Filter chain
     * @throws IOException      copyBodyToResponse 과정에서의 Exception
     * @throws ServletException doFilter 과정에서의 Exception
     */
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        List<String> secretApiList = new ArrayList<>();
        String maxSize = "";
        if (apiLog.getRequest() != null) {
            secretApiList = apiLog.getRequest().getSecretApi();
            maxSize = apiLog.getRequest().getMaxBodySize();
        }

        // request wrapping
        HttpServletRequest wrappingRequest = new ReadableRequestWrapper((HttpServletRequest) request, secretApiList, maxSize);

        // MDC 등록
        String requestId = StringUtils.isNotBlank(apiLog.getRequestIdHeaderKey()) && wrappingRequest.getHeader(apiLog.getRequestIdHeaderKey()) != null ?
                wrappingRequest.getHeader(apiLog.getRequestIdHeaderKey()) : UUID.randomUUID().toString();
        MDC.put("requestId", requestId);
        String applicationName = (!StringUtils.isBlank(apiLog.getServerName()) ? apiLog.getServerName() + "-" : "") + profile + " " + InetAddress.getLocalHost().getHostAddress();
        MDC.put("applicationName", applicationName);

        // response wrapping & doFilter
        // accept가 "text/event-stream" 인 경우, response flush 해버리면 안되기 때문에 response wrapping 하지 않음
        if (Objects.equals(wrappingRequest.getHeader("accept"), MediaType.TEXT_EVENT_STREAM_VALUE)) {
            chain.doFilter(wrappingRequest, response);
        } else {
            ContentCachingResponseWrapper wrappingResponse = new ContentCachingResponseWrapper((HttpServletResponse) response);
            chain.doFilter(wrappingRequest, wrappingResponse);
            wrappingResponse.copyBodyToResponse();
        }

        // MDC 등록 해제
        MDC.remove("requestId");
        MDC.remove("applicationName");
    }

}
