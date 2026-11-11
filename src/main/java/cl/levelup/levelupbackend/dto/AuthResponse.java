package cl.levelup.levelupbackend.dto;

public record AuthResponse(String token, String tipoToken, long expiraEn, UsuarioResponse usuario) {
}
