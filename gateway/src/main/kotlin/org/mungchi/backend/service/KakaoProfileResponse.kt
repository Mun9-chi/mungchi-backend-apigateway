package org.mungchi.backend.service

import com.fasterxml.jackson.annotation.JsonProperty

data class KakaoProfileResponse(
    val id: Long,
    @JsonProperty("kakao_account") val kakaoAccount: KakaoAccount
) {
    fun toUserInfo(): UserInfo {
        val profile = kakaoAccount.profile
        return UserInfo(
            id,
            SocialType.KAKAO,
            profile.nickname
        )
    }

    data class KakaoAccount(val profile: Profile) {
        data class Profile(val nickname: String)
    }
}
