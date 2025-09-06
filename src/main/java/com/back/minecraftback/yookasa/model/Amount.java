package com.back.minecraftback.yookasa.model;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Amount {
    @NotNull
    private String value;
    @NotNull
    private String currency;
}
