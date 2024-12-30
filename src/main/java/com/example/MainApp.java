package com.example;

import com.example.db.DatabaseManager;
import com.example.model.Product;
import com.example.model.Sale;
import com.example.util.ReportGenerator;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Random;

public class MainApp extends JFrame {
    private DatabaseManager dbManager;
    private JTable productTable;
    private DefaultTableModel productTableModel;
    private JTextField nameField, buyPriceField, sellPriceField, quantityField;
    private JTextField criticalLevelField, barcodeField, supplierField;
    private JButton addButton, updateButton, deleteButton, sellButton;
    private JButton reportButton, chartButton;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

    public MainApp() {
        dbManager = DatabaseManager.getInstance();
        initializeUI();
        loadProductData();
    }

    private void initializeUI() {
        setTitle("Stok Yönetim Sistemi");
        setSize(1200, 800);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // Ana panel
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Barkod arama paneli
        JPanel barcodePanel = createBarcodePanel();
        mainPanel.add(barcodePanel, BorderLayout.NORTH);

        // Üst panel - Giriş alanları
        JPanel inputPanel = createInputPanel();
        mainPanel.add(inputPanel, BorderLayout.CENTER);

        // Orta panel - Tablo
        JPanel tablePanel = createTablePanel();
        mainPanel.add(tablePanel, BorderLayout.CENTER);

        // Alt panel - Butonlar
        JPanel buttonPanel = createButtonPanel();
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        // Menü çubuğu
        setJMenuBar(createMenuBar());

        add(mainPanel);
    }

    private JPanel createBarcodePanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panel.setBorder(BorderFactory.createTitledBorder("Barkod Okuyucu"));

        JTextField barcodeSearchField = new JTextField(20);
        barcodeSearchField.setFont(new Font("Arial", Font.BOLD, 16));
        barcodeSearchField.addActionListener(e -> searchByBarcode(barcodeSearchField.getText()));

        panel.add(new JLabel("Barkod: "));
        panel.add(barcodeSearchField);

        return panel;
    }

    private void searchByBarcode(String barcode) {
        try {
            for (int i = 0; i < productTableModel.getRowCount(); i++) {
                String productBarcode = (String) productTableModel.getValueAt(i, 6); // Barkod sütunu
                if (barcode.equals(productBarcode)) {
                    // Ürünü seç ve görüntüle
                    productTable.setRowSelectionInterval(i, i);
                    productTable.scrollRectToVisible(productTable.getCellRect(i, 0, true));
                    loadSelectedProduct();
                    return;
                }
            }
            showError("Barkod bulunamadı: " + barcode);
        } catch (Exception ex) {
            showError("Barkod arama hatası: " + ex.getMessage());
        }
    }

    private JPanel createInputPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Ürün Bilgileri"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);

        // İlk satır
        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(new JLabel("Ürün Adı:"), gbc);
        gbc.gridx = 1;
        nameField = new JTextField(15);
        panel.add(nameField, gbc);

        gbc.gridx = 2;
        panel.add(new JLabel("Alış Fiyatı:"), gbc);
        gbc.gridx = 3;
        buyPriceField = new JTextField(10);
        panel.add(buyPriceField, gbc);

        gbc.gridx = 4;
        panel.add(new JLabel("Satış Fiyatı:"), gbc);
        gbc.gridx = 5;
        sellPriceField = new JTextField(10);
        panel.add(sellPriceField, gbc);

        // İkinci satır
        gbc.gridx = 0; gbc.gridy = 1;
        panel.add(new JLabel("Miktar:"), gbc);
        gbc.gridx = 1;
        quantityField = new JTextField(10);
        panel.add(quantityField, gbc);

        gbc.gridx = 2;
        panel.add(new JLabel("Kritik Seviye:"), gbc);
        gbc.gridx = 3;
        criticalLevelField = new JTextField(10);
        panel.add(criticalLevelField, gbc);

        gbc.gridx = 4;
        panel.add(new JLabel("Barkod:"), gbc);
        gbc.gridx = 5;
        barcodeField = new JTextField(15);
        panel.add(barcodeField, gbc);

        // Üçüncü satır
        gbc.gridx = 0; gbc.gridy = 2;
        panel.add(new JLabel("Tedarikçi:"), gbc);
        gbc.gridx = 1;
        supplierField = new JTextField(20);
        panel.add(supplierField, gbc);

        return panel;
    }

    private JPanel createTablePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Ürün Listesi"));

        String[] columns = {
            "ID", "Ürün Adı", "Alış Fiyatı", "Satış Fiyatı", 
            "Miktar", "Kritik Seviye", "Barkod", "Tedarikçi"
        };
        productTableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        productTable = new JTable(productTableModel);
        productTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        productTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                loadSelectedProduct();
            }
        });

        JScrollPane scrollPane = new JScrollPane(productTable);
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createButtonPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));

        addButton = new JButton("Ekle");
        addButton.addActionListener(this::addProduct);
        panel.add(addButton);

        updateButton = new JButton("Güncelle");
        updateButton.addActionListener(this::updateProduct);
        panel.add(updateButton);

        deleteButton = new JButton("Sil");
        deleteButton.addActionListener(this::deleteProduct);
        panel.add(deleteButton);

        sellButton = new JButton("Satış Yap");
        sellButton.addActionListener(this::makeSale);
        panel.add(sellButton);

        reportButton = new JButton("Rapor Oluştur");
        reportButton.addActionListener(this::generateReport);
        panel.add(reportButton);

        chartButton = new JButton("Grafik Göster");
        chartButton.addActionListener(this::showChart);
        panel.add(chartButton);

        return panel;
    }

    private JMenuBar createMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        
        JMenu fileMenu = new JMenu("Dosya");
        JMenuItem exitItem = new JMenuItem("Çıkış");
        exitItem.addActionListener(e -> System.exit(0));
        fileMenu.add(exitItem);
        
        JMenu reportMenu = new JMenu("Raporlar");
        JMenuItem salesReportItem = new JMenuItem("Satış Raporu");
        salesReportItem.addActionListener(this::generateSalesReport);
        JMenuItem stockReportItem = new JMenuItem("Stok Raporu");
        stockReportItem.addActionListener(this::generateStockReport);
        reportMenu.add(salesReportItem);
        reportMenu.add(stockReportItem);
        
        menuBar.add(fileMenu);
        menuBar.add(reportMenu);
        
        return menuBar;
    }

    private void loadProductData() {
        try {
            List<Product> products = dbManager.getAllProducts();
            productTableModel.setRowCount(0);
            for (Product product : products) {
                Object[] row = {
                    product.getId(),
                    product.getName(),
                    product.getBuyPrice(),
                    product.getSellPrice(),
                    product.getQuantity(),
                    product.getCriticalLevel(),
                    product.getBarcode(),
                    product.getSupplier()
                };
                productTableModel.addRow(row);
            }
        } catch (SQLException e) {
            showError("Ürünler yüklenirken hata oluştu: " + e.getMessage());
        }
    }

    private void loadSelectedProduct() {
        int selectedRow = productTable.getSelectedRow();
        if (selectedRow >= 0) {
            nameField.setText(productTableModel.getValueAt(selectedRow, 1).toString());
            buyPriceField.setText(productTableModel.getValueAt(selectedRow, 2).toString());
            sellPriceField.setText(productTableModel.getValueAt(selectedRow, 3).toString());
            quantityField.setText(productTableModel.getValueAt(selectedRow, 4).toString());
            criticalLevelField.setText(productTableModel.getValueAt(selectedRow, 5).toString());
            barcodeField.setText(productTableModel.getValueAt(selectedRow, 6).toString());
            supplierField.setText(productTableModel.getValueAt(selectedRow, 7).toString());
        }
    }

    private void clearFields() {
        nameField.setText("");
        buyPriceField.setText("");
        sellPriceField.setText("");
        quantityField.setText("");
        criticalLevelField.setText("");
        barcodeField.setText("");
        supplierField.setText("");
        productTable.clearSelection();
    }

    private void addProduct(ActionEvent e) {
        try {
            Product product = new Product();
            product.setName(nameField.getText());
            product.setBuyPrice(Double.parseDouble(buyPriceField.getText()));
            product.setSellPrice(Double.parseDouble(sellPriceField.getText()));
            product.setQuantity(Integer.parseInt(quantityField.getText()));
            product.setCriticalLevel(Integer.parseInt(criticalLevelField.getText()));
            
            // Barkod oluştur
            if (barcodeField.getText().isEmpty()) {
                String barcode = generateBarcode(product);
                product.setBarcode(barcode);
                barcodeField.setText(barcode);
            } else {
                product.setBarcode(barcodeField.getText());
            }
            
            product.setSupplier(supplierField.getText());

            dbManager.addProduct(product);
            loadProductData();
            clearFields();
            showInfo("Ürün başarıyla eklendi. Barkod: " + product.getBarcode());
        } catch (NumberFormatException ex) {
            showError("Lütfen sayısal değerleri doğru formatta girin.");
        } catch (SQLException ex) {
            showError("Ürün eklenirken hata oluştu: " + ex.getMessage());
        }
    }

    private String generateBarcode(Product product) {
        // Barkod formatı: PRD-YYYYMMDD-XXXX
        // PRD: Ürün prefixi
        // YYYYMMDD: Tarih
        // XXXX: Rastgele sayı
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
        String date = dateFormat.format(new Date());
        String random = String.format("%04d", new Random().nextInt(10000));
        return "PRD-" + date + "-" + random;
    }

    private void updateProduct(ActionEvent e) {
        int selectedRow = productTable.getSelectedRow();
        if (selectedRow < 0) {
            showError("Lütfen güncellenecek ürünü seçin.");
            return;
        }

        try {
            Product product = new Product();
            product.setId((Integer) productTableModel.getValueAt(selectedRow, 0));
            product.setName(nameField.getText());
            product.setBuyPrice(Double.parseDouble(buyPriceField.getText()));
            product.setSellPrice(Double.parseDouble(sellPriceField.getText()));
            product.setQuantity(Integer.parseInt(quantityField.getText()));
            product.setCriticalLevel(Integer.parseInt(criticalLevelField.getText()));
            product.setBarcode(barcodeField.getText());
            product.setSupplier(supplierField.getText());

            dbManager.updateProduct(product);
            loadProductData();
            clearFields();
            showInfo("Ürün başarıyla güncellendi.");
        } catch (NumberFormatException ex) {
            showError("Lütfen sayısal değerleri doğru formatta girin.");
        } catch (SQLException ex) {
            showError("Ürün güncellenirken hata oluştu: " + ex.getMessage());
        }
    }

    private void deleteProduct(ActionEvent e) {
        int selectedRow = productTable.getSelectedRow();
        if (selectedRow < 0) {
            showError("Lütfen silinecek ürünü seçin.");
            return;
        }

        int response = JOptionPane.showConfirmDialog(
            this,
            "Seçili ürünü silmek istediğinizden emin misiniz?",
            "Ürün Silme",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE
        );

        if (response == JOptionPane.YES_OPTION) {
            try {
                int productId = (Integer) productTableModel.getValueAt(selectedRow, 0);
                dbManager.deleteProduct(productId);
                loadProductData();
                clearFields();
                showInfo("Ürün başarıyla silindi.");
            } catch (SQLException ex) {
                showError("Ürün silinirken hata oluştu: " + ex.getMessage());
            }
        }
    }

    private void makeSale(ActionEvent e) {
        int selectedRow = productTable.getSelectedRow();
        if (selectedRow < 0) {
            showError("Lütfen satılacak ürünü seçin.");
            return;
        }

        try {
            String quantityStr = JOptionPane.showInputDialog(
                this,
                "Satış miktarını girin:",
                "Satış İşlemi",
                JOptionPane.QUESTION_MESSAGE
            );

            if (quantityStr != null && !quantityStr.trim().isEmpty()) {
                int saleQuantity = Integer.parseInt(quantityStr);
                int currentQuantity = (Integer) productTableModel.getValueAt(selectedRow, 4);

                if (saleQuantity <= 0) {
                    showError("Satış miktarı 0'dan büyük olmalıdır.");
                    return;
                }

                if (saleQuantity > currentQuantity) {
                    showError("Stokta yeterli ürün yok.");
                    return;
                }

                int productId = (Integer) productTableModel.getValueAt(selectedRow, 0);
                String productName = (String) productTableModel.getValueAt(selectedRow, 1);
                double buyPrice = (Double) productTableModel.getValueAt(selectedRow, 2);
                double sellPrice = (Double) productTableModel.getValueAt(selectedRow, 3);

                Sale sale = new Sale(
                    productId,
                    productName,
                    saleQuantity,
                    sellPrice,
                    buyPrice
                );

                dbManager.addSale(sale);
                loadProductData();
                showInfo("Satış başarıyla kaydedildi.");
            }
        } catch (NumberFormatException ex) {
            showError("Lütfen geçerli bir sayı girin.");
        } catch (SQLException ex) {
            showError("Satış işlemi sırasında hata oluştu: " + ex.getMessage());
        }
    }

    private void generateReport(ActionEvent e) {
        try {
            String startDateStr = JOptionPane.showInputDialog(
                this,
                "Başlangıç tarihini girin (yyyy-MM-dd):",
                dateFormat.format(new Date())
            );

            String endDateStr = JOptionPane.showInputDialog(
                this,
                "Bitiş tarihini girin (yyyy-MM-dd):",
                dateFormat.format(new Date())
            );

            if (startDateStr != null && endDateStr != null) {
                Date startDate = dateFormat.parse(startDateStr);
                Date endDate = dateFormat.parse(endDateStr);

                List<Sale> sales = dbManager.getSalesByDateRange(startDate, endDate);
                ReportGenerator.generateSalesReport(sales, "sales_report.xlsx");
                showInfo("Rapor başarıyla oluşturuldu: sales_report.xlsx");
            }
        } catch (ParseException ex) {
            showError("Tarih formatı hatalı. Lütfen yyyy-MM-dd formatında girin.");
        } catch (SQLException ex) {
            showError("Rapor oluşturulurken hata oluştu: " + ex.getMessage());
        }
    }

    private void generateSalesReport(ActionEvent e) {
        try {
            String startDateStr = JOptionPane.showInputDialog(
                this,
                "Başlangıç tarihini girin (yyyy-MM-dd):",
                dateFormat.format(new Date())
            );

            String endDateStr = JOptionPane.showInputDialog(
                this,
                "Bitiş tarihini girin (yyyy-MM-dd):",
                dateFormat.format(new Date())
            );

            if (startDateStr != null && endDateStr != null) {
                Date startDate = dateFormat.parse(startDateStr);
                Date endDate = dateFormat.parse(endDateStr);

                List<Sale> sales = dbManager.getSalesByDateRange(startDate, endDate);
                ReportGenerator.generateSalesReport(sales, "sales_report.xlsx");
                showInfo("Satış raporu başarıyla oluşturuldu: sales_report.xlsx");
            }
        } catch (ParseException ex) {
            showError("Tarih formatı hatalı. Lütfen yyyy-MM-dd formatında girin.");
        } catch (SQLException ex) {
            showError("Rapor oluşturulurken hata oluştu: " + ex.getMessage());
        }
    }

    private void generateStockReport(ActionEvent e) {
        try {
            List<Product> products = dbManager.getAllProducts();
            ReportGenerator.generateStockReport(products, "stock_report.xlsx");
            showInfo("Stok raporu başarıyla oluşturuldu: stock_report.xlsx");
        } catch (SQLException ex) {
            showError("Rapor oluşturulurken hata oluştu: " + ex.getMessage());
        }
    }

    private void showChart(ActionEvent e) {
        try {
            List<Sale> sales = dbManager.getSalesByDateRange(
                dateFormat.parse(dateFormat.format(new Date())),
                new Date()
            );
            ReportGenerator.createSalesChart(sales).setVisible(true);
        } catch (SQLException | ParseException ex) {
            showError("Grafik oluşturulurken hata oluştu: " + ex.getMessage());
        }
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(
            this,
            message,
            "Hata",
            JOptionPane.ERROR_MESSAGE
        );
    }

    private void showInfo(String message) {
        JOptionPane.showMessageDialog(
            this,
            message,
            "Bilgi",
            JOptionPane.INFORMATION_MESSAGE
        );
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        SwingUtilities.invokeLater(() -> {
            MainApp app = new MainApp();
            app.setVisible(true);
        });
    }
} 