package com.back.minecraftback.payment.tbank;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TbankInitResponse {

    @JsonProperty("Success")
    private boolean success;

    @JsonProperty("ErrorCode")
    private String errorCode;

    @JsonProperty("Message")
    private String message;

    @JsonProperty("Details")
    private String details;

    @JsonProperty("TerminalKey")
    private String terminalKey;

    @JsonProperty("Status")
    private String status;

    @JsonProperty("PaymentId")
    private Long paymentId;

    @JsonProperty("OrderId")
    private String orderId;

    @JsonProperty("PaymentURL")
    private String paymentUrl;
}
