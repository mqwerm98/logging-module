package log.munzi.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Slf4j
public class LogExController {

    @GetMapping("/hello")
    @ResponseStatus(HttpStatus.OK)
    public void hello() {

        log.trace("hello trace");
        log.info("hello info");
        log.debug("hello debug");
        log.error("hello error");

    }
}
