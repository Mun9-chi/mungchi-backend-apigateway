package org.mungchi.backend.service

class OauthService(private val oauthClients: OauthClients) {

    fun findUserInfo(socialType: SocialType, accessToken: String): UserInfo {
        return oauthClients.findUserInfo(socialType, accessToken)
    }
}
