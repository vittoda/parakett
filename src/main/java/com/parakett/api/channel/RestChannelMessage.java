package com.parakett.api.channel;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

public class RestChannelMessage {
    @Getter
    @Setter
    @NotNull(message = "Message is required") @NotBlank(message="Message is required")
    private String message;
}
