package log.munzi.interceptor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.util.ContentCachingResponseWrapper;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

/**
 * Request Servlet에 담긴 내용을 열어서 Request, Response 로그를 남겨야 하지만
 * Request Servlet은 휘발성이기 때문에, 해당 내용을 response body에 담도록 설정하는 Filter 역할.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class GlobalRequestWrappingFilter implements Filter {

    @Value("${custom.log-filter.request.secret.api}")
    private List<String> reqSecretApiList;

    @Value("${custom.log-filter.request.max-body-size}")
    private String reqMaxSize;

    @Override
    public void init(FilterConfig filterConfig) {

    }

    @Override
    public void destroy() {

    }

    /**
     * Request Servlet 에 담긴 내용을 열어보면 휘발되기 때문에, 로그로 남기기 위해 response body 에 담는 과정
     * @param request ServletRequest
     * @param response ServletResponse
     * @param chain Filter chain
     * @throws IOException copyBodyToResponse 과정에서의 Exception
     * @throws ServletException doFilter 과정에서의 Exception
     */
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest wrappingRequest = new ReadableRequestWrapper((HttpServletRequest) request, reqSecretApiList, reqMaxSize);
        ContentCachingResponseWrapper wrappingResponse = new ContentCachingResponseWrapper((HttpServletResponse) response);

        chain.doFilter(wrappingRequest, wrappingResponse);

        wrappingResponse.copyBodyToResponse(); // 캐시를 copy해 return될 response body에 저장
    }

}
