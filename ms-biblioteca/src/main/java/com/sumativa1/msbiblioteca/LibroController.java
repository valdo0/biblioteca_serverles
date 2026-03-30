package com.sumativa1.msbiblioteca;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

/**
 * Controlador CRUD para Libros de la Biblioteca.
 * En producción, usar JPA Repository con Oracle.
 */
@RestController
@RequestMapping("/libros")
public class LibroController {

    private static final Logger log = Logger.getLogger(LibroController.class.getName());

    // Almacenamiento en memoria (reemplazar con Oracle JPA en producción)
    private static final Map<Integer, Map<String, Object>> libros = new ConcurrentHashMap<>();
    private static final AtomicInteger idCounter = new AtomicInteger(1);

    static {
        // Datos de ejemplo iniciales
        Map<String, Object> l1 = new LinkedHashMap<>();
        l1.put("id", 1);
        l1.put("titulo", "Cien Años de Soledad");
        l1.put("autor", "Gabriel García Márquez");
        l1.put("isbn", "978-0307474728");
        l1.put("cantidad", 3);
        l1.put("disponible", true);
        libros.put(1, l1);

        Map<String, Object> l2 = new LinkedHashMap<>();
        l2.put("id", 2);
        l2.put("titulo", "El Principito");
        l2.put("autor", "Antoine de Saint-Exupéry");
        l2.put("isbn", "978-0156012195");
        l2.put("cantidad", 5);
        l2.put("disponible", true);
        libros.put(2, l2);
        idCounter.set(3);
    }

    // GET /libros — Listar todos
    @GetMapping
    public ResponseEntity<Collection<Map<String, Object>>> listar() {
        log.info("ms-biblioteca: Solicitud de listado de todos los libros.");
        return ResponseEntity.ok(libros.values());
    }

    // GET /libros/{id} — Obtener por ID
    @GetMapping("/{id}")
    public ResponseEntity<?> obtener(@PathVariable int id) {
        log.info("ms-biblioteca: Solicitud de obtención de libro con ID: " + id);
        Map<String, Object> libro = libros.get(id);
        if (libro == null) {
            log.warning("ms-biblioteca: Libro con ID " + id + " no encontrado.");
            return ResponseEntity.status(404).body(Map.of("error", "Libro no encontrado"));
        }
        return ResponseEntity.ok(libro);
    }

    // POST /libros — Crear
    @PostMapping
    public ResponseEntity<?> crear(@RequestBody Map<String, Object> body) {
        log.info("ms-biblioteca: Solicitud de creación de nuevo libro: " + body.get("titulo"));
        int nuevoId = idCounter.getAndIncrement();
        body.put("id", nuevoId);
        body.put("disponible", true);
        libros.put(nuevoId, body);
        log.info("ms-biblioteca: Libro creado exitosamente con ID: " + nuevoId);
        return ResponseEntity.status(201).body(body);
    }

    // PUT /libros/{id} — Actualizar
    @PutMapping("/{id}")
    public ResponseEntity<?> actualizar(@PathVariable int id, @RequestBody Map<String, Object> body) {
        log.info("ms-biblioteca: Solicitud de actualización de libro con ID: " + id);
        if (!libros.containsKey(id)) {
            return ResponseEntity.status(404).body(Map.of("error", "Libro no encontrado"));
        }
        body.put("id", id);
        libros.put(id, body);
        log.info("ms-biblioteca: Libro con ID " + id + " actualizado correctamente.");
        return ResponseEntity.ok(body);
    }

    // DELETE /libros/{id} — Eliminar
    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminar(@PathVariable int id) {
        log.info("ms-biblioteca: Solicitud de eliminación de libro con ID: " + id);
        if (libros.remove(id) == null) {
            return ResponseEntity.status(404).body(Map.of("error", "Libro no encontrado"));
        }
        log.info("ms-biblioteca: Libro con ID " + id + " eliminado exitosamente.");
        return ResponseEntity.ok(Map.of("mensaje", "Libro eliminado exitosamente"));
    }
}
