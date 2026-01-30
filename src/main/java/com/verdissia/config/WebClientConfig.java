package com.verdissia.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import javax.net.ssl.SSLException;
import javax.net.ssl.X509TrustManager;
import java.security.cert.X509Certificate;

import static io.netty.handler.ssl.SslContextBuilder.forClient;
import static io.netty.handler.ssl.util.InsecureTrustManagerFactory.INSTANCE;

@Configuration
public class WebClientConfig {

    @Bean
    public WebClient.Builder webClientBuilder() throws SSLException {
        var sslContext = forClient()
                .trustManager(INSTANCE)
                .build();

        var httpClient = HttpClient.create()
                .secure(sslProviderSpec -> sslProviderSpec.sslContext(sslContext));

        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient));
    }

    @Bean
    public WebClient webClient(WebClient.Builder webClientBuilder) {
        return webClientBuilder.build();
    }
}
