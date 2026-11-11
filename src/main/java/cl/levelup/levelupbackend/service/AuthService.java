package cl.levelup.levelupbackend.service;

import cl.levelup.levelupbackend.dto.AuthResponse;
import cl.levelup.levelupbackend.dto.LoginRequest;
import cl.levelup.levelupbackend.dto.RegistroRequest;
import cl.levelup.levelupbackend.dto.UsuarioResponse;
import cl.levelup.levelupbackend.model.Rol;
import cl.levelup.levelupbackend.model.Usuario;
import cl.levelup.levelupbackend.repository.UsuarioRepository;
import cl.levelup.levelupbackend.security.JwtService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Locale;

@Service
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthService(
        UsuarioRepository usuarioRepository,
        PasswordEncoder passwordEncoder,
        AuthenticationManager authenticationManager,
        JwtService jwtService
    ) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    public UsuarioResponse registrar(RegistroRequest solicitud) {
        validarLongitudContrasena(solicitud.contrasena());
        if (solicitud.fechaNacimiento().isAfter(LocalDate.now().minusYears(18))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Debes tener al menos 18 años.");
        }

        String correo = normalizarCorreo(solicitud.correo());
        if (usuarioRepository.existsByCorreoIgnoreCase(correo)) {
            throw correoDuplicado();
        }

        Usuario usuario = new Usuario();
        usuario.setNombre(solicitud.nombre());
        usuario.setApellido(solicitud.apellido().trim());
        usuario.setRut(solicitud.rut().trim());
        usuario.setFechaNacimiento(solicitud.fechaNacimiento());
        usuario.setCorreo(correo);
        usuario.setContrasena(passwordEncoder.encode(solicitud.contrasena()));
        usuario.setRol(Rol.CLIENTE);

        try {
            return UsuarioResponse.de(usuarioRepository.saveAndFlush(usuario));
        } catch (DataIntegrityViolationException ex) {
            // La restricción única también cubre registros simultáneos del mismo correo.
            if (usuarioRepository.existsByCorreoIgnoreCase(correo)) {
                throw correoDuplicado();
            }
            throw ex;
        }
    }

    public AuthResponse iniciarSesion(LoginRequest solicitud) {
        validarLongitudContrasena(solicitud.contrasena());
        Authentication autenticacion;
        try {
            autenticacion = authenticationManager.authenticate(UsernamePasswordAuthenticationToken.unauthenticated(
                normalizarCorreo(solicitud.correo()), solicitud.contrasena()
            ));
        } catch (BadCredentialsException ex) {
            throw credencialesInvalidas();
        }

        Usuario usuario = usuarioRepository.findByCorreoIgnoreCase(autenticacion.getName())
            .orElseThrow(AuthService::credencialesInvalidas);
        return new AuthResponse(
            jwtService.generarToken(usuario.getId()), "Bearer", jwtService.getDuracionSegundos(),
            UsuarioResponse.de(usuario)
        );
    }

    private static String normalizarCorreo(String correo) {
        return correo.trim().toLowerCase(Locale.ROOT);
    }

    private static void validarLongitudContrasena(String contrasena) {
        if (contrasena.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La contraseña no puede superar 72 bytes.");
        }
    }

    private static ResponseStatusException correoDuplicado() {
        return new ResponseStatusException(HttpStatus.CONFLICT, "Este correo ya está registrado.");
    }

    private static ResponseStatusException credencialesInvalidas() {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Correo o contraseña incorrectos.");
    }
}
