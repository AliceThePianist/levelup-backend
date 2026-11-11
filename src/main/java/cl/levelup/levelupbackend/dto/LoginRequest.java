package cl.levelup.levelupbackend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
    @NotBlank @Email String correo,
    @NotBlank @Size(min = 6, max = 72) String contrasena
) {
    @Override
    public String toString() {
        return "LoginRequest";
    }
}
