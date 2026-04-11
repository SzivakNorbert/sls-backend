package com.project.sls.repository;

import com.project.sls.entity.Courier;
import com.project.sls.entity.Package;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PackageRepository extends JpaRepository<Package, Integer> {

    // Tracking szám alapján keresés (publikus csomagkövető)
    Optional<Package> findByTrackingNumber(String trackingNumber);

    // Státusz szerinti szűrés (admin lista)
    List<Package> findAllByStatus(Package.Status status);

    // Egy adott futár összes csomagja
    List<Package> findAllByCourier(Courier courier);

    // Egy futár csomagjai státusz szerint szűrve (futár saját nézete)
    List<Package> findAllByCourierAndStatus(Courier courier, Package.Status status);

    // Még nincs futárhoz rendelve (admin: kiosztásra váró csomagok)
    List<Package> findAllByCourierIsNullAndStatus(Package.Status status);

    // Késő EXPRESS csomagok: IN_TRANSIT állapotban vannak és régebbiek X időnél
    // A scheduled job használja (30 percenként fut)
    @Query("""
        SELECT p FROM Package p
        WHERE p.priority = 'EXPRESS'
        AND p.status = 'IN_TRANSIT'
        AND p.updatedAt < :threshold
    """)
    List<Package> findDelayedExpressPackages(@Param("threshold") LocalDateTime threshold);

    // Statisztika: adott időszakban hány csomag érkezett városonként
    @Query("""
        SELECT p.city, COUNT(p) FROM Package p
        WHERE p.createdAt BETWEEN :from AND :to
        GROUP BY p.city
        ORDER BY COUNT(p) DESC
    """)
    List<Object[]> countPackagesByCity(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );

    // Statisztika: státusz szerinti megoszlás (dashboard)
    @Query("""
        SELECT p.status, COUNT(p) FROM Package p
        GROUP BY p.status
    """)
    List<Object[]> countByStatus();
}
