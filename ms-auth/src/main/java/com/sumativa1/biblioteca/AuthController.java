package com.sumativa1.biblioteca;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private static final Logger log = Logger.getLogger(AuthController.class.getName());
    private final RestTemplate restTemplate;

    @Value("${azure.function.suspicious-activity.url}")
    private String functionUrl;

    @Autowired
    private UsuarioRepository usuarioRepository;

    private int failCount = 0;

    public AuthController() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3000);
        factory.setReadTimeout(3000);
        this.restTemplate = new RestTemplate(factory);
    }

    private HttpEntity<String> jsonEntity(String payload) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(payload, headers);
    }

    // ========== REGISTRO ==========
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, Object> body) {
        String email = (String) body.get("email");
        String nombre = (String) body.get("nombre");
        String password = (String) body.get("password");
        log.info("ms-auth: Solicitud de registro recibida para email: " + email);

        if (email == null || password == null || nombre == null) {
            return ResponseEntity.status(400).body(Map.of("error", "Email, nombre y contraseña son requeridos."));
        }

        if (usuarioRepository.existsByEmail(email)) {
            log.warning("ms-auth: Intento de registro con email ya existente: " + email);
            return ResponseEntity.status(409).body(Map.of("error", "El email ya está registrado."));
        }

        Usuario usuario = new Usuario();
        usuario.setNombre(nombre);
        usuario.setEmail(email);
        usuario.setPassword(password); // En prod: usar BCryptPasswordEncoder
        usuario.setRol((String) body.getOrDefault("rol", "LECTOR"));

        usuarioRepository.save(usuario);
        usuario.setPassword(null);

        log.info("ms-auth: Usuario registrado exitosamente con email: " + email);
        return ResponseEntity.status(201)
                .body(Map.of("mensaje", "Usuario registrado exitosamente.", "usuario", usuario));
    }

    // ========== LOGIN ==========
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, Object> body) {
        String email = body.get("email") != null ? (String) body.get("email") : "desconocido";
        String password = (String) body.get("password");
        log.info("ms-auth: Solicitud de inicio de sesión recibida para: " + email);

        boolean isSuccess = false;
        if (email != null && password != null) {
            Optional<Usuario> usuarioOpt = usuarioRepository.findByEmail(email);
            if (usuarioOpt.isPresent() && password.equals(usuarioOpt.get().getPassword())) {
                isSuccess = true;
            }
        }

        if (!isSuccess) {
            failCount++;
            log.warning("ms-auth: Intento de login fallido. Intentos acumulados: " + failCount);
            if (failCount >= 3) {
                log.severe("ms-auth: ¡3 intentos fallidos! Disparando alerta de actividad sospechosa...");
                java.util.concurrent.CompletableFuture.runAsync(() -> {
                    try {
                        restTemplate.postForEntity(functionUrl,
                                jsonEntity(
                                        "{\"alerta\":\"3 intentos fallidos de login\", \"usuario\":\"" + email + "\"}"),
                                String.class);
                        log.info("ms-auth: Alerta de seguridad enviada asíncronamente a la función serverless.");
                    } catch (Exception e) {
                        log.warning("ms-auth: No se pudo contactar la función de alerta: " + e.getMessage());
                    }
                });
                failCount = 0;
                return ResponseEntity.status(401)
                        .body(Map.of("mensaje", "Login fallido 3 veces. Administrador notificado."));
            }
            return ResponseEntity.status(401).body(Map.of("mensaje", "Login fallido. Intento número " + failCount));
        }

        failCount = 0; // reset on success
        log.info("ms-auth: Login exitoso para: " + email);
        return ResponseEntity.ok(Map.of("mensaje", "Login exitoso", "token", java.util.UUID.randomUUID().toString()));
    }

    // ========== OBTENER USUARIO POR ID ==========
    @GetMapping("/{id}")
    public ResponseEntity<?> getUsuarioById(@PathVariable Long id) {
        log.info("ms-auth: Consultando usuario con ID: " + id);
        Optional<Usuario> usuarioOpt = usuarioRepository.findById(id);
        
        if (usuarioOpt.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("error", "Usuario no encontrado."));
        }
        
        Usuario usuario = usuarioOpt.get();
        usuario.setPassword(null); // Ocultar contraseña
        
        return ResponseEntity.ok(usuario);
    }
}
