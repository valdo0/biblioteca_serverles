package com.sumativa1.msbiblioteca;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

/**
 * Controlador CRUD para Préstamos de la Biblioteca.
 * En producción, usar JPA Repository con Oracle.
 */
@RestController
@RequestMapping("/prestamos")
public class PrestamoController {

    private static final Logger log = Logger.getLogger(PrestamoController.class.getName());

    private static final Map<Integer, Map<String, Object>> prestamos = new ConcurrentHashMap<>();
    private static final AtomicInteger idCounter = new AtomicInteger(1);

    static {
        Map<String, Object> p1 = new LinkedHashMap<>();
        p1.put("id", 1);
        p1.put("usuarioId", 1);
        p1.put("libroId", 1);
        p1.put("libroTitulo", "Cien Años de Soledad");
        p1.put("fechaPrestamo", "2026-03-01");
        p1.put("fechaDevolucion", "2026-03-15");
        p1.put("estado", "ACTIVO");
        prestamos.put(1, p1);
        idCounter.set(2);
    }

    // GET /prestamos — Listar todos
    @GetMapping
    public ResponseEntity<Collection<Map<String, Object>>> listar() {
        log.info("ms-biblioteca: Solicitud de listado de todos los préstamos.");
        return ResponseEntity.ok(prestamos.values());
    }

    // GET /prestamos/{id}
    @GetMapping("/{id}")
    public ResponseEntity<?> obtener(@PathVariable int id) {
        log.info("ms-biblioteca: Solicitud de obtención de préstamo con ID: " + id);
        Map<String, Object> prestamo = prestamos.get(id);
        if (prestamo == null) {
            log.warning("ms-biblioteca: Préstamo con ID " + id + " no encontrado.");
            return ResponseEntity.status(404).body(Map.of("error", "Préstamo no encontrado"));
        }
        return ResponseEntity.ok(prestamo);
    }

    // POST /prestamos — Crear
    @PostMapping
    public ResponseEntity<?> crear(@RequestBody Map<String, Object> body) {
        log.info("ms-biblioteca: Solicitud de creación de nuevo préstamo para usuario: " + body.get("usuarioId"));
        int nuevoId = idCounter.getAndIncrement();
        body.put("id", nuevoId);
        body.put("estado", "ACTIVO");
        prestamos.put(nuevoId, body);
        log.info("ms-biblioteca: Préstamo creado exitosamente con ID: " + nuevoId);
        return ResponseEntity.status(201).body(body);
    }

    // PUT /prestamos/{id} — Actualizar (ej. cambiar estado a DEVUELTO)
    @PutMapping("/{id}")
    public ResponseEntity<?> actualizar(@PathVariable int id, @RequestBody Map<String, Object> body) {
        log.info("ms-biblioteca: Solicitud de actualización de préstamo con ID: " + id);
        if (!prestamos.containsKey(id)) {
            return ResponseEntity.status(404).body(Map.of("error", "Préstamo no encontrado"));
        }
        body.put("id", id);
        prestamos.put(id, body);
        log.info("ms-biblioteca: Préstamo con ID " + id + " actualizado. Estado: " + body.get("estado"));
        return ResponseEntity.ok(body);
    }

    // DELETE /prestamos/{id} — Eliminar
    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminar(@PathVariable int id) {
        log.info("ms-biblioteca: Solicitud de eliminación de préstamo con ID: " + id);
        if (prestamos.remove(id) == null) {
            return ResponseEntity.status(404).body(Map.of("error", "Préstamo no encontrado"));
        }
        log.info("ms-biblioteca: Préstamo con ID " + id + " eliminado exitosamente.");
        return ResponseEntity.ok(Map.of("mensaje", "Préstamo eliminado exitosamente"));
    }
}
