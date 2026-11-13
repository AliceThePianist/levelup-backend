package cl.levelup.levelupbackend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class ImagenService {

    private static final int MAX_BYTES = 5 * 1024 * 1024;
    private static final Pattern NOMBRE_GUARDADO =
        Pattern.compile("[0-9a-f]{8}(?:-[0-9a-f]{4}){3}-[0-9a-f]{12}\\.(jpg|png|webp)");

    private final Path directorio;

    public ImagenService(@Value("${app.imagenes.directorio:./imagenes}") String directorio) {
        this.directorio = Path.of(directorio).toAbsolutePath().normalize();
    }

    public String guardar(MultipartFile archivo) throws IOException {
        if (archivo.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selecciona una imagen.");
        }
        if (archivo.getSize() > MAX_BYTES) {
            throw new ResponseStatusException(HttpStatus.CONTENT_TOO_LARGE);
        }
        String original = archivo.getOriginalFilename();
        String extension = extension(original == null ? "" : original);
        if (extension.equals("jpeg")) {
            extension = "jpg";
        }
        MediaType tipo = tipoDe(extension);
        if (tipo == null || !tipo.toString().equalsIgnoreCase(archivo.getContentType())) {
            throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE);
        }
        byte[] bytes = archivo.getBytes();
        if (!tieneFirma(bytes, extension)) {
            throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE);
        }

        Path carpeta = Files.createDirectories(directorio).toRealPath();
        String nombre = UUID.randomUUID() + "." + extension;
        Path temporal = Files.createTempFile(carpeta, "imagen-", ".tmp");
        try {
            Files.write(temporal, bytes);
            Files.move(temporal, carpeta.resolve(nombre));
        } finally {
            Files.deleteIfExists(temporal);
        }
        return nombre;
    }

    public Imagen leer(String nombre) throws IOException {
        if (!NOMBRE_GUARDADO.matcher(nombre).matches()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        Path ruta = directorio.toRealPath().resolve(nombre);
        if (!Files.isRegularFile(ruta, LinkOption.NOFOLLOW_LINKS)) {
            throw new NoSuchFileException(nombre);
        }
        try (var entrada = Files.newInputStream(ruta, LinkOption.NOFOLLOW_LINKS)) {
            byte[] bytes = entrada.readNBytes(MAX_BYTES + 1);
            if (bytes.length > MAX_BYTES || !tieneFirma(bytes, extension(nombre))) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND);
            }
            return new Imagen(tipoDe(extension(nombre)), bytes);
        }
    }

    private static String extension(String nombre) {
        int punto = nombre.lastIndexOf('.');
        return punto < 0 ? "" : nombre.substring(punto + 1).toLowerCase(Locale.ROOT);
    }

    private static MediaType tipoDe(String extension) {
        return switch (extension) {
            case "jpg" -> MediaType.IMAGE_JPEG;
            case "png" -> MediaType.IMAGE_PNG;
            case "webp" -> MediaType.valueOf("image/webp");
            default -> null;
        };
    }

    private static boolean tieneFirma(byte[] bytes, String extension) {
        return switch (extension) {
            case "jpg" -> coincide(bytes, 0, "ffd8ff");
            case "png" -> coincide(bytes, 0, "89504e470d0a1a0a");
            case "webp" -> coincide(bytes, 0, "52494646") && coincide(bytes, 8, "57454250")
                && (coincide(bytes, 12, "56503820") || coincide(bytes, 12, "5650384c")
                    || coincide(bytes, 12, "56503858"));
            default -> false;
        };
    }

    private static boolean coincide(byte[] bytes, int inicio, String firma) {
        byte[] esperados = HexFormat.of().parseHex(firma);
        int fin = inicio + esperados.length;
        return bytes.length >= fin && Arrays.equals(bytes, inicio, fin, esperados, 0, esperados.length);
    }

    public record Imagen(MediaType tipo, byte[] bytes) {
    }
}
