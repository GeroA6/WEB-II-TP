package com.example.demo;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.jayway.jsonpath.JsonPath;

@SpringBootTest
@Testcontainers
public class AuthYAutorizacionIT {

    // static: un solo contenedor para toda la clase (se levanta una sola vez, no
    // por test)
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    // Reemplaza la URL/usuario/clave de application.properties por los del
    // contenedor
    @DynamicPropertySource
    static void configurarBase(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        // MockMvc con la cadena de filtros de Spring Security (incluye
        // JwtAuthenticationFilter)
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    // ---------- helpers ----------

    private String emailUnico() {
        return "user-" + UUID.randomUUID() + "@test.com";
    }

    private void registrar(String email, String password) throws Exception {
        mockMvc.perform(post("/api/auth/registro")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email":"%s","password":"%s"}
                        """.formatted(email, password)))
                .andExpect(status().isCreated());
    }

    private String login(String email, String password) throws Exception {
        String body = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email":"%s","password":"%s"}
                        """.formatted(email, password)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.token");
    }

    private String registrarYLoguear() throws Exception {
        String email = emailUnico();
        registrar(email, "password123");
        return login(email, "password123");
    }

    private Integer crearLista(String token, String nombre) throws Exception {
        String body = mockMvc.perform(post("/api/listas")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"nombre":"%s"}
                        """.formatted(nombre)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.id");
    }

    // ---------- los posibles escenarios ----------

    @Test
    @DisplayName("Login con credenciales válidas → 200 con token")
    void loginValido_devuelve200ConToken() throws Exception {
        String email = emailUnico();
        registrar(email, "password123");
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email":"%s","password":"password123"}
                        """.formatted(email)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    @DisplayName("Login con credenciales inválidas → 401")
    void loginInvalido_devuelve401() throws Exception {
        String email = emailUnico();
        registrar(email, "password123");
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email":"%s","password":"incorrecta"}
                        """.formatted(email)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Acceder a una lista propia → 200")
    void listaPropia_devuelve200() throws Exception {
        String token = registrarYLoguear();
        Integer listaId = crearLista(token, "Mi lista");
        mockMvc.perform(get("/api/listas/" + listaId)
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Acceder a una lista ajena → 403")
    void listaAjena_devuelve403() throws Exception {
        String tokenDuenio = registrarYLoguear();
        String tokenIntruso = registrarYLoguear();
        Integer listaId = crearLista(tokenDuenio, "Lista privada");
        mockMvc.perform(get("/api/listas/" + listaId)
                .header("Authorization", "Bearer " + tokenIntruso))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Acceder a /api/listas sin token → 401")
    void sinToken_devuelve401() throws Exception {
        mockMvc.perform(get("/api/listas"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("ADMIN accediendo a una lista ajena → 200")
    void adminListaAjena_devuelve200() throws Exception {
        String tokenDuenio = registrarYLoguear();
        Integer listaId = crearLista(tokenDuenio, "Lista de otro");
        // Igual que en la vida real: se promueve por SQL y se vuelve a loguear
        String emailAdmin = emailUnico();
        registrar(emailAdmin, "password123");
        jdbcTemplate.update("UPDATE usuarios SET rol = 'ADMIN' WHERE email = ?", emailAdmin);
        String tokenAdmin = login(emailAdmin, "password123");
        mockMvc.perform(get("/api/listas/" + listaId)
                .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isOk());
    }
}
