package log.munzi.controller;

import log.munzi.dto.ReqHello;
import log.munzi.dto.ResHello;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/hello")
public class LogExController {

    @GetMapping("")
    @ResponseStatus(HttpStatus.OK)
    public String hello(@RequestParam String name) {

        log.trace("trace hello {}", name);
        log.info("info hello {}", name);
        log.debug("debug hello {}", name);
        log.error("error hello {}", name);

        return "hello " + name;
    }

    @PostMapping("")
    @ResponseStatus(HttpStatus.OK)
    public ResHello helloPost(@RequestBody @Validated ReqHello request) {

        log.info("name1 : {}", request.getName());
        log.info("name2 : {}", request.getName());
        log.info("name3 : {}", request.getName());

        return new ResHello(request.getName());
    }
}
