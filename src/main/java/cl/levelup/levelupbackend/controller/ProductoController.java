package cl.levelup.levelupbackend.controller;

import cl.levelup.levelupbackend.dto.ProductoRequest;
import cl.levelup.levelupbackend.model.Producto;
import cl.levelup.levelupbackend.service.ProductoService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/productos")
public class ProductoController {

    private final ProductoService productoService;

    public ProductoController(ProductoService productoService) {
        this.productoService = productoService;
    }

    @GetMapping
    public List<Producto> listar() {
        return productoService.listar();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Producto> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.of(productoService.buscarPorId(id));
    }

    @PostMapping
    public ResponseEntity<Producto> crear(@Valid @RequestBody ProductoRequest solicitud) {
        Producto producto = productoService.crear(convertir(solicitud));
        URI ubicacion = URI.create("/api/productos/" + producto.getId());
        return ResponseEntity.created(ubicacion).body(producto);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Producto> actualizar(
        @PathVariable Long id,
        @Valid @RequestBody ProductoRequest solicitud
    ) {
        return ResponseEntity.of(productoService.actualizar(id, convertir(solicitud)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        if (productoService.eliminar(id)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    private Producto convertir(ProductoRequest solicitud) {
        Producto producto = new Producto();
        producto.setNombre(solicitud.nombre().trim());
        producto.setDescripcion(solicitud.descripcion().trim());
        producto.setCategoria(solicitud.categoria().trim());
        producto.setPrecio(solicitud.precio());
        producto.setStock(solicitud.stock());
        producto.setImagen(solicitud.imagen().trim());
        return producto;
    }
}
