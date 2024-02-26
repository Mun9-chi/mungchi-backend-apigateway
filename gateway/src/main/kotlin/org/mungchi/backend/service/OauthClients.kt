package org.mungchi.backend.service

import java.util.*

class OauthClients(clients: Set<OauthClient>) {
    private val mappingClients: Map<SocialType, OauthClient>

    init {
        val mapping = EnumMap<SocialType, OauthClient>(SocialType::class.java)
        clients.forEach { client -> mapping[client.getSocialType()] = client }
        this.mappingClients = mapping
    }

    fun findUserInfo(socialType: SocialType, accessToken: String): UserInfo {
        val client: OauthClient = getClient(socialType)
        return client.findUserInfo(accessToken)
    }

    private fun getClient(socialType: SocialType): OauthClient = mappingClients.get(socialType)
        ?: throw IllegalArgumentException("해당 OAuth2 제공자는 지원되지 않습니다.")
}
