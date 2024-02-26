package org.mungchi.backend.filter

import com.fasterxml.jackson.databind.ObjectMapper
import org.mungchi.backend.suppert.JwtProvider
import org.reactivestreams.Publisher
import org.springframework.cloud.gateway.filter.GatewayFilter
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory
import org.springframework.core.Ordered
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.core.io.buffer.DataBufferUtils
import org.springframework.core.io.buffer.DefaultDataBufferFactory
import org.springframework.http.ResponseCookie
import org.springframework.http.server.reactive.ServerHttpRequestDecorator
import org.springframework.http.server.reactive.ServerHttpResponseDecorator
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.nio.charset.StandardCharsets
import java.time.Duration

@Component
class OauthFilter(
//    private val oauthService: OauthService,
    private val jwtProvider: JwtProvider
) : AbstractGatewayFilterFactory<OauthFilter.Config>(Config::class.java) ,Ordered {
    private val objectMapper = ObjectMapper()


    override fun apply(config: Config?): GatewayFilter {
        return GatewayFilter { exchange, chain ->
            val request = exchange.request
            val response = exchange.response
            val bodyFlux: Flux<DataBuffer> = request.body
            val bodyMono: Mono<String> = DataBufferUtils.join(bodyFlux)
                .flatMap { dataBuffer ->
                    val byteArray = ByteArray(dataBuffer.readableByteCount())
                    dataBuffer.read(byteArray)
                    DataBufferUtils.release(dataBuffer)
                    Mono.just(String(byteArray, Charsets.UTF_8))
                }

            bodyMono.flatMap { body ->
                val jsonNode = objectMapper.readTree(body)
                val socialType = jsonNode["socialType"].asText() ?: throw IllegalArgumentException()
                val accessToken = jsonNode["accessToken"].asText() ?: throw IllegalArgumentException()
//                val userInfo = oauthService.findUserInfo(SocialType.from(socialType), accessToken)

//                val modifiedBody = objectMapper.writeValueAsString(userInfo.toMap())
                val modifiedBody = objectMapper.writeValueAsString(
                    mapOf(
                        "socialId" to 1,
                        "socialType" to "kako",
                        "nickname" to "123"
                    )
                )

                val modifiedRequest = object : ServerHttpRequestDecorator(request) {
                    override fun getBody(): Flux<DataBuffer> {
                        val bufferFactory = DefaultDataBufferFactory()
                        val dataBuffer = bufferFactory.wrap(modifiedBody.toByteArray(StandardCharsets.UTF_8))
                        return Flux.just(dataBuffer)
                    }
                }


                chain.filter(exchange.mutate().request(modifiedRequest).build())
                    .then(Mono.fromRunnable {
                        // 수정된 Response Body를 처리하는 데코레이터 생성
                        val decoratedResponse = object : ServerHttpResponseDecorator(response) {
                            override fun writeWith(body: Publisher<out DataBuffer>): Mono<Void> {
                                println("aaa        aaaaaaaaaaaaaaa")
                                if (body is Flux) {
                                    val fluxBody = body
                                    return super.writeWith(fluxBody.map { dataBuffer ->
                                        val content = ByteArray(dataBuffer.readableByteCount())
                                        dataBuffer.read(content)

                                        // 예: content를 문자열로 변환하여 로깅
                                        val responseBody = String(content, StandardCharsets.UTF_8)

                                        println(responseBody+"        aaaaaaaaaaaaaaa")
                                        // 여기에서 response body의 값을 찾아서 로직을 수행하고 body를 수정
                                        val userId = findUserIDFromResponseBody(responseBody)
                                        val createToken = jwtProvider.createToken(userId)

                                        // ResponseCookie 객체로 변경
                                        val responseCookie = ResponseCookie.from("accessToken", createToken)
                                            .maxAge(Duration.ofHours(12))
                                            .build()
                                        response.addCookie(responseCookie)
                                        println("asdasdad")

                                        // 최종적으로 body를 비우기
                                        DefaultDataBufferFactory().wrap(ByteArray(0))
                                    })
                                }
                                return super.writeWith(body)
                            }
                        }


//                         chain.filter를 사용하여 불필요한 subscribe를 제거
                        exchange.mutate().response(decoratedResponse).build()
                    })
            }
        }
    }


    override fun getOrder(): Int {
        return -2 // -1 is response write filter, must be called before that
    }

    private fun findUserIDFromResponseBody(responseBody: String): String {
        val jsonNode = objectMapper.readTree(responseBody)
        val userId = jsonNode["userID"] ?: throw IllegalArgumentException()
        return userId.asText()
    }

    class Config
}
