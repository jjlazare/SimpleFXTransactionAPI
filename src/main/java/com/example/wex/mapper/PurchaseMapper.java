package com.example.wex.mapper;

import com.example.wex.entity.PurchaseEntity;
import com.example.wex.model.PurchaseCreateRequest;
import com.example.wex.model.PurchaseResponse;
import org.springframework.stereotype.Component;

@Component
public class PurchaseMapper {

    public PurchaseEntity toEntity(PurchaseCreateRequest req) {
        PurchaseEntity e = new PurchaseEntity();
        e.setDescription(req.getDescription());
        e.setTransactionDate(req.getTransactionDate());
        e.setAmountUsd(req.getAmountUsd());
        return e;
    }

    public PurchaseResponse toResponse(PurchaseEntity e) {
        PurchaseResponse r = new PurchaseResponse();
        r.setId(e.getId());
        r.setDescription(e.getDescription());
        r.setTransactionDate(e.getTransactionDate());
        r.setAmountUsd(e.getAmountUsd());
        return r;
    }
}
