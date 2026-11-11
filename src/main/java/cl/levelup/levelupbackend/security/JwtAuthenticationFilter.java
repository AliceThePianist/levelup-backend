package cl.levelup.levelupbackend.security;

import cl.levelup.levelupbackend.model.Usuario;
import cl.levelup.levelupbackend.repository.UsuarioRepository;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String PREFIJO = "Bearer ";

    private final JwtService jwtService;
    private final UsuarioRepository usuarioRepository;

    public JwtAuthenticationFilter(JwtService jwtService, UsuarioRepository usuarioRepository) {
        this.jwtService = jwtService;
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
        throws ServletException, IOException {
        String autorizacion = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (autorizacion == null || !autorizacion.regionMatches(true, 0, PREFIJO, 0, PREFIJO.length())) {
            chain.doFilter(request, response);
            return;
        }

        try {
            Long usuarioId = jwtService.validarToken(autorizacion.substring(PREFIJO.length()));
            Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new JwtException("El usuario del token no existe."));
            var autoridad = new SimpleGrantedAuthority("ROLE_" + usuario.getRol().name());
            var autenticacion = UsernamePasswordAuthenticationToken.authenticated(
                usuario.getCorreo(), null, List.of(autoridad)
            );
            var contexto = SecurityContextHolder.createEmptyContext();
            contexto.setAuthentication(autenticacion);
            SecurityContextHolder.setContext(contexto);
        } catch (JwtException | IllegalArgumentException ex) {
            SecurityContextHolder.clearContext();
            responderNoAutorizado(response);
            return;
        }

        chain.doFilter(request, response);
    }

    static void responderNoAutorizado(HttpServletResponse response) throws IOException {
        response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
        response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
    }
}
