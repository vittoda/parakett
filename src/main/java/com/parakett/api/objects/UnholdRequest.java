package com.parakett.api.objects;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

public class UnholdRequest {


    @Getter
    @Setter
    @NotNull(message = "response is required")
    private String response;

    @Getter
    @Setter
    @NotNull(message = "status is required")
    private String status;
    
}
