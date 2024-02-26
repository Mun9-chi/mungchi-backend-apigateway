package org.mungchi.backend.service

enum class SocialType {
    KAKAO;

    companion object {
        fun from(socialType: String): SocialType {
            return when {
                KAKAO.name.equals(socialType, ignoreCase = true) ->KAKAO
                //TODO: 커스텀 에러로 변경
                else -> throw IllegalArgumentException()
            }
        }
    }
}
