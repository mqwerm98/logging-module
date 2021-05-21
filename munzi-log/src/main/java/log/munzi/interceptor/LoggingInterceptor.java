package log.munzi.interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Slf4j
@RequiredArgsConstructor
@Component
public class LoggingInterceptor implements HandlerInterceptor {

    private final ObjectMapper objectMapper;

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        if (request.getClass().getName().contains("SecurityContextHolderAwareRequestWrapper")) return;
        final ContentCachingRequestWrapper cachingRequest = (ContentCachingRequestWrapper) request;
        final ContentCachingResponseWrapper cachingResponse = (ContentCachingResponseWrapper) response;

        if (cachingRequest.getContentType() != null && cachingRequest.getContentType().contains("application/json")) {
            cachingRequest.getContentAsByteArray();
            if (cachingRequest.getContentAsByteArray().length != 0) {
                log.info("REQ BODY) {}", objectMapper.readTree(cachingRequest.getContentAsByteArray()));
            }
        }
        if (cachingResponse.getContentType() != null && cachingResponse.getContentType().contains("application/json")) {
            cachingResponse.getContentAsByteArray();
            if (cachingResponse.getContentAsByteArray().length != 0) {
                log.info("RES BODY) {}", objectMapper.readTree(cachingResponse.getContentAsByteArray()));
            }
        }

    }
}
