package com.bebis.BeBiS.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

import static org.springframework.security.config.Customizer.withDefaults;

@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/items/**").permitAll()
                        .requestMatchers("/api/profile/**").authenticated()
                        .anyRequest().permitAll()
                )
                .oauth2Login(withDefaults());

        return http.build();
    }

}
