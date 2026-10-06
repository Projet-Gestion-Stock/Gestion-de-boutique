package com.boutique.gestionboutique.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /** Hachage BCrypt : les mots de passe ne sont jamais stockés en clair. */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        // Ressources publiques
                        .requestMatchers("/login", "/error", "/webjars/**", "/css/**", "/js/**", "/images/**").permitAll()

                        // Utilisateurs : administrateur uniquement
                        .requestMatchers("/utilisateurs/**").hasRole("ADMIN")

                        // Grossistes et livraisons : administrateur uniquement
                        .requestMatchers("/grossistes/**", "/livraisons/**").hasRole("ADMIN")

                        // Produits : l'ORDRE compte (la première règle qui correspond s'applique)
                        // 1) formulaires d'ajout et de modification : ADMIN
                        .requestMatchers("/produits/nouveau", "/produits/*/modifier").hasRole("ADMIN")
                        // 2) liste et recherche : ADMIN et CAISSIER (lecture seule)
                        .requestMatchers(HttpMethod.GET, "/produits").hasAnyRole("ADMIN", "CAISSIER")
                        // 3) tout le reste (création, modification, activation) : ADMIN
                        .requestMatchers("/produits/**").hasRole("ADMIN")

                        // Catégories : administrateur uniquement
                        .requestMatchers("/categories/**").hasRole("ADMIN")

                        // Tout le reste exige d'être connecté
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .defaultSuccessUrl("/", true)
                        .failureHandler((request, response, exception) -> {
                            // Compte désactivé : message dédié ; tout le reste : message générique
                            String code = (exception instanceof DisabledException) ? "disabled" : "bad";
                            response.sendRedirect(request.getContextPath() + "/login?error=" + code);
                        })
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout")
                        .permitAll()
                );
        return http.build();
    }
}