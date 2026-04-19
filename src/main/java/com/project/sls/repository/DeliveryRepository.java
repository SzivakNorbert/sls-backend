package com.project.sls.repository;

import com.project.sls.entity.Courier;
import com.project.sls.entity.Delivery;
import com.project.sls.entity.Package;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DeliveryRepository extends JpaRepository<Delivery, Integer> {

    long countByCourierIdAndStatus(Integer courierId, Delivery.Status status);

    // Egy csomag aktuális kézbesítési rekordja
    Optional<Delivery> findByPkg(Package pkg);

    // Egy futár összes kézbesítése
    List<Delivery> findAllByCourier(Courier courier);

    // Egy futár aktív kézbesítései (ASSIGNED vagy IN_TRANSIT)
    @Query("""
        SELECT d FROM Delivery d
        WHERE d.courier = :courier
        AND d.status IN ('ASSIGNED', 'IN_TRANSIT')
        ORDER BY d.assignedAt DESC
    """)
    List<Delivery> findActiveDeliveriesByCourier(@Param("courier") Courier courier);

    // Egy futár befejezett kézbesítéseinek száma (teljesítmény statisztika)
    long countByCourierAndStatus(Courier courier, Delivery.Status status);

    // Összes sikertelen kézbesítés (FAILED) — admin riport
    List<Delivery> findAllByStatus(Delivery.Status status);

    // Egy csomag kézbesítési előzményei (ha újra lett kézbesítve)
    List<Delivery> findAllByPkgOrderByAssignedAtDesc(Package pkg);
}
