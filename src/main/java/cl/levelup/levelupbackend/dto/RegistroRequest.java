package cl.levelup.levelupbackend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record RegistroRequest(
    @NotBlank @Size(min = 3) String nombre,
    @NotBlank String apellido,
    @NotBlank @Size(max = 12) String rut,
    @NotNull @Past LocalDate fechaNacimiento,
    @NotBlank @Email String correo,
    @NotBlank @Size(min = 6, max = 72) String contrasena
) {
    public RegistroRequest {
        nombre = nombre == null ? null : nombre.trim();
    }

    @Override
    public String toString() {
        return "RegistroRequest";
    }
}
