package cl.levelup.levelupbackend.service;

import cl.levelup.levelupbackend.model.Producto;
import cl.levelup.levelupbackend.repository.ProductoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class ProductoService {

    private final ProductoRepository productoRepository;

    public ProductoService(ProductoRepository productoRepository) {
        this.productoRepository = productoRepository;
    }

    public List<Producto> listar() {
        return productoRepository.findAll();
    }

    public Optional<Producto> buscarPorId(Long id) {
        return productoRepository.findById(id);
    }

    public Producto crear(Producto producto) {
        validarPrecioYStock(producto);
        return productoRepository.save(producto);
    }

    @Transactional
    public Optional<Producto> actualizar(Long id, Producto datos) {
        validarPrecioYStock(datos);
        return productoRepository.findById(id).map(producto -> {
            producto.setNombre(datos.getNombre());
            producto.setDescripcion(datos.getDescripcion());
            producto.setCategoria(datos.getCategoria());
            producto.setPrecio(datos.getPrecio());
            producto.setStock(datos.getStock());
            producto.setImagen(datos.getImagen());
            return productoRepository.save(producto);
        });
    }

    @Transactional
    public boolean eliminar(Long id) {
        Optional<Producto> producto = productoRepository.findById(id);
        if (producto.isEmpty()) {
            return false;
        }

        productoRepository.delete(producto.get());
        return true;
    }

    private void validarPrecioYStock(Producto producto) {
        if (producto.getPrecio() == null || producto.getPrecio().signum() <= 0) {
            throw new IllegalArgumentException("El precio debe ser mayor que cero.");
        }

        if (producto.getStock() == null || producto.getStock() < 0) {
            throw new IllegalArgumentException("El stock debe ser mayor o igual a cero.");
        }
    }
}
