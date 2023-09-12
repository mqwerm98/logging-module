package log.munzi.interceptor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.json.JSONObject;
import org.springframework.stereotype.Component;

/**
 * Error Log를 일정 포맷에 맞게 찍어주는 Error Aspect
 * example format : ERR > httpStatus=400, errorCode="E001", errorType="org.springframework.web.bind.MethodArgumentNotValidException", message="널이어서는 안됩니다",\nstackTrace="Validation failed for argument..."
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class ErrorAspect {

    /**
     * exception handler pointcut
     */
    @Pointcut("@annotation(org.springframework.web.bind.annotation.ExceptionHandler)")
    public void exceptionHandler() {
    }

    /**
     * record error log
     * exceptionHandler return 후 error log를 찍어준다.
     * error log format : ERR > httpStatus=${httpStatus}, errorCode="${errorCode}", errorType="${errorType}", message="${message}",\nstackTrace="${stackTrace}"
     *
     * @param joinPoint   Exception
     * @param returnValue error response (error DTO)
     */
    @AfterReturning(pointcut = "exceptionHandler()", returning = "returnValue")
    public void recordErrorLog(JoinPoint joinPoint, Object returnValue) {
        Exception exception = (Exception) joinPoint.getArgs()[0];

        JSONObject jsonObject = new JSONObject(returnValue);
        Integer httpStatus = null;
        if (jsonObject.has("httpStatus")) {
            httpStatus = jsonObject.getInt("httpStatus");
        } else if (jsonObject.has("status")) {
            httpStatus = jsonObject.getInt("status");
        }

        String errorCode = "";
        if (jsonObject.has("errorCode")) {
            errorCode = jsonObject.getString("errorCode");
        } else if (jsonObject.has("code")) {
            errorCode = jsonObject.getString("code");
        } else if (jsonObject.has("properties")) {
            JSONObject tmp = jsonObject.getJSONObject("properties");
            if (tmp.has("errorCode")) {
                errorCode = tmp.getString("errorCode");
            }
        }

        String message = "";
        if (jsonObject.has("message")) {
            message = jsonObject.getString("message");
        } else if (jsonObject.has("detail")) {
            message = jsonObject.getString("detail");
        }

        String errorType = exception.getClass().getName();
        String stackTrace = exception.getMessage();

        log.error("ERR > httpStatus={}, errorCode=\"{}\", errorType=\"{}\", message=\"{}\",\nstackTrace=\"{}\"", httpStatus, errorCode, errorType, message, stackTrace);
    }

}