package com.project.sls.repository;

import com.project.sls.entity.Courier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CourierRepository extends JpaRepository<Courier, Integer> {

    // User ID alapján megkeresi a hozzá tartozó courier profilt
    Optional<Courier> findByUserId(Integer userId);

    // Csak az aktív futárokat listázza (admin dashboard)
    List<Courier> findAllByIsActiveTrue();

    // Aktív futárok egy adott jármű típussal
    List<Courier> findAllByIsActiveTrueAndVehicleType(Courier.VehicleType vehicleType);

    // Megkeresi azokat a futárokat, akiknek kevesebb mint X aktív kézbesítésük van
    // (hozzárendelés előtt ellenőrizzük, hogy túlterhelt-e a futár)
    @Query("""
        SELECT c FROM Courier c
        WHERE c.isActive = true
        AND (SELECT COUNT(d) FROM Delivery d
             WHERE d.courier = c
             AND d.status IN ('ASSIGNED', 'IN_TRANSIT')) < :maxActive
    """)
    List<Courier> findAvailableCouriers(int maxActive);
}
