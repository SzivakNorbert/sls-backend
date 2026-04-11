package com.project.sls.repository;

import com.project.sls.entity.Package;
import com.project.sls.entity.StatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StatusHistoryRepository extends JpaRepository<StatusHistory, Integer> {

    // Egy csomag teljes státusz előzménye időrend szerint
    // (csomagkövető oldalon jelenik meg)
    List<StatusHistory> findAllByPkgOrderByChangedAtAsc(Package pkg);

    // Egy csomag legutolsó státuszváltása
    // (ellenőrzéshez: mi volt az előző státusz)
    StatusHistory findTopByPkgOrderByChangedAtDesc(Package pkg);
}
