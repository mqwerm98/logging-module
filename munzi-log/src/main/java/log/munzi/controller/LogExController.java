package log.munzi.controller;

import log.munzi.dto.ReqHello;
import log.munzi.dto.ResHello;
import log.munzi.service.MunziService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;


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

    @PostMapping(value = "", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public ResHello helloPost(@RequestParam @Validated String name,
                              @RequestParam(value = "file") MultipartFile file) {

        log.debug("name : {}, fileSize : {}", name, file.getSize());

        munziService.createMunzi(name);
        return new ResHello(name);
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

    @PostMapping("/no-secret")
    @ResponseStatus(HttpStatus.OK)
    public ResHello helloPostNoSecret(@RequestBody @Validated ReqHello request) {

        return new ResHello(request.getName());
    }
}
