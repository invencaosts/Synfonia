package com.joaopaulo.musicas.configs;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestClient;




@Configuration
public class RestClientConfig {

    @Bean
    public RestClient restClient() { // Removi o parâmetro daqui
        // 1. Criamos o conversor para aceitar o formato da Apple
        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
        java.util.List<MediaType> mediaTypes = new java.util.ArrayList<>();
        mediaTypes.add(MediaType.APPLICATION_JSON);
        mediaTypes.add(new MediaType("text", "javascript", java.util.Objects.requireNonNull(java.nio.charset.StandardCharsets.UTF_8)));
        converter.setSupportedMediaTypes(mediaTypes);



        // 2. Criamos o Builder manualmente e buildamos o RestClient
        return RestClient.builder()
                .messageConverters(converters -> converters.add(0, converter))
                .defaultHeader("User-Agent", "Synfonia/1.0 (Spring RestClient)")
                .build();
    }
}