package cl.levelup.levelupbackend.repository;

import cl.levelup.levelupbackend.model.Producto;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductoRepository extends JpaRepository<Producto, Long> {
}
