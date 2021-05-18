package log.munzi.filter;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.CommonsRequestLoggingFilter;

@Configuration
public class ReqLoggingFilterConfig {

    @Bean
    public CommonsRequestLoggingFilter logFilter() {
        CommonsRequestLoggingFilter filter = new CommonsRequestLoggingFilter();
        filter.setIncludeClientInfo(true); // client 주소와 session id를 로그에 포함
        filter.setIncludeHeaders(true); // header 정보를 로그에 포함
        filter.setIncludePayload(true); // request 내용을 로그에 포함
        filter.setIncludeQueryString(true); // query 문자열을 로그에 포함
        filter.setMaxPayloadLength(1000); // 로그의 최대 길이 설정
        return filter;
    }
}
