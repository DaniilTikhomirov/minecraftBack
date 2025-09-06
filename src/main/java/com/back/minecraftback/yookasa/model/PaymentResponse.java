package com.back.minecraftback.yookasa.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PaymentResponse {
    @NotNull
    private String id;
    @NotNull
    private String status;
    @NotNull
    private Amount amount;

    @JsonProperty("income_amount")
    private Amount incomeAmount;
    private String description;
    private String captured_at;

    @JsonProperty("created_at")
    @NotNull
    private String createdAt;
    @NotNull
    private Boolean test;
    @NotNull
    private Boolean paid;
    @NotNull
    private Boolean refundable;
    private Map<String, String> metadata;
}
