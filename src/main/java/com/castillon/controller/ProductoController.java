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
public class ProductoController {

    @Autowired
    private JdbcTemplate db;

    // GET /api/categorias
    @GetMapping("/api/categorias")
    public ResponseEntity<?> getCategorias() {
        try {
            return ResponseEntity.ok(db.queryForList(
                "SELECT IDCATEGORIA as idCategoria, NOMCATEGORIA as nomCategoria FROM CATEGORIA ORDER BY NOMCATEGORIA"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    // GET /api/productos
    @GetMapping("/api/productos")
    public ResponseEntity<?> listar() {
        try {
            String sql = """
                SELECT pr.IDPRODUCTO   as idProducto,
                       pr.NOMPRODUCTO  as nomProducto,
                       pr.DESCRIPCION  as descripcion,
                       pr.MARCA        as marca,
                       pr.PRECIO       as precio,
                       pr.ESTADO       as estado,
                       pr.IDCATEGORIA  as idCategoria,
                       c.NOMCATEGORIA  as categoria
                FROM PRODUCTO pr
                LEFT JOIN CATEGORIA c ON pr.IDCATEGORIA = c.IDCATEGORIA
                ORDER BY pr.NOMPRODUCTO
                """;
            List<Map<String, Object>> rows = db.queryForList(sql);
            rows.forEach(r -> {
                Object est = r.get("estado");
                String s = String.valueOf(est);
                if ("1".equals(s)) r.put("estado", "A");
                else if ("0".equals(s)) r.put("estado", "I");
            });
            return ResponseEntity.ok(rows);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    // POST /api/productos
    @PostMapping("/api/productos")
    public ResponseEntity<?> crear(@RequestBody Map<String, Object> body) {
        try {
            String sql = "INSERT INTO PRODUCTO (NOMPRODUCTO, DESCRIPCION, IDCATEGORIA, MARCA, PRECIO, ESTADO) VALUES (?,?,?,?,?,?)";
            KeyHolder kh = new GeneratedKeyHolder();
            db.update(con -> {
                PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
                ps.setString(1, str(body,"nomProducto"));
                ps.setString(2, str(body,"descripcion"));
                ps.setObject(3, intOrNull(body,"idCategoria"));
                ps.setString(4, str(body,"marca"));
                ps.setObject(5, body.get("precio"));
                ps.setString(6, estadoBD(str(body,"estado")));
                return ps;
            }, kh);
            return ResponseEntity.ok(Map.of("success", true, "idProducto", kh.getKey().intValue()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    // PUT /api/productos/{id}
    @PutMapping("/api/productos/{id}")
    public ResponseEntity<?> actualizar(@PathVariable int id, @RequestBody Map<String, Object> body) {
        try {
            int rows = db.update(
                "UPDATE PRODUCTO SET NOMPRODUCTO=?,DESCRIPCION=?,IDCATEGORIA=?,MARCA=?,PRECIO=?,ESTADO=? WHERE IDPRODUCTO=?",
                str(body,"nomProducto"), str(body,"descripcion"),
                intOrNull(body,"idCategoria"), str(body,"marca"),
                body.get("precio"), estadoBD(str(body,"estado")), id);
            if (rows == 0) return ResponseEntity.status(404).body(Map.of("error","No encontrado"));
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    // DELETE /api/productos/{id}
    @DeleteMapping("/api/productos/{id}")
    public ResponseEntity<?> eliminar(@PathVariable int id) {
        try {
            int rows = db.update("DELETE FROM PRODUCTO WHERE IDPRODUCTO=?", id);
            if (rows == 0) return ResponseEntity.status(404).body(Map.of("error","No encontrado"));
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
