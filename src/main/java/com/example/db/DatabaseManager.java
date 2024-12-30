package com.example.db;

import com.example.model.Product;
import com.example.model.Sale;

import java.sql.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class DatabaseManager {
    private static final String DB_URL = "jdbc:sqlite:stock_management.db";
    private static DatabaseManager instance;
    private Connection connection;

    private DatabaseManager() {
        initializeDatabase();
    }

    public static DatabaseManager getInstance() {
        if (instance == null) {
            instance = new DatabaseManager();
        }
        return instance;
    }

    private void initializeDatabase() {
        try {
            connection = DriverManager.getConnection(DB_URL);
            createTables();
        } catch (SQLException e) {
            e.printStackTrace();
            System.err.println("Veritabanı bağlantısı kurulamadı: " + e.getMessage());
        }
    }

    private void createTables() {
        try (Statement statement = connection.createStatement()) {
            // Ürünler tablosu
            statement.execute("""
                CREATE TABLE IF NOT EXISTS products (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    name TEXT NOT NULL,
                    buy_price REAL NOT NULL,
                    sell_price REAL NOT NULL,
                    quantity INTEGER NOT NULL,
                    critical_level INTEGER NOT NULL,
                    barcode TEXT,
                    supplier TEXT
                )
            """);

            // Satışlar tablosu
            statement.execute("""
                CREATE TABLE IF NOT EXISTS sales (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    product_id INTEGER NOT NULL,
                    product_name TEXT NOT NULL,
                    quantity INTEGER NOT NULL,
                    unit_price REAL NOT NULL,
                    total_amount REAL NOT NULL,
                    sale_date TIMESTAMP NOT NULL,
                    profit REAL NOT NULL,
                    FOREIGN KEY (product_id) REFERENCES products(id)
                )
            """);
        } catch (SQLException e) {
            e.printStackTrace();
            System.err.println("Tablolar oluşturulurken hata: " + e.getMessage());
        }
    }

    // Ürün işlemleri
    public void addProduct(Product product) throws SQLException {
        // Barkod kontrolü
        if (isBarcodeExists(product.getBarcode())) {
            throw new SQLException("Bu barkod zaten kullanımda: " + product.getBarcode());
        }

        String sql = """
            INSERT INTO products (name, buy_price, sell_price, quantity, critical_level, barcode, supplier)
            VALUES (?, ?, ?, ?, ?, ?, ?)
        """;
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, product.getName());
            pstmt.setDouble(2, product.getBuyPrice());
            pstmt.setDouble(3, product.getSellPrice());
            pstmt.setInt(4, product.getQuantity());
            pstmt.setInt(5, product.getCriticalLevel());
            pstmt.setString(6, product.getBarcode());
            pstmt.setString(7, product.getSupplier());
            
            pstmt.executeUpdate();
            
            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    product.setId(generatedKeys.getInt(1));
                }
            }
        }
    }

    public void updateProduct(Product product) throws SQLException {
        // Barkod kontrolü (kendi ID'si hariç)
        if (isBarcodeExistsExcept(product.getBarcode(), product.getId())) {
            throw new SQLException("Bu barkod başka bir ürün tarafından kullanılıyor: " + product.getBarcode());
        }

        String sql = """
            UPDATE products 
            SET name = ?, buy_price = ?, sell_price = ?, quantity = ?, 
                critical_level = ?, barcode = ?, supplier = ?
            WHERE id = ?
        """;
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, product.getName());
            pstmt.setDouble(2, product.getBuyPrice());
            pstmt.setDouble(3, product.getSellPrice());
            pstmt.setInt(4, product.getQuantity());
            pstmt.setInt(5, product.getCriticalLevel());
            pstmt.setString(6, product.getBarcode());
            pstmt.setString(7, product.getSupplier());
            pstmt.setInt(8, product.getId());
            
            pstmt.executeUpdate();
        }
    }

    public void deleteProduct(int id) throws SQLException {
        String sql = "DELETE FROM products WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            pstmt.executeUpdate();
        }
    }

    public Product getProduct(int id) throws SQLException {
        String sql = "SELECT * FROM products WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return extractProductFromResultSet(rs);
            }
        }
        return null;
    }

    public List<Product> getAllProducts() throws SQLException {
        List<Product> products = new ArrayList<>();
        String sql = "SELECT * FROM products";
        
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                products.add(extractProductFromResultSet(rs));
            }
        }
        return products;
    }

    public List<Product> getLowStockProducts() throws SQLException {
        List<Product> products = new ArrayList<>();
        String sql = "SELECT * FROM products WHERE quantity <= critical_level";
        
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                products.add(extractProductFromResultSet(rs));
            }
        }
        return products;
    }

    // Satış işlemleri
    public void addSale(Sale sale) throws SQLException {
        String sql = """
            INSERT INTO sales (product_id, product_name, quantity, unit_price, 
                             total_amount, sale_date, profit)
            VALUES (?, ?, ?, ?, ?, ?, ?)
        """;
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setInt(1, sale.getProductId());
            pstmt.setString(2, sale.getProductName());
            pstmt.setInt(3, sale.getQuantity());
            pstmt.setDouble(4, sale.getUnitPrice());
            pstmt.setDouble(5, sale.getTotalAmount());
            pstmt.setTimestamp(6, new Timestamp(sale.getSaleDate().getTime()));
            pstmt.setDouble(7, sale.getProfit());
            
            pstmt.executeUpdate();
            
            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    sale.setId(generatedKeys.getInt(1));
                }
            }
            
            // Stok miktarını güncelle
            updateProductQuantity(sale.getProductId(), sale.getQuantity());
        }
    }

    private void updateProductQuantity(int productId, int soldQuantity) throws SQLException {
        String sql = "UPDATE products SET quantity = quantity - ? WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, soldQuantity);
            pstmt.setInt(2, productId);
            pstmt.executeUpdate();
        }
    }

    public List<Sale> getSalesByDateRange(Date startDate, Date endDate) throws SQLException {
        List<Sale> sales = new ArrayList<>();
        String sql = "SELECT * FROM sales WHERE sale_date BETWEEN ? AND ?";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setTimestamp(1, new Timestamp(startDate.getTime()));
            pstmt.setTimestamp(2, new Timestamp(endDate.getTime()));
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    sales.add(extractSaleFromResultSet(rs));
                }
            }
        }
        return sales;
    }

    public double getTotalRevenue() throws SQLException {
        String sql = "SELECT SUM(total_amount) as total FROM sales";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            if (rs.next()) {
                return rs.getDouble("total");
            }
        }
        return 0.0;
    }

    public double getTotalProfit() throws SQLException {
        String sql = "SELECT SUM(profit) as total FROM sales";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            if (rs.next()) {
                return rs.getDouble("total");
            }
        }
        return 0.0;
    }

    public Product findByBarcode(String barcode) throws SQLException {
        String sql = "SELECT * FROM products WHERE barcode = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, barcode);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return extractProductFromResultSet(rs);
            }
        }
        return null;
    }

    private boolean isBarcodeExists(String barcode) throws SQLException {
        if (barcode == null || barcode.trim().isEmpty()) {
            return false;
        }

        String sql = "SELECT COUNT(*) FROM products WHERE barcode = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, barcode);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        }
        return false;
    }

    private boolean isBarcodeExistsExcept(String barcode, int productId) throws SQLException {
        if (barcode == null || barcode.trim().isEmpty()) {
            return false;
        }

        String sql = "SELECT COUNT(*) FROM products WHERE barcode = ? AND id != ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, barcode);
            pstmt.setInt(2, productId);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        }
        return false;
    }

    private Product extractProductFromResultSet(ResultSet rs) throws SQLException {
        Product product = new Product();
        product.setId(rs.getInt("id"));
        product.setName(rs.getString("name"));
        product.setBuyPrice(rs.getDouble("buy_price"));
        product.setSellPrice(rs.getDouble("sell_price"));
        product.setQuantity(rs.getInt("quantity"));
        product.setCriticalLevel(rs.getInt("critical_level"));
        product.setBarcode(rs.getString("barcode"));
        product.setSupplier(rs.getString("supplier"));
        return product;
    }

    private Sale extractSaleFromResultSet(ResultSet rs) throws SQLException {
        Sale sale = new Sale();
        sale.setId(rs.getInt("id"));
        sale.setProductId(rs.getInt("product_id"));
        sale.setProductName(rs.getString("product_name"));
        sale.setQuantity(rs.getInt("quantity"));
        sale.setUnitPrice(rs.getDouble("unit_price"));
        sale.setTotalAmount(rs.getDouble("total_amount"));
        sale.setSaleDate(new Date(rs.getTimestamp("sale_date").getTime()));
        sale.setProfit(rs.getDouble("profit"));
        return sale;
    }
} 