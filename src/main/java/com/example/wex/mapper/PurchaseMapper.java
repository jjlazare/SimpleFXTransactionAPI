package com.example.wex.mapper;

import com.example.wex.entity.PurchaseEntity;
import com.example.wex.model.PurchaseCreateRequest;
import com.example.wex.model.PurchaseResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
@Slf4j
@Component
public class PurchaseMapper {

    public PurchaseEntity toEntity(PurchaseCreateRequest req) {
        PurchaseEntity e = new PurchaseEntity();
        e.setDescription(req.getDescription());
        e.setTransactionDate(req.getTransactionDate());

        if (req.getAmountUsd() != null) {
            BigDecimal bd = new BigDecimal(req.getAmountUsd())
                    .setScale(2, RoundingMode.HALF_UP);
            e.setAmountUsd(bd);
        } else {
            e.setAmountUsd(null);
        }

        log.debug("REQUEST:: Amount in USD has been set to: {}", e.getAmountUsd());

        return e;
    }

    public PurchaseResponse toResponse(PurchaseEntity e) {
        PurchaseResponse r = new PurchaseResponse();
        r.setId(e.getId());
        r.setDescription(e.getDescription());
        r.setTransactionDate(e.getTransactionDate());

        r.setAmountUsd(
                e.getAmountUsd() != null
                        ? e.getAmountUsd().toPlainString()
                        : null
        );

        log.debug("RSP:: Amount in USD has been set to: {}", r.getAmountUsd());

        return r;
    }
}