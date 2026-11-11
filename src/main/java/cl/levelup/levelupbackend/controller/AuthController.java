package cl.levelup.levelupbackend.controller;

import cl.levelup.levelupbackend.dto.AuthResponse;
import cl.levelup.levelupbackend.dto.LoginRequest;
import cl.levelup.levelupbackend.dto.RegistroRequest;
import cl.levelup.levelupbackend.dto.UsuarioResponse;
import cl.levelup.levelupbackend.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/registro")
    @ResponseStatus(HttpStatus.CREATED)
    public UsuarioResponse registrar(@Valid @RequestBody RegistroRequest solicitud) {
        return authService.registrar(solicitud);
    }

    @PostMapping("/login")
    public AuthResponse iniciarSesion(@Valid @RequestBody LoginRequest solicitud) {
        return authService.iniciarSesion(solicitud);
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, HttpMessageNotReadableException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> solicitudInvalida() {
        return Map.of("mensaje", "Revisa los datos enviados.");
    }
}
