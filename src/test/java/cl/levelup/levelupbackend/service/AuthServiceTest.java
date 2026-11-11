package cl.levelup.levelupbackend.service;

import cl.levelup.levelupbackend.dto.RegistroRequest;
import cl.levelup.levelupbackend.model.Usuario;
import cl.levelup.levelupbackend.repository.UsuarioRepository;
import cl.levelup.levelupbackend.security.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AuthServiceTest {

    private final UsuarioRepository repositorio = mock(UsuarioRepository.class);
    private final AuthService servicio = new AuthService(
        repositorio, new BCryptPasswordEncoder(4), mock(AuthenticationManager.class), mock(JwtService.class)
    );

    @Test
    void rechazaContrasenaQueSupera72BytesAntesDeGuardar() {
        var error = assertThrows(ResponseStatusException.class, () -> servicio.registrar(registro("á".repeat(37))));
        assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
        verifyNoInteractions(repositorio);
    }

    @Test
    void respondeConflictoSiOtroRegistroGuardaElCorreoPrimero() {
        when(repositorio.existsByCorreoIgnoreCase("cliente@example.test")).thenReturn(false, true);
        when(repositorio.saveAndFlush(any(Usuario.class))).thenThrow(new DataIntegrityViolationException("Correo único"));

        var error = assertThrows(ResponseStatusException.class, () -> servicio.registrar(registro(UUID.randomUUID().toString())));
        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
    }

    private RegistroRequest registro(String contrasena) {
        return new RegistroRequest(
            "Cliente", "Local", "11111111-1", LocalDate.now().minusYears(25), "CLIENTE@example.test", contrasena
        );
    }
}
