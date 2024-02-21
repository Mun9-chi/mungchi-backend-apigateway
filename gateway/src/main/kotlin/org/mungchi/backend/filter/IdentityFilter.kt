package org.mungchi.backend.filter

import org.mungchi.backend.suppert.JwtProvider
import org.springframework.cloud.gateway.filter.GatewayFilter
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory
import org.springframework.http.HttpHeaders
import org.springframework.http.server.reactive.ServerHttpRequest
import org.springframework.stereotype.Component

private const val BEARER = "Bearer"

@Component
class IdentityFilter(
    private val jwtProvider: JwtProvider
) : AbstractGatewayFilterFactory<IdentityFilter.Config>(Config::class.java) {

    override fun apply(config: Config?): GatewayFilter {
        return GatewayFilter { exchange, chain ->
            val request = exchange.request
            var userID = "GUEST"
            runCatching {
                if (request.headers.containsKey(HttpHeaders.AUTHORIZATION)) {
                    val token = extractBearerToken(request)
                    if (jwtProvider.isValidToken(token)) {
                        userID = jwtProvider.getSubject(token)
                    }
                }
            }.onFailure {
            }.also{
                removeAuthorizationHeader(request)
            }

            addUserIDHeader(request, userID)
            chain.filter(exchange)
        }
    }

    private fun removeAuthorizationHeader(request: ServerHttpRequest) {
        request.mutate().headers { headers ->
            headers.remove(HttpHeaders.AUTHORIZATION)
        }
    }

    private fun extractBearerToken(request: ServerHttpRequest): String {
        val authorization = request.headers.getFirst(HttpHeaders.AUTHORIZATION).orEmpty()
        val (tokenType, token) = splitToTokenFormat(authorization)
        if (tokenType != BEARER) {
            throw IllegalArgumentException()
        }
        return token
    }

    private fun splitToTokenFormat(authorization: String): Pair<String, String> {
        return try {
            val tokenFormat = authorization.split(" ")
            tokenFormat[0] to tokenFormat[1]
        } catch (e: IndexOutOfBoundsException) {
            throw IllegalArgumentException()
        }
    }

    private fun addUserIDHeader(request: ServerHttpRequest, userId: String) {
        request.mutate().headers { headers ->
            headers.set("userID", userId)
        }
    }

    class Config
}