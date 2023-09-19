package log.munzi.interceptor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.json.JSONException;
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

        JSONObject jsonObject;
        try {
            jsonObject = new JSONObject(returnValue);
        } catch (JSONException e) {
            return;
        }

        if (jsonObject.isEmpty()) {
            return;
        }

        Integer httpStatus = null;
        if (jsonObject.has("httpStatus")) {
            httpStatus = jsonObject.getInt("httpStatus");
        } else if (jsonObject.has("status")) {
            httpStatus = jsonObject.getInt("status");
        } else if (jsonObject.has("statusCodeValue")) {
            httpStatus = jsonObject.getInt("statusCodeValue");
        }

        String errorCode = "";
        if (jsonObject.has("errorCode")) {
            errorCode = jsonObject.getString("errorCode");
        } else if (jsonObject.has("code")) {
            errorCode = jsonObject.getString("code");
            JSONObject properties = jsonObject.getJSONObject("properties");
            if (properties.has("errorCode")) {
                errorCode = properties.getString("errorCode");
            }
        } else if (jsonObject.has("body")) {
            JSONObject body = jsonObject.getJSONObject("body");
            if (body.has("code")) {
                errorCode = body.getString("code");
            } else if (body.has("errorCode")) {
                errorCode = body.getString("errorCode");
            }
        }

        String message = "";
        if (jsonObject.has("message")) {
            message = jsonObject.getString("message");
        } else if (jsonObject.has("detail")) {
            message = jsonObject.getString("detail");
        } else if (jsonObject.has("body")) {
            JSONObject body = jsonObject.getJSONObject("body");
            if (body.has("message")) {
                message = body.getString("message");
            } else if (body.has("errorMessage")) {
                message = body.getString("errorMessage");
            }
        }

        String errorType = exception.getClass().getName();
        String stackTrace = exception.getMessage();

        log.error("ERR > httpStatus={}, errorCode=\"{}\", errorType=\"{}\", message=\"{}\",\nstackTrace=\"{}\"", httpStatus, errorCode, errorType, message, stackTrace);
    }

}

