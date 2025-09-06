package com.back.minecraftback.yookasa.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Recipient {
    @JsonProperty("account_id")
    private String accountIid;
    @JsonProperty("gateway_id")
    private String gatewayId;
}
