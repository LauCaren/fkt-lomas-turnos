package com.fktlomas.turnos.config;

import com.mercadopago.client.payment.PaymentClient;
import com.mercadopago.client.preference.PreferenceClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Beans en vez de "new" directo en MercadoPagoService, para poder mockearlos en los tests del servicio. */
@Configuration
public class MercadoPagoClientConfig {

    @Bean
    public PreferenceClient preferenceClient() {
        return new PreferenceClient();
    }

    @Bean
    public PaymentClient paymentClient() {
        return new PaymentClient();
    }
}
