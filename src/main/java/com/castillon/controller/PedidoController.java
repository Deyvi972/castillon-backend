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
public class PedidoController {

    @Autowired
    private JdbcTemplate db;

    // ─── MESAS ───────────────────────────────────

    // GET /api/mesas
    @GetMapping("/api/mesas")
    public ResponseEntity<?> getMesas() {
        try {
            String sql = "SELECT IDMESA as idMesa, NUNMESA as nunMesa, NUMMESA as numMesa, NUMPISO as numPiso, ESTADO as estado FROM MESA ORDER BY NUMPISO, NUNMESA";
            List<Map<String, Object>> rows = db.queryForList(sql);
            rows.forEach(r -> {
                // unificar nunMesa / numMesa para el frontend
                if (r.get("numMesa") == null) r.put("numMesa", r.get("nunMesa"));
                Object est = r.get("estado");
                if ("1".equals(String.valueOf(est))) r.put("estado", "A");
                else r.put("estado", "I");
            });
            return ResponseEntity.ok(rows);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    // POST /api/mesas
    @PostMapping("/api/mesas")
    public ResponseEntity<?> crearMesa(@RequestBody Map<String, Object> body) {
        try {
            Object numMesa = body.getOrDefault("nunMesa", body.get("numMesa"));
            Object numPiso = body.get("numPiso");
            String estado  = estadoBD(str(body,"estado"));

            String sql = "INSERT INTO MESA (NUNMESA, NUMPISO, ESTADO) VALUES (?,?,?)";
            KeyHolder kh = new GeneratedKeyHolder();
            db.update(con -> {
                PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
                ps.setObject(1, numMesa);
                ps.setObject(2, numPiso);
                ps.setString(3, estado);
                return ps;
            }, kh);
            return ResponseEntity.ok(Map.of("success", true, "idMesa", kh.getKey().intValue()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    // ─── PEDIDOS ─────────────────────────────────

    // GET /api/pedidos
    @GetMapping("/api/pedidos")
    public ResponseEntity<?> getPedidos() {
        try {
            String sql = """
                SELECT p.IDPEDIDO  as idPedido,
                       p.IDMESA    as idMesa,
                       p.FECHA     as fecha,
                       p.ESTADO    as estado,
                       m.NUNMESA   as nunMesa,
                       m.NUMMESA   as numMesa,
                       m.NUMPISO   as numPiso
                FROM PEDIDO p
                LEFT JOIN MESA m ON p.IDMESA = m.IDMESA
                ORDER BY p.FECHA DESC, p.IDPEDIDO DESC
                """;
            List<Map<String, Object>> rows = db.queryForList(sql);
            rows.forEach(r -> {
                if (r.get("numMesa") == null) r.put("numMesa", r.get("nunMesa"));
                // estado de pedido: A=Abierto, C=Cerrado (guardado como texto, no 0/1)
            });
            return ResponseEntity.ok(rows);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    // POST /api/pedidos
    @PostMapping("/api/pedidos")
    public ResponseEntity<?> crearPedido(@RequestBody Map<String, Object> body) {
        try {
            String sql = "INSERT INTO PEDIDO (IDMESA, FECHA, ESTADO) VALUES (?,?,?)";
            KeyHolder kh = new GeneratedKeyHolder();
            db.update(con -> {
                PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
                ps.setObject(1, intOrNull(body,"idMesa"));
                ps.setObject(2, body.get("fecha"));
                ps.setString(3, str(body,"estado") != null ? str(body,"estado") : "A");
                return ps;
            }, kh);
            return ResponseEntity.ok(Map.of("success", true, "idPedido", kh.getKey().intValue()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    // PUT /api/pedidos/{id}  (cierre de pedido u otro cambio de estado)
    @PutMapping("/api/pedidos/{id}")
    public ResponseEntity<?> actualizarPedido(@PathVariable int id, @RequestBody Map<String, Object> body) {
        try {
            String nuevoEstado = str(body,"estado");
            int rows = db.update("UPDATE PEDIDO SET ESTADO=? WHERE IDPEDIDO=?", nuevoEstado, id);
            if (rows == 0) return ResponseEntity.status(404).body(Map.of("error","Pedido no encontrado"));
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    // DELETE /api/pedidos/{id}
    @DeleteMapping("/api/pedidos/{id}")
    public ResponseEntity<?> eliminarPedido(@PathVariable int id) {
        try {
            db.update("DELETE FROM PEDIDO WHERE IDPEDIDO=?", id);
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
