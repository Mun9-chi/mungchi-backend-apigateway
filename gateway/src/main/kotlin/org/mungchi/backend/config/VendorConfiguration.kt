package org.mungchi.backend.config

import org.mungchi.backend.service.OauthClient
import org.mungchi.backend.service.OauthClients
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration


@Configuration
class VendorConfiguration {

    @Bean
    fun oauthClients(clients: Set<OauthClient>): OauthClients {
        return OauthClients(clients)
    }
}
