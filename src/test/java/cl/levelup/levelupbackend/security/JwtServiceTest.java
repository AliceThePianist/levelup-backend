package cl.levelup.levelupbackend.security;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Base64;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtServiceTest {

    @Test
    void validaFirmaEmisorYExpiracion() {
        var clave = Jwts.SIG.HS256.key().build();
        var jwt = new JwtService(Base64.getEncoder().encodeToString(clave.getEncoded()), 3600);

        assertEquals(42L, jwt.validarToken(jwt.generarToken(42L)));

        var otraClave = Jwts.SIG.HS256.key().build();
        var otroJwt = new JwtService(Base64.getEncoder().encodeToString(otraClave.getEncoded()), 3600);
        assertThrows(JwtException.class, () -> jwt.validarToken(otroJwt.generarToken(42L)));

        var vencido = Jwts.builder().issuer("levelup-backend").subject("42")
            .expiration(Date.from(Instant.now().minusSeconds(60))).signWith(clave).compact();
        var sinVencimiento = Jwts.builder().issuer("levelup-backend").subject("42")
            .signWith(clave).compact();
        var otroEmisor = Jwts.builder().issuer("otra-api").subject("42")
            .expiration(Date.from(Instant.now().plusSeconds(60))).signWith(clave).compact();
        assertThrows(JwtException.class, () -> jwt.validarToken(vencido));
        assertThrows(JwtException.class, () -> jwt.validarToken(sinVencimiento));
        assertThrows(JwtException.class, () -> jwt.validarToken(otroEmisor));
    }
}
