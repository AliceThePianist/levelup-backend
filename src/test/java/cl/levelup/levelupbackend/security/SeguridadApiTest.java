package cl.levelup.levelupbackend.security;

import cl.levelup.levelupbackend.controller.AuthController;
import cl.levelup.levelupbackend.controller.ProductoController;
import cl.levelup.levelupbackend.model.Producto;
import cl.levelup.levelupbackend.model.Rol;
import cl.levelup.levelupbackend.model.Usuario;
import cl.levelup.levelupbackend.repository.ProductoRepository;
import cl.levelup.levelupbackend.repository.UsuarioRepository;
import cl.levelup.levelupbackend.service.AuthService;
import cl.levelup.levelupbackend.service.ProductoService;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDate;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringJUnitWebConfig(SeguridadApiTest.Configuracion.class)
class SeguridadApiTest {

    @Autowired private WebApplicationContext contexto;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private ProductoRepository productoRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    @DynamicPropertySource
    static void jwtTemporal(DynamicPropertyRegistry propiedades) {
        var clave = Jwts.SIG.HS256.key().build();
        propiedades.add("security.jwt.secret", () -> Base64.getEncoder().encodeToString(clave.getEncoded()));
        propiedades.add("security.jwt.duracion-segundos", () -> 3600);
    }

    @Test
    void aplicaRegistroLoginYPermisosEnLaCadenaReal() throws Exception {
        Map<Long, Usuario> usuarios = prepararUsuarios();
        prepararProductos();
        MockMvc mvc = MockMvcBuilders.webAppContextSetup(contexto).apply(springSecurity()).build();

        String clave = UUID.randomUUID().toString();
        String registro = """
            {"nombre":"Cliente","apellido":"Local","rut":"11111111-1",
             "fechaNacimiento":"%s","correo":"CLIENTE@EXAMPLE.TEST","contrasena":"%s","rol":"ADMINISTRADOR"}
            """.formatted(LocalDate.now().minusYears(25), clave);
        mvc.perform(post("/api/auth/registro").contentType(MediaType.APPLICATION_JSON).content(registro))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.correo").value("cliente@example.test"))
            .andExpect(jsonPath("$.rol").value("CLIENTE"))
            .andExpect(jsonPath("$.contrasena").doesNotExist());
        String hash = usuarios.get(1L).getContrasena();
        assertNotEquals(clave, hash);
        assertTrue(passwordEncoder.matches(clave, hash));

        mvc.perform(post("/api/auth/registro").contentType(MediaType.APPLICATION_JSON).content(registro))
            .andExpect(status().isConflict());
        mvc.perform(post("/api/auth/registro").contentType(MediaType.APPLICATION_JSON)
                .content(registro.replace("CLIENTE@EXAMPLE.TEST", "correo-invalido")))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.mensaje").value("Revisa los datos enviados."));
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(credenciales("cliente@example.test", UUID.randomUUID().toString())))
            .andExpect(status().isUnauthorized());

        String ruta = "/api/productos";
        String datos = """
            {"nombre":"Mando","descripcion":"Producto local","categoria":"Accesorios",
             "precio":19.90,"stock":2,"imagen":"/assets/mando.png"}
            """;
        mvc.perform(get(ruta)).andExpect(status().isOk());
        mvc.perform(get(ruta + "/1")).andExpect(status().isOk());
        mvc.perform(post(ruta).contentType(MediaType.APPLICATION_JSON).content(datos))
            .andExpect(status().isUnauthorized())
            .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, "Bearer"));
        mvc.perform(post(ruta).contentType(MediaType.APPLICATION_JSON).content(datos)
                .header(HttpHeaders.AUTHORIZATION, "Bearer token-invalido"))
            .andExpect(status().isUnauthorized());

        String cliente = iniciarSesion(mvc, "cliente@example.test", clave, "CLIENTE");
        mvc.perform(post(ruta).contentType(MediaType.APPLICATION_JSON).content(datos)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + cliente))
            .andExpect(status().isForbidden());
        mvc.perform(put(ruta + "/1").contentType(MediaType.APPLICATION_JSON).content(datos)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + cliente))
            .andExpect(status().isForbidden());
        mvc.perform(delete(ruta + "/1").header(HttpHeaders.AUTHORIZATION, "Bearer " + cliente))
            .andExpect(status().isForbidden());

        // Simula el rol asignado por el operador en una base de datos de desarrollo.
        usuarios.get(1L).setRol(Rol.ADMINISTRADOR);
        String administrador = iniciarSesion(mvc, "cliente@example.test", clave, "ADMINISTRADOR");
        mvc.perform(post(ruta).contentType(MediaType.APPLICATION_JSON).content(datos)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + administrador))
            .andExpect(status().isCreated());
        mvc.perform(put(ruta + "/1").contentType(MediaType.APPLICATION_JSON).content(datos)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + administrador))
            .andExpect(status().isOk());
        mvc.perform(delete(ruta + "/1").header(HttpHeaders.AUTHORIZATION, "Bearer " + administrador))
            .andExpect(status().isNoContent());

        usuarios.get(1L).setRol(Rol.CLIENTE);
        mvc.perform(delete(ruta + "/1").header(HttpHeaders.AUTHORIZATION, "Bearer " + administrador))
            .andExpect(status().isForbidden());
    }

    private Map<Long, Usuario> prepararUsuarios() {
        Map<Long, Usuario> usuarios = new HashMap<>();
        when(usuarioRepository.existsByCorreoIgnoreCase(anyString())).thenAnswer(invocacion ->
            usuarios.values().stream().anyMatch(u -> u.getCorreo().equalsIgnoreCase(invocacion.getArgument(0))));
        when(usuarioRepository.findByCorreoIgnoreCase(anyString())).thenAnswer(invocacion ->
            usuarios.values().stream().filter(u -> u.getCorreo().equalsIgnoreCase(invocacion.getArgument(0))).findFirst());
        when(usuarioRepository.findById(anyLong())).thenAnswer(invocacion ->
            Optional.ofNullable(usuarios.get(invocacion.<Long>getArgument(0))));
        when(usuarioRepository.saveAndFlush(any(Usuario.class))).thenAnswer(invocacion -> {
            Usuario usuario = invocacion.getArgument(0);
            long id = usuarios.size() + 1L;
            ReflectionTestUtils.setField(usuario, "id", id);
            usuarios.put(id, usuario);
            return usuario;
        });
        return usuarios;
    }

    private void prepararProductos() {
        Producto producto = new Producto();
        ReflectionTestUtils.setField(producto, "id", 1L);
        when(productoRepository.findById(1L)).thenReturn(Optional.of(producto));
        when(productoRepository.save(any(Producto.class))).thenAnswer(invocacion -> {
            Producto guardado = invocacion.getArgument(0);
            if (guardado.getId() == null) {
                ReflectionTestUtils.setField(guardado, "id", 2L);
            }
            return guardado;
        });
    }

    private static String credenciales(String correo, String contrasena) {
        return JsonMapper.shared().writeValueAsString(Map.of("correo", correo, "contrasena", contrasena));
    }

    private static String iniciarSesion(MockMvc mvc, String correo, String clave, String rol) throws Exception {
        var resultado = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(credenciales(correo, clave)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.tipoToken").value("Bearer"))
            .andExpect(jsonPath("$.usuario.rol").value(rol))
            .andExpect(jsonPath("$.usuario.contrasena").doesNotExist())
            .andReturn();
        assertNull(resultado.getRequest().getSession(false));
        return JsonMapper.shared().readTree(resultado.getResponse().getContentAsByteArray()).get("token").asString();
    }

    @Configuration(proxyBeanMethods = false)
    @EnableWebMvc
    @Import({SecurityConfig.class, JwtService.class, AuthService.class, AuthController.class,
        ProductoService.class, ProductoController.class})
    static class Configuracion {

        @Bean
        UsuarioRepository usuarioRepository() {
            return mock(UsuarioRepository.class);
        }

        @Bean
        ProductoRepository productoRepository() {
            return mock(ProductoRepository.class);
        }
    }
}
