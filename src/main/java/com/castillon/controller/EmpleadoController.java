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
public class EmpleadoController {

    @Autowired
    private JdbcTemplate db;

    // GET /api/cargos
    @GetMapping("/api/cargos")
    public ResponseEntity<?> getCargos() {
        try {
            return ResponseEntity.ok(db.queryForList(
                "SELECT IDCARGO as idCargo, NOMCARGO as nomCargo FROM CARGO ORDER BY NOMCARGO"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    // GET /api/contratos
    @GetMapping("/api/contratos")
    public ResponseEntity<?> getContratos() {
        try {
            return ResponseEntity.ok(db.queryForList(
                "SELECT IDCONTRATO as idContrato, NOMCONTRATO as nomContrato FROM CONTRATO ORDER BY NOMCONTRATO"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    // GET /api/empleados
    @GetMapping("/api/empleados")
    public ResponseEntity<?> listar() {
        try {
            String sql = """
                SELECT e.IDEMPLEADO   as idEmpleado,
                       p.NOMBRES      as nombres,
                       p.APEPATERNO   as apePaterno,
                       p.APEMATERNO   as apeMaterno,
                       p.DNI          as dni,
                       p.CELULAR      as celular,
                       p.CORREO       as correo,
                       e.IDCARGO      as idCargo,
                       c.NOMCARGO     as cargo,
                       e.IDCONTRATO   as idContrato,
                       ct.NOMCONTRATO as contrato,
                       e.SALARIO      as salario,
                       e.TURNO        as turno,
                       e.ESTADO       as estado
                FROM EMPLEADO e
                INNER JOIN PERSONA  p  ON e.IDPERSONA   = p.IDPERSONA
                LEFT  JOIN CARGO    c  ON e.IDCARGO     = c.IDCARGO
                LEFT  JOIN CONTRATO ct ON e.IDCONTRATO  = ct.IDCONTRATO
                ORDER BY p.APEPATERNO, p.NOMBRES
                """;
            List<Map<String, Object>> rows = db.queryForList(sql);
            rows.forEach(r -> {
                Object est = r.get("estado");
                if ("1".equals(String.valueOf(est))) r.put("estado", "A");
                else r.put("estado", "I");
            });
            return ResponseEntity.ok(rows);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    // POST /api/empleados
    @PostMapping("/api/empleados")
    public ResponseEntity<?> crear(@RequestBody Map<String, Object> body) {
        try {
            // 1. Insertar PERSONA
            String sqlP = "INSERT INTO PERSONA (NOMBRES, APEPATERNO, APEMATERNO, DNI, CELULAR, CORREO) VALUES (?,?,?,?,?,?)";
            KeyHolder kh = new GeneratedKeyHolder();
            db.update(con -> {
                PreparedStatement ps = con.prepareStatement(sqlP, Statement.RETURN_GENERATED_KEYS);
                ps.setString(1, str(body,"nombres"));
                ps.setString(2, str(body,"apePaterno"));
                ps.setString(3, str(body,"apeMaterno"));
                ps.setString(4, str(body,"dni"));
                ps.setString(5, str(body,"celular"));
                ps.setString(6, str(body,"correo"));
                return ps;
            }, kh);
            int idPersona = kh.getKey().intValue();

            // 2. Insertar EMPLEADO
            String sqlE = "INSERT INTO EMPLEADO (IDPERSONA, IDCARGO, IDCONTRATO, SALARIO, TURNO, ESTADO) VALUES (?,?,?,?,?,?)";
            KeyHolder kh2 = new GeneratedKeyHolder();
            db.update(con -> {
                PreparedStatement ps = con.prepareStatement(sqlE, Statement.RETURN_GENERATED_KEYS);
                ps.setInt(1, idPersona);
                ps.setObject(2, intOrNull(body,"idCargo"));
                ps.setObject(3, intOrNull(body,"idContrato"));
                ps.setObject(4, body.get("salario"));
                ps.setString(5, str(body,"turno"));
                ps.setString(6, estadoBD(str(body,"estado")));
                return ps;
            }, kh2);

            return ResponseEntity.ok(Map.of("success", true, "idEmpleado", kh2.getKey().intValue()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    // PUT /api/empleados/{id}
    @PutMapping("/api/empleados/{id}")
    public ResponseEntity<?> actualizar(@PathVariable int id, @RequestBody Map<String, Object> body) {
        try {
            Integer idPersona = db.queryForObject(
                "SELECT IDPERSONA FROM EMPLEADO WHERE IDEMPLEADO=?", Integer.class, id);
            if (idPersona == null) return ResponseEntity.status(404).body(Map.of("error","No encontrado"));

            db.update("UPDATE PERSONA SET NOMBRES=?,APEPATERNO=?,APEMATERNO=?,DNI=?,CELULAR=?,CORREO=? WHERE IDPERSONA=?",
                str(body,"nombres"), str(body,"apePaterno"), str(body,"apeMaterno"),
                str(body,"dni"), str(body,"celular"), str(body,"correo"), idPersona);

            db.update("UPDATE EMPLEADO SET IDCARGO=?,IDCONTRATO=?,SALARIO=?,TURNO=?,ESTADO=? WHERE IDEMPLEADO=?",
                intOrNull(body,"idCargo"), intOrNull(body,"idContrato"),
                body.get("salario"), str(body,"turno"), estadoBD(str(body,"estado")), id);

            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    // DELETE /api/empleados/{id}
    @DeleteMapping("/api/empleados/{id}")
    public ResponseEntity<?> eliminar(@PathVariable int id) {
        try {
            Integer idPersona = db.queryForObject(
                "SELECT IDPERSONA FROM EMPLEADO WHERE IDEMPLEADO=?", Integer.class, id);
            if (idPersona == null) return ResponseEntity.status(404).body(Map.of("error","No encontrado"));
            db.update("DELETE FROM EMPLEADO WHERE IDEMPLEADO=?", id);
            db.update("DELETE FROM PERSONA WHERE IDPERSONA=?", idPersona);
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    private String str(Map<String, Object> m, String k) {
        Object v = m.get(k); return v == null ? null : v.toString();
    }
    private Integer intOrNull(Map<String, Object> m, String k) {
        Object v = m.get(k);
        if (v == null || v.toString().isBlank()) return null;
        try { return Integer.parseInt(v.toString()); } catch (Exception e) { return null; }
    }
    private String estadoBD(String s) {
        if (s == null) return "1";
        return "A".equalsIgnoreCase(s) || "1".equals(s) ? "1" : "0";
    }
}
