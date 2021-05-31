package log.munzi.controller;

import log.munzi.dto.ReqHello;
import log.munzi.dto.ResHello;
import log.munzi.service.MunziService;
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

    private final MunziService munziService;

    @GetMapping("")
    @ResponseStatus(HttpStatus.OK)
    public String hello(@RequestParam String name) {

        log.trace("trace hello {}", name);
        log.info("info hello {}", name);
        log.debug("debug hello {}", name);
        log.warn("warn hello {}", name);
        log.error("error hello {}", name);

        return munziService.getNames();
    }

    @PostMapping("")
    @ResponseStatus(HttpStatus.OK)
    public ResHello helloPost(@RequestBody @Validated ReqHello request) {

        log.debug("name : {}", request.getName());

        munziService.createMunzi(request.getName());
        return new ResHello(request.getName());
    }

    @GetMapping("/secret")
    @ResponseStatus(HttpStatus.OK)
    public String helloSecret() {
        return munziService.getNames();
    }

    @PostMapping("/secret")
    @ResponseStatus(HttpStatus.OK)
    public ResHello helloPostSecret(@RequestBody @Validated ReqHello request) {

        log.debug("name : {}", request.getName());

        munziService.createMunzi(request.getName());
        return new ResHello(request.getName());
    }
}
