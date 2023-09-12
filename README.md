# Munzi Log - API Server를 위한 Request, Response Logging 처리 모듈

## 적용 예시
```bash
# Request Log
[INFO ] 2023/09/12 15:19:00.956 [127.0.0.1 772f27b1-d5d9-4453-8bbb-daa8f54df0cd] http-nio-10108-exec-1 [l.m.i.LoggingInterceptor.preHandle:127] REQ > [POST /hello],
headers={"Accept":"*/*", "User-Agent":"PostmanRuntime/7.26.10", "Connection":"keep-alive", "Postman-Token":"88ed20f8-cb8a-4a17-b4b0-6acb813abd39", "Host":"localhost:10009", "Accept-Encoding":"gzip, deflate, br", "Content-Length":"46", "Content-Type":"application/json"}, 
params={}, 
body={"name":"승리를","name2":"tt"}

# Response Log
[INFO ] 2023/09/12 15:22:06.643 [127.0.0.1 6b7da681-6fee-4de8-9e83-6c42d790ba9a] http-nio-10108-exec-4 [l.m.i.LoggingInterceptor.postHandle:209] RES > 201 [POST /hello] 6ms,
headers={"Accept":"*/*", "User-Agent":"PostmanRuntime/7.26.10", "Connection":"keep-alive", "Postman-Token":"88ed20f8-cb8a-4a17-b4b0-6acb813abd39", "Host":"localhost:10009", "Accept-Encoding":"gzip, deflate, br", "Content-Length":"46", "Content-Type":"application/json"}, 
payload={"name":"승리를"}

# Checked Exception Error Log
[ERROR] 2023/09/12 15:20:47.440 [127.0.0.1 1f125f13-ba47-42e7-90d3-7cb388a4dfcf] http-nio-10108-exec-3 [l.m.i.ErrorAspect.recordErrorLog:71] ERR > httpStatus=400, errorCode="003", errorType="org.springframework.web.bind.MethodArgumentNotValidException", message="[issuedDate] 널이어서는 안됩니다",
stackTrace="Validation failed for argument [1] in public java.lang.String ..."

# Unchecked Exception Error Log
[ERROR] 2023/09/12 15:29:38.290 [127.0.0.1 faaa0aaa-2914-4202-8ce3-329f3cf7ddae] http-nio-10108-exec-4 [l.m.i.ErrorAspect.recordErrorLog:71] ERR > httpStatus=500, errorCode="", errorType="java.lang.NullPointerException", message="Cannot invoke \"net.test.api.module.dto.request.ReqDto.getNumber()\" because \"reqDto\" is null",
stackTrace="Cannot invoke ..."
```

# 로깅 모듈 적용

### 0. 의존성 추가 (build.gradle)

```groovy
ext {
	version_log4j= '2.17.0' 
}

configurations {
    compileOnly {
        extendsFrom annotationProcessor
    }

    all {
        // log4j2를 사용하기 위해, spring의 default인 logback을 제외
        exclude group: 'org.springframework.boot', module: 'spring-boot-starter-logging'
        resolutionStrategy.cacheChangingModulesFor 0, 'seconds'
    }
}

repositories {
	mavenCentral()
}

dependencies {
	implementation group:'log.munzi', name:'munzi-log', version:'0.1.3'

	implementation group: 'org.apache.logging.log4j', name: 'log4j-api', version: "${version_log4j}"
	implementation group: 'org.apache.logging.log4j', name: 'log4j-core', version: "${version_log4j}"
	implementation group: 'org.apache.logging.log4j', name: 'log4j-jul', version: "${version_log4j}"
	implementation group: 'org.apache.logging.log4j', name: 'log4j-slf4j-impl', version: "${version_log4j}"
	implementation group: 'org.apache.logging.log4j', name: 'log4j-web', version: "${version_log4j}"
	implementation group: 'org.springframework.boot', name: 'spring-boot-starter-log4j2'
	implementation group: 'com.fasterxml.jackson.dataformat', name: 'jackson-dataformat-yaml', version: "${version_jackson}"
	implementation group: 'org.bgee.log4jdbc-log4j2', name: 'log4jdbc-log4j2-jdbc4.1', version: '1.16'
	implementation group: 'org.json', name: 'json', version: '20230618'
}
```

---

### 1. ApiLogProperties bean 등록

```java
@EnableConfigurationProperties(ApiLogProperties.class) // 이부분 추가!
public class ApiApplication {
	public static void main(String[] args) {
			...
	}
}
```

### 2. Filter, Interceptor bean 등록

<LoggingConfig.java>

```java
import com.fasterxml.jackson.databind.ObjectMapper;
import log.munzi.interceptor.GlobalRequestWrappingFilter;
import log.munzi.interceptor.LoggingInterceptor;
import log.munzi.interceptor.config.ApiLogProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import log.munzi.interceptor.ErrorAspect;

@Configuration
@RequiredArgsConstructor
public class LoggingConfig {

    public final ApiLogProperties apiLogProperties;

    @Bean
    public LoggingInterceptor loggingInterceptor() {
        return new LoggingInterceptor(new ObjectMapper(), apiLogProperties);
    }

    @Bean // security 사용시에만 등록
    public GlobalRequestWrappingFilter globalRequestWrappingFilter() {
        return new GlobalRequestWrappingFilter(apiLogProperties);
    }

    @Bean // error log 찍을때에만 등록
    public ErrorAspect errorAspect() {
        return new ErrorAspect();
    }
}
```

### 3. bean 등록한 Filter 적용

GlobalRequestWrappingFilter는 기본적으로 Filter 우선순위 제일 아래로 등록이 된다.
하지만 security 설정이 들어가면 Filter Chain에 제대로 들어가지 않는 오류(?)가 있어서
SecurityConfig 설정을 해줄 때, addFilter를 해줘야 한다.

<SecurityConfig.java (Security Configuration Class)>

```java
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
	    ...
	
	    http.addFilterBefore(munziLoginFilter(), UsernamePasswordAuthenticationFilter.class);
	    http.addFilterBefore(globalRequestWrappingFilter, UsernamePasswordAuthenticationFilter.class);
	
	    return http.build();
	}
}
```
다음과 같이 addFilter를 해준다.
꼭 UserNamePasswordAuthenticationFiler 하위에 해줄 필요는 없다.
SpringSecurityFilter들보다만 후순위로 들어가게 설정해주면 된다.


### 4. bean 등록한 Interceptor 적용

<WebMvcConfig.java>

```java
import log.munzi.interceptor.LoggingInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final LoggingInterceptor loggingInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(loggingInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns("/vendor/**", "/css/*", "/img/*");

    }
}
```

### 5. interceptor 사용을 위한 Filter 처리

<GlobalRequestWrappingFilter.java>

```java
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import log.munzi.interceptor.config.ApiLogProperties;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
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

        HttpServletRequest wrappingRequest = new ReadableRequestWrapper((HttpServletRequest) request, secretApiList, maxSize);
        ContentCachingResponseWrapper wrappingResponse = new ContentCachingResponseWrapper((HttpServletResponse) response);

        MDC.put("requestId", UUID.randomUUID().toString());
        MDC.put("applicationName", InetAddress.getLocalHost().getHostAddress());

        chain.doFilter(wrappingRequest, wrappingResponse);

        wrappingResponse.copyBodyToResponse(); // 캐시를 copy해 return될 response body에 저장

        MDC.remove("requestId");
        MDC.remove("applicationName");
    }

}
```

## 설정파일

---

<application.yml>

```yaml
spring:
  output:
    ansi:
      enabled: ALWAYS # 로그 알록달록 예쁘게 나오게 설정
  datasource: # log4jdbc-log4j2 사용시 필요한 설정
    driver-class-name: net.sf.log4jdbc.sql.jdbcapi.DriverSpy # 고정
    url: jdbc:log4jdbc:mariadb~~~ # 기존 jdbc:mariadb~~ 이런식으로 썼던 부분 사이에 log4jdbc 추가

logging:
  config: file:/apps/dkargo/munzi-log/config/log4j2-local.yml #log4j2.yml 파일의 경로
  # config: classpath:log4j2-local.yml # resources 하위의 경우

api-log:
  ignore-security-log: true # default = false, true일 경우에만 security여도 로그 찍음
  use: true # request, response 로그를 찍는지 여부
  json-pretty: true # request, response 로그 내 json 데이터를 정렬해서 보여줄지 여부
  debug-api: GET /api/debug/* # info가 아닌, debug로 찍고 싶은 api 목록
  request:
    max-body-size: 1 MB # request body max size
    secret-api: # 해당 api의 경우, body 전체를 로그에 안찍음
    inactive-api: GET /api/webjars/*, GET /api/, GET /api/swagger*, GET /api/code/*, OPTIONS /api/code/*
  response:
    max-body-size: 10 KB # response body max size
    secret-api:
    inactive-api: GET /api/webjars/*, GET /api/, GET /api/swagger*, GET /api/code/*, OPTIONS /api/code/*

```

<aside>
💡 기존 logging 설정은 모두 주석처리 하거나 지워주세요
log관련 설정은 log4j2.yml파일에 몰아 넣을겁니다

</aside>

<log4j2.yml>

```yaml
Configuration:
  name: log4j2 local
  status: INFO
  monitorInterval: 5 # 1

  Properties: # 2
    Property:
      - name: package-name
        value: "log.munzi"
      - name: log-path
        value: "/apps/logs/munzi-log"
      - name: log-filename
        value: "munzi-log.log"
      - name: log-db-filename
        value: "munzi-db-log.log"
      - name: log-pattern
        value: "%highlight{[%-5p]}{FATAL=bg_red, ERROR=red, INFO=green, DEBUG=blue} %style{%d{yyyy/MM/dd HH:mm:ss.SSS}}{cyan} %style{[%X{applicationName} %X{requestId}]}{magenta} %style{%t}{yellow} %style{[%C{1.}.%M:%L]}{blue} %m%n"
      - name: log-pattern-no-color
        value: "[%-5p] %d{yyyy/MM/dd HH:mm:ss.SSS} [%X{applicationName} %X{requestId}] %t [%C{1.}.%M:%L] %m%n"

  Appenders: # 3
    Console:
      name: Console_Appender
      target: SYSTEM_OUT
      PatternLayout:
        pattern: ${log-pattern}
    RollingFile:
     - name: RollingFile_Appender
        fileName: ${log-path}/${log-filename}
        filePattern: ${log-path}/archive/${log-filename}.%d{yyyy-MM-dd-hh-mm}.gz
        PatternLayout:
          pattern: ${log-pattern-no-color}
        Policies:
          SizeBasedTriggeringPolicy:
            size: 500 MB
        DefaultRollOverStrategy:
          max: 30
          Delete:
            basePath: ${log-path}/archive
            maxDepth: 1
            IfAccumulatedFileCount:
              exceeds: 31
      - name: Debug_RollingFile_Appender
        fileName: ${log-path}/${debug-log-filename}
        filePattern: ${log-path}/archive/${debug-log-filename}.%d{yyyy-MM-dd-hh-mm}.gz
        PatternLayout:
          pattern: ${log-pattern-no-color}
        LevelRangeFilter:
          minLevel: FATAL
          maxLevel: DEBUG
          onMatch: ACCEPT
          onMismatch: DENY
        Policies:
          SizeBasedTriggeringPolicy:
            size: 500 MB
        DefaultRollOverStrategy:
          max: 30
          Delete:
            basePath: ${log-path}/archive
            maxDepth: 1
            IfAccumulatedFileCount:
              exceeds: 31
      - name: Info_RollingFile_Appender
        fileName: ${log-path}/${log-filename}
        filePattern: ${log-path}/archive/${log-filename}.%d{yyyy-MM-dd-hh-mm}.gz
        PatternLayout:
          pattern: ${log-pattern-no-color}
        LevelRangeFilter:
          minLevel: FATAL
          maxLevel: INFO
          onMatch: ACCEPT
          onMismatch: DENY
        Policies:
          SizeBasedTriggeringPolicy:
            size: 500 MB
        DefaultRollOverStrategy:
          max: 30
          Delete:
            basePath: ${log-path}/archive
            maxDepth: 1
            IfAccumulatedFileCount:
              exceeds: 31
      - name: RollingFile_Appender_Color
        fileName: ${log-path}/${log-filename}
        filePattern: ${log-path}/archive/${log-filename}.%d{yyyy-MM-dd-hh-mm}.gz
        PatternLayout:
          pattern: ${log-pattern}
        Policies:
          SizeBasedTriggeringPolicy:
            size: 500 MB
        DefaultRollOverStrategy:
          max: 30
          Delete:
            basePath: ${log-path}/archive
            maxDepth: 1
            IfAccumulatedFileCount:
              exceeds: 31
      - name: RollingDBFile_Appender
        fileName: ${log-path}/${log-db-filename}
        filePattern: ${log-path}/archive/${log-db-filename}.%d{yyyy-MM-dd-hh-mm}.gz
        PatternLayout:
          pattern: ${log-pattern}
        Policies:
          SizeBasedTriggeringPolicy:
            size: 500 MB
        DefaultRollOverStrategy:
          max: 30
          Delete:
            basePath: ${log-path}/archive
            maxDepth: 1
            IfAccumulatedFileCount:
              exceeds: 31

  Loggers: # 4
    Root: # 5
      includeLocation: TRUE
      level: INFO
      AppenderRef:
        - ref: Console_Appender
    AsyncLogger: # 6
      # package
      - name: ${package-name}
        includeLocation: TRUE
        additivity: FALSE
        level: DEBUG
        AppenderRef:
          - ref: Console_Appender
          - ref: RollingFile_Appender

      - name: org.springframework.web
        includeLocation: TRUE
        additivity: FALSE
        level: ERROR
        AppenderRef:
          - ref: Console_Appender
          - ref: RollingFile_Appender

      - name: org.hibernate
        includeLocation: TRUE
        additivity: FALSE
        level: ERROR
        AppenderRef:
          - ref: RollingDBFile_Appender
#      - name: org.hibernate.SQL
#        includeLocation: TRUE
#        additivity: FALSE
#        level: DEBUG
#        AppenderRef:
#          - ref: RollingDBFile_Appender

      - name: log4jdbc.log4j2 # 7
        includeLocation: TRUE
        additivity: FALSE
        level: DEBUG
        AppenderRef:
          - ref: Console_Appender
          - ref: RollingFile_Appender
          - ref: RollingDBFile_Appender
        MarkerFilter:
#          - marker: LOG4JDBC_JDBC # jdbc
#            onMatch: DENY
#            onMismatch: NEUTRAL
#          - marker: LOG4JDBC_CONNECTION # jdbc.connection
#            onMatch: DENY
#            onMismatch: DENY
#          - marker: LOG4JDBC_NON_STATEMENT # jdbc.sqltiming
#            onMatch: ACCEPT
#            onMismatch: DENY
#          - marker: LOG4JDBC_SQL
#            onMatch: DENY
#            onMismatch: DENY
#          - marker: LOG4JDBC_AUDIT # jdbc.audit
#            onMatch: DENY
#            onMismatch: DENY
#          - marker: LOG4JDBC_RESULTSET # jdbc.resultset
#            onMatch: DENY
#            onMismatch: DENY
          - marker: LOG4JDBC_RESULTSETTABLE # jdbc.resultsettable
            onMatch: DENY
            onMismatch: DENY

# 88888888
      - name: log.munzi.interceptor
        includeLocation: TRUE
        additivity: FALSE
        level: DEBUG
        AppenderRef:
          - ref: Console_Appender
          - ref: Info_RollingFile_Appender
          - ref: Debug_RollingFile_Appender
```

<aside>
💡 profile 설정을 위해 log4j2-local.yml 등으로 만들어서 사용하시면 됩니다.

</aside>

1. monitorInterval : 서버를 재시작 하지 않아도, 적용된 시간 단위로(초 단위) 해당 파일의 수정 사항을 반영해 주는 설정
2. 파일 내에서 사용할 변수 지정
3. Appender 설정 : 로그를 어떤 방식으로 찍을 것인지 설정
    
    ConsoleAppender, FileAppender, RollingFileAppender 등 존재
    
    Console : console에 System.out으로 찍음
    
    File : 파일에 찍음
    
    RollingFile : 파일에 찍고, 특정 기준에 따라 압축
    
    자세한 내용은 아래 링크 참고!
    [Log4j 2 제대로 사용하기 - 개념](https://velog.io/@bread_dd/Log4j-2-%EC%A0%9C%EB%8C%80%EB%A1%9C-%EC%82%AC%EC%9A%A9%ED%95%98%EA%B8%B0-%EA%B0%9C%EB%85%90)

4. Logger : 어디에서 찍을지 설정할 부분

5. Root : 모든 로그

	→ INFO 레벨 이상의 모든 로그를 Console_Appender를 이용해 찍겠다고 설정한 부분

6. 패키지 단위 설정

	동기 방식의 Logger가 있고, 비동기 방식의 AsyncLogger가 있는데

	비동기 방식을 사용할 경우, includeLocation: true를 설정해 줘야 호출한 경로를 찾아올 수 있으므로 붙여주자.

	name : package 경로

	additivity : 중복 제거 설정

7.  log4jdbc-log4j2를 사용하는 경우, log4jdbc.log4j2에서 모든 로그를 찍고 있기 때문에
    
    기존에 application.yml에서 logging.jdbc.connection: ERROR 식으로 썼던 부분을 상세하게 설정하고 싶은 경우엔 MarkerFilter를 사용해서 설정해 주면 된다.
    
    위 소스는 jdbc.resultsettable이면 찍고, 그 외에는 모두 찍지 않겠다고 설정해 놓은 것이다.
    
8. req, res, err 로그를 log.munzi.interceptor에서 찍기 때문에 다음을 추가해 주어야 한다.

---

**< DB를 사용하지 않는 경우! >**

```groovy
<build.gradle 파일>

configurations {
...
    all {
        ...
        exclude group: 'org.springframework.boot', module: 'spring-boot-starter-logging'
        exclude group: 'org.bgee.log4jdbc-log4j2', module: 'log4jdbc-log4j2-jdbc4.1'
        exclude group: 'org.springframework.boot', module: 'spring-boot-starter-data-jpa'
        resolutionStrategy.cacheChangingModulesFor 0, 'seconds'
    }
}
```
