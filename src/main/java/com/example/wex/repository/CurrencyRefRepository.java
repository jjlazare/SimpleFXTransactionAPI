package com.example.wex.repository;

import com.example.wex.entity.CurrencyRefEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface CurrencyRefRepository extends JpaRepository<CurrencyRefEntity, Long> {

    @Query("select c from CurrencyRefEntity c order by c.countryCurrencyDesc asc")
    List<CurrencyRefEntity> findAllOrdered();
}
