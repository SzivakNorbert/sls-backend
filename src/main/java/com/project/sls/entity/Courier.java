package com.project.sls.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Entity
@Table(name = "couriers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Courier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "vehicle_type", length = 20)
    private VehicleType vehicleType;

    @Column(name = "license_plate", length = 20)
    private String licensePlate;

    @Column(length = 20)
    private String phone;

    @Column(name = "max_weight_kg", precision = 6, scale = 2)
    private BigDecimal maxWeightKg;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    // --- Kapcsolatok ---

    @OneToMany(mappedBy = "courier", fetch = FetchType.LAZY)
    private List<Package> packages;

    @OneToMany(mappedBy = "courier", fetch = FetchType.LAZY)
    private List<com.project.sls.entity.Delivery> deliveries;

    // --- Enum ---

    public enum VehicleType {
        CAR, BIKE, TRUCK, VAN
    }
}
