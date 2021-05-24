package log.munzi.interceptor;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

//@Component
public class CustomServletWrappingFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
//        ContentCachingRequestWrapper wrappingRequest = new ContentCachingRequestWrapper(request);
//        ContentCachingResponseWrapper wrappingResponse = new ContentCachingResponseWrapper(response);
//        filterChain.doFilter(wrappingRequest, wrappingResponse);
//        wrappingResponse.copyBodyToResponse(); //body값을 copy해 캐시로 저장
    }
}
