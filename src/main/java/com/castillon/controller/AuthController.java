package com.castillon.controller;

import com.castillon.model.LoginRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody LoginRequest request) {
        Map<String, Object> response = new HashMap<>();

        try {
            String sql = """
                SELECT
                    u.IDUSUARIO,
                    u.IDEMPLEADO,
                    u.IDTIPO_USUARIO,
                    u.LOGEO,
                    u.ESTADO,
                    p.NOMBRES,
                    p.APEPATERNO,
                    p.APEMATERNO,
                    t.NOMUSUARIO
                FROM USUARIO u
                INNER JOIN EMPLEADO e     ON u.IDEMPLEADO     = e.IDEMPLEADO
                INNER JOIN PERSONA  p     ON e.IDPERSONA      = p.IDPERSONA
                INNER JOIN TIPO_USUARIO t ON u.IDTIPO_USUARIO = t.IDTIPO_USUARIO
                WHERE u.LOGEO = ? AND u.CLAVE = ? AND u.ESTADO = '1'
                """;

            List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql,
                    request.getLogeo(), request.getClave());

            if (rows.isEmpty()) {
                response.put("success", false);
                response.put("mensaje", "Credenciales incorrectas o usuario inactivo");
                return ResponseEntity.status(401).body(response);
            }

            Map<String, Object> user = rows.get(0);
            response.put("success", true);
            response.put("mensaje", "Login exitoso");
            response.put("idusuario",      user.get("IDUSUARIO"));
            response.put("idempleado",     user.get("IDEMPLEADO"));
            response.put("idtipo_usuario", user.get("IDTIPO_USUARIO"));
            response.put("logeo",          user.get("LOGEO"));
            response.put("nombres",        user.get("NOMBRES"));
            response.put("apepaterno",     user.get("APEPATERNO"));
            response.put("apematerno",     user.get("APEMATERNO"));
            response.put("nomusuario",     user.get("NOMUSUARIO"));
            response.put("estado",         user.get("ESTADO"));

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("mensaje", "Error en el servidor: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    // Endpoint de salud para verificar que el backend está activo
    @GetMapping("/ping")
    public ResponseEntity<Map<String, Object>> ping() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "ok");
        response.put("mensaje", "CastillonV2 API activa");
        return ResponseEntity.ok(response);
    }
}
