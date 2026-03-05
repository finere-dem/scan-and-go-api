package com.finere.scan_and_go_api.repository;

import com.finere.scan_and_go_api.model.Store;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StoreRepository extends JpaRepository<Store, Long> {
}
