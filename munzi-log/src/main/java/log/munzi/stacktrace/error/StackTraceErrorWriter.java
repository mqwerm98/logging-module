package log.munzi.stacktrace.error;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * StackTrace를 포함한 error를 임의로 찍어주는 역할을 하는 writer
 * <p>
 * ErrorAspect에서 찍는 에러 로그에 exception이 없어서, 추가로 exception이 포함된 에러로그를 찍는 부분
 * log type : ERR_STACK_TRACE
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StackTraceErrorWriter {

    /**
     * StackTrace를 포함한 error 로그 작성
     *
     * @param httpStatus http status
     * @param errorCode  에러 코드
     * @param errorType  에러 타입 (Exception type)
     * @param message    에러 메세지
     * @param exception  Exception
     */
    public void writeStackTraceError(Integer httpStatus, String errorCode, String errorType, String message, Exception exception) {
        log.error("ERR_STACK_TRACE > httpStatus={}, errorCode=\"{}\", errorType=\"{}\", message=\"{}\"", httpStatus, errorCode, errorType, message, exception);
    }

}
