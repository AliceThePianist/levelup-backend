package cl.levelup.levelupbackend.security;

import cl.levelup.levelupbackend.model.Rol;
import cl.levelup.levelupbackend.repository.UsuarioRepository;
import jakarta.servlet.DispatcherType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    UserDetailsService userDetailsService(UsuarioRepository usuarioRepository) {
        return correo -> usuarioRepository.findByCorreoIgnoreCase(correo)
            .map(usuario -> User.withUsername(usuario.getCorreo())
                .password(usuario.getContrasena())
                .roles(usuario.getRol().name())
                .build())
            .orElseThrow(() -> new UsernameNotFoundException("Usuario inexistente."));
    }

    @Bean
    AuthenticationManager authenticationManager(UserDetailsService userDetailsService, PasswordEncoder passwordEncoder) {
        var proveedor = new DaoAuthenticationProvider(userDetailsService);
        proveedor.setPasswordEncoder(passwordEncoder);
        return new ProviderManager(proveedor);
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, JwtService jwtService, UsuarioRepository usuarioRepository)
        throws Exception {
        String administrador = Rol.ADMINISTRADOR.name();
        return http
            // El cliente envía el JWT explícitamente en Authorization. La API no autentica mediante cookies.
            .csrf(AbstractHttpConfigurer::disable)
            .httpBasic(AbstractHttpConfigurer::disable)
            .formLogin(AbstractHttpConfigurer::disable)
            .logout(AbstractHttpConfigurer::disable)
            .requestCache(AbstractHttpConfigurer::disable)
            .sessionManagement(sesion -> sesion.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(errores -> errores.authenticationEntryPoint(
                (request, response, ex) -> JwtAuthenticationFilter.responderNoAutorizado(response)
            ))
            .authorizeHttpRequests(rutas -> rutas
                .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                .requestMatchers(HttpMethod.POST, "/api/auth/registro", "/api/auth/login").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/productos", "/api/productos/{id}").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/productos").hasRole(administrador)
                .requestMatchers(HttpMethod.PUT, "/api/productos/{id}").hasRole(administrador)
                .requestMatchers(HttpMethod.DELETE, "/api/productos/{id}").hasRole(administrador)
                .anyRequest().denyAll()
            )
            .addFilterBefore(new JwtAuthenticationFilter(jwtService, usuarioRepository), UsernamePasswordAuthenticationFilter.class)
            .build();
    }
}
