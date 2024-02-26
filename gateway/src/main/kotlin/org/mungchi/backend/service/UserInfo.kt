package org.mungchi.backend.service

data class UserInfo(
    val socialId: Long,
    val socialType: SocialType,
    val nickname: String
)
