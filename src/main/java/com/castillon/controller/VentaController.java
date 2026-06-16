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
public class VentaController {

    @Autowired
    private JdbcTemplate db;

    // GET /api/metodos-pago
    @GetMapping("/api/metodos-pago")
    public ResponseEntity<?> getMetodosPago() {
        try {
            return ResponseEntity.ok(db.queryForList(
                "SELECT IDMETODO_PAGO as idMetodoPago, NOMMETODO as nomMetodo FROM METODO_PAGO ORDER BY NOMMETODO"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    // GET /api/ventas
    @GetMapping("/api/ventas")
    public ResponseEntity<?> listar() {
        try {
            String sql = """
                SELECT v.IDVENTA        as idVenta,
                       v.DOCUMENTO      as documento,
                       v.FECHAVENTA     as fechaVenta,
                       v.MONTOTOTAL     as montoTotal,
                       v.DESCUENTO      as descuento,
                       v.SUBTOTOTAL     as subTototal,
                       v.IGV            as igv,
                       v.TOTALPAGAR     as totalPagar,
                       v.ESTADO         as estado,
                       v.IDCLIENTE      as idCliente,
                       v.IDMETODO_PAGO  as idMetodoPago,
                       v.IDUSUARIO      as idUsuario,
                       CONCAT(p.NOMBRES,' ',p.APEPATERNO) as clienteNombre,
                       mp.NOMMETODO     as metodoPago
                FROM VENTA v
                INNER JOIN CLIENTE    c  ON v.IDCLIENTE     = c.IDCLIENTE
                INNER JOIN PERSONA    p  ON c.IDPERSONA     = p.IDPERSONA
                LEFT  JOIN METODO_PAGO mp ON v.IDMETODO_PAGO = mp.IDMETODO_PAGO
                ORDER BY v.FECHAVENTA DESC, v.IDVENTA DESC
                """;
            return ResponseEntity.ok(db.queryForList(sql));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    // POST /api/ventas  (incluye detalles)
    @SuppressWarnings("unchecked")
    @PostMapping("/api/ventas")
    public ResponseEntity<?> crear(@RequestBody Map<String, Object> body) {
        try {
            // 1. Insertar cabecera VENTA
            String sqlV = """
                INSERT INTO VENTA (IDCLIENTE, IDMETODO_PAGO, IDUSUARIO, DOCUMENTO,
                                   FECHAVENTA, MONTOTOTAL, DESCUENTO, SUBTOTOTAL,
                                   IGV, TOTALPAGAR, ESTADO)
                VALUES (?,?,?,?,?,?,?,?,?,?,?)
                """;
            KeyHolder kh = new GeneratedKeyHolder();
            db.update(con -> {
                PreparedStatement ps = con.prepareStatement(sqlV, Statement.RETURN_GENERATED_KEYS);
                ps.setObject(1, intOrNull(body,"idCliente"));
                ps.setObject(2, intOrNull(body,"idMetodoPago"));
                ps.setObject(3, intOrNull(body,"idUsuario"));
                ps.setString(4, str(body,"documento"));
                ps.setObject(5, body.get("fechaVenta"));
                ps.setObject(6, body.get("montoTotal"));
                ps.setObject(7, body.getOrDefault("descuento", 0));
                ps.setObject(8, body.get("subTototal"));
                ps.setObject(9, body.get("igv"));
                ps.setObject(10, body.get("totalPagar"));
                ps.setString(11, "A");   // Activa
                return ps;
            }, kh);
            int idVenta = kh.getKey().intValue();

            // 2. Insertar DETALLE_VENTA
            List<Map<String, Object>> detalles = (List<Map<String, Object>>) body.get("detalles");
            if (detalles != null) {
                String sqlD = "INSERT INTO DETALLE_VENTA (IDVENTA, IDPRODUCTO, CANTIDAD, PRECIO) VALUES (?,?,?,?)";
                for (Map<String, Object> d : detalles) {
                    db.update(sqlD, idVenta,
                        intOrNull(d,"idProducto"),
                        intOrNull(d,"cantidad"),
                        d.get("precio"));
                }
            }

            return ResponseEntity.ok(Map.of("success", true, "idVenta", idVenta));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    // PUT /api/ventas/{id}/anular
    @PutMapping("/api/ventas/{id}/anular")
    public ResponseEntity<?> anular(@PathVariable int id) {
        try {
            int rows = db.update("UPDATE VENTA SET ESTADO='X' WHERE IDVENTA=?", id);
            if (rows == 0) return ResponseEntity.status(404).body(Map.of("error","Venta no encontrada"));
            return ResponseEntity.ok(Map.of("success", true, "mensaje", "Venta anulada"));
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
}
