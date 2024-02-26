package org.mungchi.backend.filter

import com.fasterxml.jackson.databind.ObjectMapper
import org.mungchi.backend.suppert.JwtProvider
import org.reactivestreams.Publisher
import org.springframework.cloud.gateway.filter.GatewayFilter
import org.springframework.cloud.gateway.filter.NettyWriteResponseFilter
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory
import org.springframework.core.Ordered
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.http.ResponseCookie
import org.springframework.http.server.reactive.ServerHttpResponseDecorator
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.nio.charset.StandardCharsets
import java.time.Duration

@Component
class OauthFilter2(
    private val jwtProvider: JwtProvider
) : AbstractGatewayFilterFactory<OauthFilter2.Config>(Config::class.java), Ordered {
    private val objectMapper = ObjectMapper()

    override fun apply(config: Config?): GatewayFilter {
        return GatewayFilter { exchange, chain ->
            chain.filter(exchange).then(Mono.fromRunnable {
                val response = exchange.response

                // 수정된 Response Body를 처리하는 데코레이터 생성
                val decoratedResponse = object : ServerHttpResponseDecorator(response) {
                    override fun writeWith(body: Publisher<out DataBuffer>): Mono<Void> {
                        if (body is Flux) {
                            return super.writeWith(body.flatMap { dataBuffer ->
                                val content = ByteArray(dataBuffer.readableByteCount())
                                dataBuffer.read(content)
                                val responseBody = String(content, StandardCharsets.UTF_8)

                                // 여기에서 response body의 값을 찾아서 로직을 수행하고 body를 수정
                                val userId = findUserIDFromResponseBody(responseBody)
                                val createToken = jwtProvider.createToken(userId)

                                // ResponseCookie 객체로 변경
                                val responseCookie = ResponseCookie.from("accessToken", createToken)
                                    .maxAge(Duration.ofHours(12))
                                    .build()
                                response.addCookie(responseCookie)

                                // 최종적으로 body를 비우기
                                Mono.just(dataBuffer)
                            })
                        }
                        return super.writeWith(body)
                    }
                }

                // chain.filter를 사용하여 불필요한 subscribe를 제거
                exchange.mutate().response(decoratedResponse).build()
            })
        }
    }

    override fun getOrder(): Int {
        return NettyWriteResponseFilter.WRITE_RESPONSE_FILTER_ORDER - 1;
    }

    private fun findUserIDFromResponseBody(responseBody: String): String {
        return try {
            val jsonNode = objectMapper.readTree(responseBody)
            jsonNode["userID"]?.asText() ?: throw IllegalArgumentException("UserID not found")
        } catch (e: Exception) {
            throw RuntimeException("Failed to extract UserID from response body", e)
        }
    }

    class Config
}
