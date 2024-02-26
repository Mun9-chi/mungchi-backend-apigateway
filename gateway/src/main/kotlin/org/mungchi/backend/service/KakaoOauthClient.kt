package org.mungchi.backend.service

import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono

private const val PROFILE_URL = "https://kapi.kakao.com/v2/user/me"

@Component
class KakaoOauthClient(
    private val webClient: WebClient = WebClient.create(PROFILE_URL)
) : OauthClient {

    override fun findUserInfo(accessToken: String): UserInfo {
        return webClient.get()
            .header("Authorization", "Bearer $accessToken")
            .retrieve()
            .onStatus({ it.is4xxClientError }) { Mono.error(IllegalArgumentException()) }
            .onStatus({ it.is5xxServerError }) { Mono.error(IllegalArgumentException()) }
            .bodyToMono(KakaoProfileResponse::class.java)
            .block()?.toUserInfo() ?: throw IllegalArgumentException()
    }

    override fun getSocialType(): SocialType {
        return SocialType.KAKAO
    }
}
