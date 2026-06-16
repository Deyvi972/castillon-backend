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
@RequestMapping("/api/clientes")
@CrossOrigin(origins = "*")
public class ClienteController {

    @Autowired
    private JdbcTemplate db;

    // GET /api/clientes
    @GetMapping
    public ResponseEntity<?> listar() {
        try {
            String sql = """
                SELECT c.IDCLIENTE   as idCliente,
                       p.NOMBRES     as nombres,
                       p.APEPATERNO  as apePaterno,
                       p.APEMATERNO  as apeMaterno,
                       p.DNI         as dni,
                       p.CELULAR     as celular,
                       p.CORREO      as correo,
                       p.DIRECCION   as direccion,
                       p.FECNAC      as fecNac,
                       p.ESTCIVIL    as estCivil,
                       c.ESTADO      as estado
                FROM CLIENTE c
                INNER JOIN PERSONA p ON c.IDPERSONA = p.IDPERSONA
                ORDER BY p.APEPATERNO, p.NOMBRES
                """;
            return ResponseEntity.ok(db.queryForList(sql));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    // POST /api/clientes
    @PostMapping
    public ResponseEntity<?> crear(@RequestBody Map<String, Object> body) {
        try {
            // 1. Insertar PERSONA
            String sqlP = "INSERT INTO PERSONA (NOMBRES, APEPATERNO, APEMATERNO, DNI, CELULAR, CORREO, DIRECCION, FECNAC, ESTCIVIL) VALUES (?,?,?,?,?,?,?,?,?)";
            KeyHolder kh = new GeneratedKeyHolder();
            db.update(con -> {
                PreparedStatement ps = con.prepareStatement(sqlP, Statement.RETURN_GENERATED_KEYS);
                ps.setString(1, str(body, "nombres"));
                ps.setString(2, str(body, "apePaterno"));
                ps.setString(3, str(body, "apeMaterno"));
                ps.setString(4, str(body, "dni"));
                ps.setString(5, str(body, "celular"));
                ps.setString(6, str(body, "correo"));
                ps.setString(7, str(body, "direccion"));
                ps.setObject(8, body.get("fecNac"));
                ps.setString(9, str(body, "estCivil"));
                return ps;
            }, kh);
            int idPersona = kh.getKey().intValue();

            // 2. Insertar CLIENTE
            String sqlC = "INSERT INTO CLIENTE (IDPERSONA, ESTADO) VALUES (?, ?)";
            KeyHolder kh2 = new GeneratedKeyHolder();
            db.update(con -> {
                PreparedStatement ps = con.prepareStatement(sqlC, Statement.RETURN_GENERATED_KEYS);
                ps.setInt(1, idPersona);
                ps.setString(2, estadoBD(str(body, "estado")));
                return ps;
            }, kh2);

            return ResponseEntity.ok(Map.of("success", true, "idCliente", kh2.getKey().intValue()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    // PUT /api/clientes/{id}
    @PutMapping("/{id}")
    public ResponseEntity<?> actualizar(@PathVariable int id, @RequestBody Map<String, Object> body) {
        try {
            // Obtener idPersona del cliente
            Integer idPersona = db.queryForObject(
                "SELECT IDPERSONA FROM CLIENTE WHERE IDCLIENTE=?", Integer.class, id);
            if (idPersona == null) return ResponseEntity.status(404).body(Map.of("error", "Cliente no encontrado"));

            db.update("UPDATE PERSONA SET NOMBRES=?,APEPATERNO=?,APEMATERNO=?,DNI=?,CELULAR=?,CORREO=?,DIRECCION=?,FECNAC=?,ESTCIVIL=? WHERE IDPERSONA=?",
                str(body,"nombres"), str(body,"apePaterno"), str(body,"apeMaterno"),
                str(body,"dni"), str(body,"celular"), str(body,"correo"),
                str(body,"direccion"), body.get("fecNac"), str(body,"estCivil"), idPersona);

            db.update("UPDATE CLIENTE SET ESTADO=? WHERE IDCLIENTE=?",
                estadoBD(str(body,"estado")), id);

            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    // DELETE /api/clientes/{id}
    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminar(@PathVariable int id) {
        try {
            Integer idPersona = db.queryForObject(
                "SELECT IDPERSONA FROM CLIENTE WHERE IDCLIENTE=?", Integer.class, id);
            if (idPersona == null) return ResponseEntity.status(404).body(Map.of("error", "No encontrado"));
            db.update("DELETE FROM CLIENTE WHERE IDCLIENTE=?", id);
            db.update("DELETE FROM PERSONA WHERE IDPERSONA=?", idPersona);
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    private String str(Map<String, Object> m, String k) {
        Object v = m.get(k); return v == null ? null : v.toString();
    }
    private String estadoBD(String s) {
        if (s == null) return "1";
        return "A".equalsIgnoreCase(s) || "1".equals(s) ? "1" : "0";
    }
}
