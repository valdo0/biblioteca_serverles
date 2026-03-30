package com.sumativa1.biblioteca;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.*;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.logging.Logger;

@RestController
@RequestMapping("/api")
public class ProxyController {

    private static final Logger log = Logger.getLogger(ProxyController.class.getName());
    private final RestTemplate restTemplate;

    public ProxyController() {
        // Configurar timeout de 5 segundos
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(5000);

        RestTemplate rt = new RestTemplate(factory);

        // Hacer que StringHttpMessageConverter soporte application/json
        rt.getMessageConverters().stream()
                .filter(c -> c instanceof StringHttpMessageConverter)
                .map(c -> (StringHttpMessageConverter) c)
                .forEach(c -> c.setSupportedMediaTypes(
                        List.of(MediaType.TEXT_PLAIN, MediaType.APPLICATION_JSON, MediaType.ALL)));

        rt.setErrorHandler(new DefaultResponseErrorHandler() {
            @Override
            public boolean hasError(ClientHttpResponse response) {
                return false;
            }
        });

        this.restTemplate = rt;
    }

    @Value("${services.ms-roles.url}")
    private String msRolesUrl;

    @Value("${services.ms-auth.url}")
    private String msAuthUrl;

    @Value("${services.ms-biblioteca.url}")
    private String msBibliotecaUrl;

    @Value("${azure.function.base-url}")
    private String functionBaseUrl;

    private HttpEntity<String> jsonEntity(String payload) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(payload, headers);
    }

    /**
     * Previene que se copien cabeceras problemáticas como Transfer-Encoding y
     * devuelve JSON
     **/
    private ResponseEntity<String> cleanResponse(ResponseEntity<String> response) {
        HttpHeaders cleanHeaders = new HttpHeaders();
        cleanHeaders.setContentType(MediaType.APPLICATION_JSON);
        return ResponseEntity.status(response.getStatusCode())
                .headers(cleanHeaders)
                .body(response.getBody());
    }

    // ===== AUTENTICACIÓN (ms-auth) =====

    @PostMapping("/auth/register")
    public ResponseEntity<String> register(@RequestBody String payload) {
        log.info("BFF: Reenviando solicitud de registro de usuario a ms-auth.");
        return cleanResponse(
                restTemplate.postForEntity(msAuthUrl + "/auth/register", jsonEntity(payload), String.class));
    }

    @PostMapping("/auth/login")
    public ResponseEntity<String> login(@RequestBody String payload) {
        log.info("BFF: Reenviando solicitud de inicio de sesión a ms-auth.");
        return cleanResponse(restTemplate.postForEntity(msAuthUrl + "/auth/login", jsonEntity(payload), String.class));
    }

    // ===== ROLES (ms-roles) =====

    @PostMapping("/roles/assign")
    public ResponseEntity<String> assignRole(@RequestBody String payload) {
        log.info("BFF: Reenviando solicitud de asignación de rol a ms-roles.");
        return cleanResponse(
                restTemplate.postForEntity(msRolesUrl + "/roles/assign", jsonEntity(payload), String.class));
    }

    // ===== LIBROS (ms-biblioteca) =====

    @GetMapping("/libros")
    public ResponseEntity<String> listarLibros() {
        log.info("BFF: Reenviando solicitud de listado de libros a ms-biblioteca.");
        return cleanResponse(restTemplate.getForEntity(msBibliotecaUrl + "/libros", String.class));
    }

    @GetMapping("/libros/{id}")
    public ResponseEntity<String> obtenerLibro(@PathVariable String id) {
        log.info("BFF: Reenviando solicitud de obtención de libro ID=" + id + " a ms-biblioteca.");
        return cleanResponse(restTemplate.getForEntity(msBibliotecaUrl + "/libros/" + id, String.class));
    }

    @PostMapping("/libros")
    public ResponseEntity<String> crearLibro(@RequestBody String payload) {
        log.info("BFF: Reenviando solicitud de creación de libro a ms-biblioteca.");
        return cleanResponse(
                restTemplate.postForEntity(msBibliotecaUrl + "/libros", jsonEntity(payload), String.class));
    }

    @PutMapping("/libros/{id}")
    public ResponseEntity<String> actualizarLibro(@PathVariable String id, @RequestBody String payload) {
        log.info("BFF: Reenviando solicitud de actualización de libro ID=" + id + " a ms-biblioteca.");
        return cleanResponse(restTemplate.exchange(msBibliotecaUrl + "/libros/" + id, HttpMethod.PUT,
                jsonEntity(payload), String.class));
    }

    @DeleteMapping("/libros/{id}")
    public ResponseEntity<String> eliminarLibro(@PathVariable String id) {
        log.info("BFF: Reenviando solicitud de eliminación de libro ID=" + id + " a ms-biblioteca.");
        return cleanResponse(restTemplate.exchange(msBibliotecaUrl + "/libros/" + id, HttpMethod.DELETE,
                HttpEntity.EMPTY, String.class));
    }

    // ===== PRÉSTAMOS (ms-biblioteca) =====

    @GetMapping("/prestamos")
    public ResponseEntity<String> listarPrestamos() {
        log.info("BFF: Reenviando solicitud de listado de préstamos a ms-biblioteca.");
        return cleanResponse(restTemplate.getForEntity(msBibliotecaUrl + "/prestamos", String.class));
    }

    @GetMapping("/prestamos/{id}")
    public ResponseEntity<String> obtenerPrestamo(@PathVariable String id) {
        log.info("BFF: Reenviando solicitud de obtención de préstamo ID=" + id + " a ms-biblioteca.");
        return cleanResponse(restTemplate.getForEntity(msBibliotecaUrl + "/prestamos/" + id, String.class));
    }

    @PostMapping("/prestamos")
    public ResponseEntity<String> crearPrestamo(@RequestBody String payload) {
        log.info("BFF: Reenviando solicitud de creación de préstamo a ms-biblioteca.");
        return cleanResponse(
                restTemplate.postForEntity(msBibliotecaUrl + "/prestamos", jsonEntity(payload), String.class));
    }

    @PutMapping("/prestamos/{id}")
    public ResponseEntity<String> actualizarPrestamo(@PathVariable String id, @RequestBody String payload) {
        log.info("BFF: Reenviando solicitud de actualización de préstamo ID=" + id + " a ms-biblioteca.");
        return cleanResponse(restTemplate.exchange(msBibliotecaUrl + "/prestamos/" + id, HttpMethod.PUT,
                jsonEntity(payload), String.class));
    }

    @DeleteMapping("/prestamos/{id}")
    public ResponseEntity<String> eliminarPrestamo(@PathVariable String id) {
        log.info("BFF: Reenviando solicitud de eliminación de préstamo ID=" + id + " a ms-biblioteca.");
        return cleanResponse(restTemplate.exchange(msBibliotecaUrl + "/prestamos/" + id, HttpMethod.DELETE,
                HttpEntity.EMPTY, String.class));
    }
}
