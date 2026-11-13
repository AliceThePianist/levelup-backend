package cl.levelup.levelupbackend.controller;

import cl.levelup.levelupbackend.service.ImagenService;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URI;
import java.nio.file.NoSuchFileException;
import java.time.Duration;
import java.util.Map;

@RestController
@RequestMapping("/api/imagenes")
public class ImagenController {

    private final ImagenService imagenService;

    public ImagenController(ImagenService imagenService) {
        this.imagenService = imagenService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, String>> subir(@RequestParam("archivo") MultipartFile archivo) throws IOException {
        String ruta = "/api/imagenes/" + imagenService.guardar(archivo);
        return ResponseEntity.created(URI.create(ruta)).body(Map.of("imagen", ruta));
    }

    @GetMapping("/{nombre}")
    public ResponseEntity<byte[]> consultar(@PathVariable String nombre) throws IOException {
        ImagenService.Imagen imagen = imagenService.leer(nombre);
        return ResponseEntity.ok()
            .contentType(imagen.tipo())
            .cacheControl(CacheControl.maxAge(Duration.ofDays(1)).cachePublic())
            .body(imagen.bytes());
    }

    @ExceptionHandler(NoSuchFileException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public void imagenInexistente() {
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    @ResponseStatus(HttpStatus.CONTENT_TOO_LARGE)
    public void imagenDemasiadoGrande() {
    }

    @ExceptionHandler(IOException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public void errorDeAlmacenamiento() {
    }
}
