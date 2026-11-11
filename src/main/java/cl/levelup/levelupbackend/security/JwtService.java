package cl.levelup.levelupbackend.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;

@Service
public class JwtService {

    private static final String EMISOR = "levelup-backend";

    private final SecretKey clave;
    private final JwtParser parser;
    private final long duracionSegundos;

    public JwtService(
        @Value("${security.jwt.secret}") String secreto,
        @Value("${security.jwt.duracion-segundos}") long duracionSegundos
    ) {
        this.clave = cargarClave(secreto);
        if (duracionSegundos <= 0) {
            throw new IllegalArgumentException("La duración del JWT debe ser mayor que cero.");
        }
        this.duracionSegundos = duracionSegundos;
        this.parser = Jwts.parser()
            .verifyWith(clave)
            .sig().clear().add(Jwts.SIG.HS256).and()
            .requireIssuer(EMISOR)
            .build();
    }

    public String generarToken(Long usuarioId) {
        Instant ahora = Instant.now();
        return Jwts.builder()
            .issuer(EMISOR)
            .subject(usuarioId.toString())
            .issuedAt(Date.from(ahora))
            .expiration(Date.from(ahora.plusSeconds(duracionSegundos)))
            .signWith(clave, Jwts.SIG.HS256)
            .compact();
    }

    public Long validarToken(String token) {
        Claims datos = parser.parseSignedClaims(token).getPayload();
        if (datos.getExpiration() == null) {
            throw new JwtException("El token debe tener fecha de expiración.");
        }

        try {
            long usuarioId = Long.parseLong(datos.getSubject());
            if (usuarioId > 0) {
                return usuarioId;
            }
        } catch (NumberFormatException ex) {
            throw new JwtException("El identificador del token es inválido.", ex);
        }
        throw new JwtException("El identificador del token es inválido.");
    }

    public long getDuracionSegundos() {
        return duracionSegundos;
    }

    private static SecretKey cargarClave(String secreto) {
        byte[] bytes;
        try {
            bytes = Base64.getDecoder().decode(secreto);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("JWT_SECRET debe estar codificado en Base64.");
        }
        if (bytes.length < 32) {
            throw new IllegalArgumentException("JWT_SECRET debe representar al menos 32 bytes.");
        }
        return new SecretKeySpec(bytes, "HmacSHA256");
    }
}
