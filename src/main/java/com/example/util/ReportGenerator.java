package com.example.util;

import com.example.model.Product;
import com.example.model.Sale;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.general.DefaultPieDataset;

import javax.swing.*;
import java.awt.*;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;

public class ReportGenerator {
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public static void generateSalesReport(List<Sale> sales, String filePath) {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Satış Raporu");

            // Başlık stilini oluştur
            CellStyle headerStyle = workbook.createCellStyle();
            org.apache.poi.ss.usermodel.Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);

            // Başlıkları oluştur
            Row headerRow = sheet.createRow(0);
            String[] columns = {
                "Satış ID", "Ürün ID", "Ürün Adı", "Miktar", 
                "Birim Fiyat", "Toplam Tutar", "Kâr", "Satış Tarihi"
            };

            for (int i = 0; i < columns.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(columns[i]);
                cell.setCellStyle(headerStyle);
                sheet.setColumnWidth(i, 15 * 256); // 15 karakter genişliğinde
            }

            // Verileri doldur
            int rowNum = 1;
            for (Sale sale : sales) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(sale.getId());
                row.createCell(1).setCellValue(sale.getProductId());
                row.createCell(2).setCellValue(sale.getProductName());
                row.createCell(3).setCellValue(sale.getQuantity());
                row.createCell(4).setCellValue(sale.getUnitPrice());
                row.createCell(5).setCellValue(sale.getTotalAmount());
                row.createCell(6).setCellValue(sale.getProfit());
                row.createCell(7).setCellValue(dateFormat.format(sale.getSaleDate()));
            }

            // Özet bilgileri ekle
            rowNum += 2;
            Row summaryRow = sheet.createRow(rowNum++);
            summaryRow.createCell(0).setCellValue("Toplam Satış Tutarı:");
            summaryRow.createCell(1).setCellValue(
                sales.stream()
                    .mapToDouble(Sale::getTotalAmount)
                    .sum()
            );

            Row profitRow = sheet.createRow(rowNum);
            profitRow.createCell(0).setCellValue("Toplam Kâr:");
            profitRow.createCell(1).setCellValue(
                sales.stream()
                    .mapToDouble(Sale::getProfit)
                    .sum()
            );

            // Dosyaya kaydet
            try (FileOutputStream fileOut = new FileOutputStream(filePath)) {
                workbook.write(fileOut);
            }

        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Rapor oluşturulurken hata: " + e.getMessage());
        }
    }

    public static void generateStockReport(List<Product> products, String filePath) {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Stok Raporu");

            // Başlık stilini oluştur
            CellStyle headerStyle = workbook.createCellStyle();
            org.apache.poi.ss.usermodel.Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);

            // Başlıkları oluştur
            Row headerRow = sheet.createRow(0);
            String[] columns = {
                "Ürün ID", "Ürün Adı", "Alış Fiyatı", "Satış Fiyatı",
                "Stok Miktarı", "Toplam Değer", "Kritik Seviye",
                "Barkod", "Tedarikçi", "Durum"
            };

            for (int i = 0; i < columns.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(columns[i]);
                cell.setCellStyle(headerStyle);
                sheet.setColumnWidth(i, 15 * 256);
            }

            // Verileri doldur
            int rowNum = 1;
            for (Product product : products) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(product.getId());
                row.createCell(1).setCellValue(product.getName());
                row.createCell(2).setCellValue(product.getBuyPrice());
                row.createCell(3).setCellValue(product.getSellPrice());
                row.createCell(4).setCellValue(product.getQuantity());
                row.createCell(5).setCellValue(product.getTotalValue());
                row.createCell(6).setCellValue(product.getCriticalLevel());
                row.createCell(7).setCellValue(product.getBarcode());
                row.createCell(8).setCellValue(product.getSupplier());
                row.createCell(9).setCellValue(product.isLowStock() ? "Kritik" : "Normal");
            }

            // Özet bilgileri ekle
            rowNum += 2;
            Row summaryRow = sheet.createRow(rowNum++);
            summaryRow.createCell(0).setCellValue("Toplam Stok Değeri:");
            summaryRow.createCell(1).setCellValue(
                products.stream()
                    .mapToDouble(Product::getTotalValue)
                    .sum()
            );

            Row criticalRow = sheet.createRow(rowNum);
            criticalRow.createCell(0).setCellValue("Kritik Seviyedeki Ürün Sayısı:");
            criticalRow.createCell(1).setCellValue(
                products.stream()
                    .filter(Product::isLowStock)
                    .count()
            );

            // Dosyaya kaydet
            try (FileOutputStream fileOut = new FileOutputStream(filePath)) {
                workbook.write(fileOut);
            }

        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Rapor oluşturulurken hata: " + e.getMessage());
        }
    }

    public static JFrame createSalesChart(List<Sale> sales) {
        // Satış verilerini günlük olarak grupla
        Map<String, Double> dailySales = new TreeMap<>();
        SimpleDateFormat dayFormat = new SimpleDateFormat("yyyy-MM-dd");
        
        for (Sale sale : sales) {
            String day = dayFormat.format(sale.getSaleDate());
            dailySales.merge(day, sale.getTotalAmount(), Double::sum);
        }

        // Veri setini oluştur
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        dailySales.forEach((day, amount) -> 
            dataset.addValue(amount, "Satış", day)
        );

        // Grafiği oluştur
        JFreeChart chart = ChartFactory.createLineChart(
            "Günlük Satış Grafiği",
            "Tarih",
            "Satış Tutarı (TL)",
            dataset,
            PlotOrientation.VERTICAL,
            true,
            true,
            false
        );

        // Pencereyi oluştur
        JFrame frame = new JFrame("Satış Grafiği");
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setSize(800, 600);
        frame.setLocationRelativeTo(null);

        // Grafiği pencereye ekle
        ChartPanel chartPanel = new ChartPanel(chart);
        chartPanel.setPreferredSize(new Dimension(750, 500));
        frame.add(chartPanel);

        return frame;
    }

    public static JFrame createProductDistributionChart(List<Product> products) {
        // Veri setini oluştur
        DefaultPieDataset dataset = new DefaultPieDataset();
        for (Product product : products) {
            dataset.setValue(product.getName(), product.getTotalValue());
        }

        // Grafiği oluştur
        JFreeChart chart = ChartFactory.createPieChart(
            "Ürün Dağılımı",
            dataset,
            true,
            true,
            false
        );

        // Pencereyi oluştur
        JFrame frame = new JFrame("Ürün Dağılımı");
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setSize(800, 600);
        frame.setLocationRelativeTo(null);

        // Grafiği pencereye ekle
        ChartPanel chartPanel = new ChartPanel(chart);
        chartPanel.setPreferredSize(new Dimension(750, 500));
        frame.add(chartPanel);

        return frame;
    }
} 