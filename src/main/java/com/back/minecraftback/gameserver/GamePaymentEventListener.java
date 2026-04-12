package com.back.minecraftback.gameserver;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class GamePaymentEventListener {

    private final GameServerPaymentNotifyService gameServerPaymentNotifyService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPaymentPaid(PaymentPaidGameEvent event) {
        gameServerPaymentNotifyService.notifyPaymentPaid(event);
    }
}
