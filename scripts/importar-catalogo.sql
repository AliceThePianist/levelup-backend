-- Importación manual. Ejecutar después de que Hibernate cree public.productos.
-- Conserva productos existentes con el mismo nombre, ignorando mayúsculas y espacios exteriores.
BEGIN;

LOCK TABLE public.productos IN SHARE ROW EXCLUSIVE MODE;

INSERT INTO public.productos (nombre, descripcion, categoria, precio, stock, imagen)
SELECT original.nombre, original.descripcion, original.categoria, original.precio, original.stock, original.imagen
FROM (VALUES
    ('Catan', 'Juego de estrategia ideal para compartir con amigos.',
     'Juegos de Mesa', 29990, 10, '/catalogo/catan.jpg'),
    ('Controlador Inalámbrico Xbox Series X', 'Control inalámbrico compatible con consolas Xbox y computadores.',
     'Accesorios', 59990, 10, '/catalogo/controlador-xbox.jpg'),
    ('PlayStation 5', 'Consola de última generación con gráficos de alta calidad.',
     'Consolas', 549990, 10, '/catalogo/playstation-5.jpg'),
    ('PC Gamer ASUS ROG Strix', 'Equipo de alto rendimiento para los jugadores más exigentes.',
     'Computadores Gamers', 1299990, 10, '/catalogo/computador-gamer.jpg')
) AS original(nombre, descripcion, categoria, precio, stock, imagen)
WHERE NOT EXISTS (
    SELECT 1 FROM public.productos AS existente
    WHERE lower(btrim(existente.nombre)) = lower(original.nombre)
)
RETURNING id, nombre;

COMMIT;
