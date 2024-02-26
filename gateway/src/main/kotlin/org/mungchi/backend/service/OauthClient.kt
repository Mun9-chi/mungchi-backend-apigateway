package org.mungchi.backend.service

interface OauthClient {
    fun findUserInfo(accessToken :String) :UserInfo

    fun  getSocialType(): SocialType
}
