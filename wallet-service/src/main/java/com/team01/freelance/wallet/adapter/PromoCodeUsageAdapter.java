package com.team01.freelance.wallet.adapter;

import com.team01.freelance.wallet.dto.PromoCodeUsageDTO;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;

@Component
public class PromoCodeUsageAdapter {

    public PromoCodeUsageDTO adapt(Object[] row) {
        if (row == null) {
            return null;
        }

        boolean isExpired = false;
        if (row.length > 7 && row[7] != null) {
            if (row[7] instanceof LocalDateTime ldt) {
                isExpired = ldt.isBefore(LocalDateTime.now());
            } else if (row[7] instanceof java.sql.Timestamp ts) {
                isExpired = ts.toLocalDateTime().isBefore(LocalDateTime.now());
            }
        }

        return PromoCodeUsageDTO.builder()
                .promoCodeId(row[0] != null ? ((Number) row[0]).longValue() : null)
                .code(row[1] != null ? String.valueOf(row[1]) : null)
                .discountType(row[2] != null ? String.valueOf(row[2]) : null)
                .discountValue(row[3] != null ? ((Number) row[3]).doubleValue() : 0.0)
                .timesUsed(row[4] != null ? ((Number) row[4]).longValue() : 0L)
                .totalDiscountGiven(row[5] != null ? ((Number) row[5]).doubleValue() : 0.0)
                .active(row[6] != null ? (Boolean) row[6] : false)
                .expired(isExpired)
                .build();
    }
}