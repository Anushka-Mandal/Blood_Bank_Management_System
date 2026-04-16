package com.bbms.config;

import com.bbms.model.User;
import com.bbms.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import java.util.List;

/**
 * SecurityConfig — Spring Security setup.
 * Pattern: Facade — simplifies complex security configuration.
 * SOLID: Open-Closed — new roles can be added without changing existing rules.
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final UserRepository userRepository;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.ignoringRequestMatchers("/h2-console/**"))
            .headers(h -> h.frameOptions(fo -> fo.sameOrigin()))
            .authorizeHttpRequests(auth -> auth
                // Public routes
                .requestMatchers("/", "/register", "/login", "/css/**", "/js/**", "/h2-console/**").permitAll()
                // Role-based routes
                .requestMatchers("/donor/**").hasRole("DONOR")
                .requestMatchers("/recipient/**").hasRole("RECIPIENT")
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .requestMatchers("/maintainer/**").hasRole("MAINTAINER")
                .requestMatchers("/hematology/**").hasRole("HEMATOLOGY")
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .successHandler((request, response, authentication) -> {
                    // Route to role-specific dashboard after login
                    String role = authentication.getAuthorities().iterator().next().getAuthority();
                    switch (role) {
                        case "ROLE_DONOR"      -> response.sendRedirect("/donor/dashboard");
                        case "ROLE_RECIPIENT"  -> response.sendRedirect("/recipient/dashboard");
                        case "ROLE_ADMIN"      -> response.sendRedirect("/admin/dashboard");
                        case "ROLE_MAINTAINER" -> response.sendRedirect("/maintainer/dashboard");
                        case "ROLE_HEMATOLOGY" -> response.sendRedirect("/hematology/dashboard");
                        default                -> response.sendRedirect("/");
                    }
                })
                .failureUrl("/login?error=true")
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout=true")
                .permitAll()
            );
        return http.build();
    }

    @Bean
    public UserDetailsService userDetailsService() {
        return username -> {
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

            // Block unapproved users from logging in
            if (user.getAccountStatus() == User.AccountStatus.PENDING) {
                throw new UsernameNotFoundException("Account pending approval.");
            }
            if (user.getAccountStatus() == User.AccountStatus.REJECTED) {
                throw new UsernameNotFoundException("Account rejected.");
            }

            return new org.springframework.security.core.userdetails.User(
                    user.getUsername(),
                    user.getPassword(),
                    List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
            );
        };
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
