package ct01.n07.backend.dto.stats;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CaregiverOverviewResponse {
    private KpiSummary kpi;
    private List<AdherenceTrendDay> weeklyAdherence;
    private List<AttentionRelative> attentionRelatives;
    private List<CaregiverTodayDose> todayTimeline;
    private List<CaregiverWarning> recentWarnings;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class KpiSummary {
        private double overallAdherence;
        private int totalDosesToday;
        private int takenDosesToday;
        private int missedDosesToday;
        private int attentionCount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AdherenceTrendDay {
        private String date; // format e.g. "2026-05-22" or "22/05"
        private double adherencePercent;
        private int taken;
        private int total;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AttentionRelative {
        private String patientId;
        private String patientName;
        private String relationLabel;
        private String avatarUrl;
        private String reason;
        private String severity; // "HIGH", "MEDIUM", "LOW"
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CaregiverTodayDose {
        private String eventId;
        private String patientId;
        private String patientName;
        private String relationLabel;
        private String avatarUrl;
        private String pillName;
        private LocalDateTime scheduledAt;
        private String status; // DoseStatus string representation
        private LocalDateTime takenAt;
        private BigDecimal dosageAmount;
        private String dosageUnit;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CaregiverWarning {
        private String id;
        private String patientName;
        private String relationLabel;
        private String avatarUrl;
        private String pillName;
        private LocalDateTime scheduledAt;
        private String status; // MISSED or OVERDUE
    }
}
