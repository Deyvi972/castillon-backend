package com.castillon.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/deudas")
@CrossOrigin(origins = "*")
public class DeudaController {

    @Autowired
    private JdbcTemplate db;

    // GET /api/deudas
    @GetMapping
    public ResponseEntity<?> listar() {
        try {
            String sql = """
                SELECT d.IDDEUDA_GENERADA as idDeudaGenerada,
                       d.IDVENTAS         as idVentas,
                       d.DESCRIPCION      as descripcion,
                       d.ESTADO           as estado,
                       v.TOTALPAGAR       as totalPagar,
                       CONCAT(p.NOMBRES,' ',p.APEPATERNO) as clienteNombre
                FROM DEUDA_GENERADA d
                LEFT JOIN VENTA   v  ON d.IDVENTAS   = v.IDVENTA
                LEFT JOIN CLIENTE c  ON v.IDCLIENTE  = c.IDCLIENTE
                LEFT JOIN PERSONA p  ON c.IDPERSONA  = p.IDPERSONA
                ORDER BY d.IDDEUDA_GENERADA DESC
                """;
            return ResponseEntity.ok(db.queryForList(sql));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    // PUT /api/deudas/{id}  — marcar pagada u otro cambio de estado
    @PutMapping("/{id}")
    public ResponseEntity<?> actualizar(@PathVariable int id, @RequestBody Map<String, Object> body) {
        try {
            String nuevoEstado = body.get("estado") != null ? body.get("estado").toString() : "P";
            int rows = db.update("UPDATE DEUDA_GENERADA SET ESTADO=? WHERE IDDEUDA_GENERADA=?", nuevoEstado, id);
            if (rows == 0) return ResponseEntity.status(404).body(Map.of("error","Deuda no encontrada"));
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
}
