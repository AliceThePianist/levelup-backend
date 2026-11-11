package cl.levelup.levelupbackend.dto;

import cl.levelup.levelupbackend.model.Rol;
import cl.levelup.levelupbackend.model.Usuario;

import java.time.LocalDate;

public record UsuarioResponse(
    Long id,
    String nombre,
    String apellido,
    String rut,
    LocalDate fechaNacimiento,
    String correo,
    Rol rol,
    int descuento
) {
    public static UsuarioResponse de(Usuario usuario) {
        String correo = usuario.getCorreo();
        String dominio = correo.substring(correo.lastIndexOf('@') + 1);
        boolean correoDuoc = dominio.equalsIgnoreCase("duoc.cl") || dominio.equalsIgnoreCase("duocuc.cl");
        int descuento = usuario.getRol() == Rol.CLIENTE && correoDuoc ? 20 : 0;
        return new UsuarioResponse(
            usuario.getId(), usuario.getNombre(), usuario.getApellido(), usuario.getRut(),
            usuario.getFechaNacimiento(), correo, usuario.getRol(), descuento
        );
    }
}
