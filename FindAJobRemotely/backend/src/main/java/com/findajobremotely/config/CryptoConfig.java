package com.findajobremotely.config;

import com.findajobremotely.core.FieldCrypto;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CryptoConfig {

    /** Fails fast at start-up when the encryption key is missing or malformed. */
    @Bean
    public FieldCrypto fieldCrypto(AppProperties props) {
        return new FieldCrypto(props.security().dataEncryptionKey());
    }
}
