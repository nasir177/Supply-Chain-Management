package com.sorascm.backend.analytics.service;

import com.sorascm.backend.analytics.dto.AnalyticsDto;
import com.sorascm.backend.analytics.entity.StockAlert;
import com.sorascm.backend.analytics.repository.AnalyticsRepository;
import com.sorascm.backend.analytics.repository.StockAlertRepository;
import com.sorascm.backend.product.entity.Product;
import com.sorascm.backend.product.repository.ProductRepository;
import com.sorascm.backend.warehouse.entity.Warehouse;
import com.sorascm.backend.warehouse.repository.WarehouseRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class AnalyticsService {

    private static final Logger log = LoggerFactory.getLogger(AnalyticsService.class);

    private final AnalyticsRepository analyticsRepository;
    private final StockAlertRepository stockAlertRepository;
    private final ProductRepository productRepository;
    private final WarehouseRepository warehouseRepository;

    public AnalyticsService(
            AnalyticsRepository analyticsRepository,
            StockAlertRepository stockAlertRepository,
            ProductRepository productRepository,
            WarehouseRepository warehouseRepository
    ) {
        this.analyticsRepository = analyticsRepository;
        this.stockAlertRepository = stockAlertRepository;
        this.productRepository = productRepository;
        this.warehouseRepository = warehouseRepository;
    }

    public List<AnalyticsDto.LowStockItem> getLowStockInventory() {
        return analyticsRepository.findProductsBelowReorderThreshold().stream()
                .map(p -> new AnalyticsDto.LowStockItem(
                        p.getProductId(),
                        p.getProductSku(),
                        p.getProductName(),
                        p.getWarehouseId(),
                        p.getWarehouseName(),
                        p.getTotalOnHand(),
                        p.getTotalReserved(),
                        p.getTotalOnHand() - p.getTotalReserved(),
                        p.getReorderThreshold()
                )).toList();
    }

    public List<AnalyticsDto.WarehouseValuation> getWarehouseValuations() {
        return analyticsRepository.calculateWarehouseValuations().stream()
                .map(v -> new AnalyticsDto.WarehouseValuation(
                        v.getWarehouseId(),
                        v.getWarehouseCode(),
                        v.getWarehouseName(),
                        v.getTotalUnits(),
                        v.getTotalValuation()
                )).toList();
    }

    public List<AnalyticsDto.StockAlertResponse> getActiveAlerts() {
        return stockAlertRepository.findByStatusOrderByCreatedAtDesc("ACTIVE").stream()
                .map(a -> new AnalyticsDto.StockAlertResponse(
                        a.getId(),
                        a.getProduct().getId(),
                        a.getProduct().getName(),
                        a.getWarehouse().getId(),
                        a.getWarehouse().getName(),
                        a.getCurrentStock(),
                        a.getReorderThreshold(),
                        a.getStatus(),
                        a.getAlertMessage(),
                        a.getCreatedAt()
                )).toList();
    }

    @Scheduled(cron = "0 0 * * * *") // Runs every hour (or manually invoked)
    @Transactional
    public void scanAndGenerateAlerts() {
        log.info("Executing scheduled low-stock scan...");
        List<AnalyticsRepository.LowStockProjection> lowStockItems = analyticsRepository.findProductsBelowReorderThreshold();

        for (AnalyticsRepository.LowStockProjection item : lowStockItems) {
            boolean alertAlreadyActive = stockAlertRepository.existsByProductIdAndWarehouseIdAndStatus(
                    item.getProductId(),
                    item.getWarehouseId(),
                    "ACTIVE"
            );

            if (!alertAlreadyActive) {
                Product product = productRepository.getReferenceById(item.getProductId());
                Warehouse warehouse = warehouseRepository.getReferenceById(item.getWarehouseId());

                StockAlert alert = new StockAlert();
                alert.setProduct(product);
                alert.setWarehouse(warehouse);
                alert.setCurrentStock(item.getTotalOnHand());
                alert.setReorderThreshold(item.getReorderThreshold());
                alert.setStatus("ACTIVE");
                alert.setAlertMessage("Stock level (%d) has dropped below threshold (%d) for product %s"
                        .formatted(item.getTotalOnHand(), item.getReorderThreshold(), item.getProductSku()));

                stockAlertRepository.save(alert);
                log.warn("Generated Stock Alert for Product {} at Warehouse {}", item.getProductSku(), item.getWarehouseName());
            }
        }
    }
}