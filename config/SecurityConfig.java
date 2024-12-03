package com.hoit.checkers.config;

import com.hoit.checkers.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    private CustomLogoutSuccessHandler customLogoutSuccessHandler; // CustomLogoutSuccessHandler 주입
    
    
    @Autowired
    public void configureGlobal(AuthenticationManagerBuilder auth) throws Exception {
        auth.userDetailsService(userService).passwordEncoder(passwordEncoder);
    }

    @Autowired
    public SecurityConfig(UserService userService, PasswordEncoder passwordEncoder) {
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
    }

    // 비밀번호 암호화 Bean
    

    // AuthenticationManager Bean 설정
    @Bean
    public AuthenticationManager authenticationManager(HttpSecurity http) throws Exception {
        return http.getSharedObject(AuthenticationManagerBuilder.class)
                   .userDetailsService(userService)
                   .passwordEncoder(passwordEncoder) // 수정: 주입된 passwordEncoder 사용
                   .and()
                   .build();
    }

    // SecurityFilterChain Bean 설정
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // CSRF 설정: /api/rooms/leave 엔드포인트 제외
            .csrf(csrf -> csrf
            		.ignoringRequestMatchers("/api/**", "/ws/**") // CSRF 보호에서 제외할 엔드포인트
            )
            .authorizeHttpRequests(authorizeRequests -> authorizeRequests
                .requestMatchers("/**").permitAll()  // 인증 없이 접근 허용할 경로들
                .requestMatchers("/api/user/**", "/api/rooms/**").permitAll() // API 엔드포인트 허용 (필요 시 조정)
                .anyRequest().authenticated()  // 그 외의 요청은 인증 필요
            )
            .formLogin(formLogin -> formLogin
                .loginPage("/login")  // 로그인 페이지 경로 설정
                .defaultSuccessUrl("/lobby", true)  // 로그인 성공 시 이동할 기본 경로 설정
                .permitAll()
            )
            .logout(logout -> logout
            	    .logoutUrl("/logout")
            	    .logoutSuccessHandler(customLogoutSuccessHandler)
            	    .invalidateHttpSession(true) // 로그아웃 시 세션 무효화
            	    .deleteCookies("JSESSIONID") // JSESSIONID 쿠키 삭제
            	    .permitAll()
            	)
            .sessionManagement(session -> session
                .sessionFixation().migrateSession()
                .maximumSessions(1)
                .expiredUrl("/login?expired")
            );

        return http.build();
    }
}
