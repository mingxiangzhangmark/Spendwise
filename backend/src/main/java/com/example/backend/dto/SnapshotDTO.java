package com.example.backend.dto;

import com.example.backend.model.FeatureSnapshot;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SnapshotDTO {
    private Long id;
    private String totalsByCategoryJson; // [{catId,catName,amount,pct}]
    private BigDecimal totalSpending;
    private String currency;

    public static SnapshotDTO toDTO(FeatureSnapshot featureSnapshot) {
        SnapshotDTO snapshotDTO = new SnapshotDTO();
        snapshotDTO.setId(featureSnapshot.getSnapshotId());
        snapshotDTO.setCurrency(featureSnapshot.getCurrency());
        snapshotDTO.setTotalSpending(featureSnapshot.getTotalSpending());
        snapshotDTO.setTotalsByCategoryJson(featureSnapshot.getTotalsByCategoryJson());
        return snapshotDTO;
    }
}
