package com.castillon.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.web.bind.annotation.*;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.*;

@RestController
@CrossOrigin(origins = "*")
public class UsuarioController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // ─────────────────────────────────────────
    // TIPO_USUARIO
    // ─────────────────────────────────────────
    @GetMapping("/api/tipo-usuarios")
    public ResponseEntity<?> getTipoUsuarios() {
        try {
            String sql = "SELECT IDTIPO_USUARIO as idTipoUsuario, NOMUSUARIO as nomUsuario FROM TIPO_USUARIO ORDER BY IDTIPO_USUARIO";
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);
            return ResponseEntity.ok(rows);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    // ─────────────────────────────────────────
    // EMPLEADOS
    // ─────────────────────────────────────────
    @GetMapping("/api/empleados-dropdown")
    public ResponseEntity<?> getEmpleados() {
        try {
            String sql = """
                SELECT e.IDEMPLEADO as idEmpleado,
                       p.NOMBRES    as nombres,
                       p.APEPATERNO as apePaterno,
                       p.APEMATERNO as apeMaterno
                FROM EMPLEADO e
                INNER JOIN PERSONA p ON e.IDPERSONA = p.IDPERSONA
                ORDER BY p.APEPATERNO, p.NOMBRES
                """;
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);
            return ResponseEntity.ok(rows);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    // ─────────────────────────────────────────
    // USUARIOS — Listar
    // ─────────────────────────────────────────
    @GetMapping("/api/usuarios")
    public ResponseEntity<?> getUsuarios() {
        try {
            String sql = """
                SELECT u.IDUSUARIO      as idUsuario,
                       u.LOGEO          as logeo,
                       u.ESTADO         as estado,
                       u.IDEMPLEADO     as idEmpleado,
                       u.IDTIPO_USUARIO as idTipoUsuario,
                       CONCAT(p.NOMBRES, ' ', p.APEPATERNO) as empleadoNombre,
                       t.NOMUSUARIO    as tipoUsuario
                FROM USUARIO u
                INNER JOIN EMPLEADO    e ON u.IDEMPLEADO     = e.IDEMPLEADO
                INNER JOIN PERSONA     p ON e.IDPERSONA      = p.IDPERSONA
                INNER JOIN TIPO_USUARIO t ON u.IDTIPO_USUARIO = t.IDTIPO_USUARIO
                ORDER BY u.IDUSUARIO
                """;
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);
            // Convertir estado: '1'->A, '0'->I  (compatibilidad con el frontend)
            rows.forEach(r -> {
                Object est = r.get("estado");
                if ("1".equals(String.valueOf(est))) r.put("estado", "A");
                else if ("0".equals(String.valueOf(est))) r.put("estado", "I");
            });
            return ResponseEntity.ok(rows);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    // ─────────────────────────────────────────
    // USUARIOS — Crear
    // ─────────────────────────────────────────
    @PostMapping("/api/usuarios")
    public ResponseEntity<?> crearUsuario(@RequestBody Map<String, Object> body) {
        try {
            String logeo      = String.valueOf(body.get("logeo"));
            String clave      = String.valueOf(body.get("clave"));
            int idTipo        = Integer.parseInt(String.valueOf(body.get("idTipoUsuario")));
            int idEmpleado    = Integer.parseInt(String.valueOf(body.get("idEmpleado")));
            String estadoFront = String.valueOf(body.getOrDefault("estado", "A"));
            String estado     = "A".equals(estadoFront) ? "1" : "0";

            String sql = "INSERT INTO USUARIO (LOGEO, CLAVE, IDTIPO_USUARIO, IDEMPLEADO, ESTADO) VALUES (?, ?, ?, ?, ?)";

            KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbcTemplate.update(con -> {
                PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
                ps.setString(1, logeo);
                ps.setString(2, clave);
                ps.setInt(3, idTipo);
                ps.setInt(4, idEmpleado);
                ps.setString(5, estado);
                return ps;
            }, keyHolder);

            Number generatedId = keyHolder.getKey();
            return ResponseEntity.ok(Map.of(
                "success", true,
                "idUsuario", generatedId != null ? generatedId.intValue() : -1,
                "mensaje", "Usuario creado correctamente"
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    // ─────────────────────────────────────────
    // USUARIOS — Actualizar
    // ─────────────────────────────────────────
    @PutMapping("/api/usuarios/{id}")
    public ResponseEntity<?> actualizarUsuario(@PathVariable int id, @RequestBody Map<String, Object> body) {
        try {
            String logeo      = String.valueOf(body.get("logeo"));
            String clave      = String.valueOf(body.get("clave"));
            int idTipo        = Integer.parseInt(String.valueOf(body.get("idTipoUsuario")));
            int idEmpleado    = Integer.parseInt(String.valueOf(body.get("idEmpleado")));
            String estadoFront = String.valueOf(body.getOrDefault("estado", "A"));
            String estado     = "A".equals(estadoFront) ? "1" : "0";

            String sql = "UPDATE USUARIO SET LOGEO=?, CLAVE=?, IDTIPO_USUARIO=?, IDEMPLEADO=?, ESTADO=? WHERE IDUSUARIO=?";
            int rows = jdbcTemplate.update(sql, logeo, clave, idTipo, idEmpleado, estado, id);

            if (rows == 0) return ResponseEntity.status(404).body(Map.of("error", "Usuario no encontrado"));
            return ResponseEntity.ok(Map.of("success", true, "mensaje", "Usuario actualizado"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    // ─────────────────────────────────────────
    // USUARIOS — Eliminar
    // ─────────────────────────────────────────
    @DeleteMapping("/api/usuarios/{id}")
    public ResponseEntity<?> eliminarUsuario(@PathVariable int id) {
        try {
            int rows = jdbcTemplate.update("DELETE FROM USUARIO WHERE IDUSUARIO=?", id);
            if (rows == 0) return ResponseEntity.status(404).body(Map.of("error", "Usuario no encontrado"));
            return ResponseEntity.ok(Map.of("success", true, "mensaje", "Usuario eliminado"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
}
