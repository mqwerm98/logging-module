package log.munzi.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class ReqHello {

    @NotBlank
    private String name;

}
