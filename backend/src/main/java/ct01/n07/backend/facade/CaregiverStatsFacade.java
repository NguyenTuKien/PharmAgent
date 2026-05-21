package ct01.n07.backend.facade;

import ct01.n07.backend.dto.stats.CaregiverOverviewResponse;
import ct01.n07.backend.model.EventDose;
import ct01.n07.backend.model.Medication;
import ct01.n07.backend.model.Relationship;
import ct01.n07.backend.model.UserProfile;
import ct01.n07.backend.model.enums.DoseStatus;
import ct01.n07.backend.model.enums.FamilyRelation;
import ct01.n07.backend.model.enums.Role;
import ct01.n07.backend.service.EventDoseService;
import ct01.n07.backend.service.MedicationCoreService;
import ct01.n07.backend.service.RelationshipService;
import ct01.n07.backend.service.UserProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class CaregiverStatsFacade {

    private final UserProfileService userProfileService;
    private final RelationshipService relationshipService;
    private final MedicationCoreService medicationCoreService;
    private final EventDoseService eventDoseService;

    public CaregiverOverviewResponse getCaregiverOverview() {
        UserProfile caregiver = userProfileService.getCurrentUserProfile();
        if (caregiver.getRole() != Role.CAREGIVER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Chỉ tài khoản Caregiver mới có quyền xem trang này");
        }

        log.info("Calculating caregiver overview statistics for caregiver: {}", caregiver.getId());

        List<Relationship> relationships = relationshipService.getAcceptedElderlyRelationships();
        if (relationships.isEmpty()) {
            return createEmptyResponse();
        }

        List<String> patientIds = relationships.stream().map(Relationship::getElderlyId).toList();
        Map<String, UserProfile> patientMap = userProfileService.findAllById(patientIds).stream()
                .collect(Collectors.toMap(UserProfile::getId, p -> p));

        Map<String, Relationship> relationshipMap = relationships.stream()
                .collect(Collectors.toMap(Relationship::getElderlyId, r -> r, (r1, r2) -> r1));

        List<Medication> medications = medicationCoreService.getMedicationsByPatientIds(patientIds);
        if (medications.isEmpty()) {
            return createEmptyResponse();
        }

        Map<String, Medication> medicationMap = medications.stream()
                .collect(Collectors.toMap(Medication::getId, m -> m));

        List<String> medicationIds = medications.stream().map(Medication::getId).toList();

        LocalDate today = LocalDate.now();
        LocalDateTime startOfToday = today.atStartOfDay();
        LocalDateTime endOfToday = today.atTime(LocalTime.MAX);

        List<EventDose> todayDoses = eventDoseService.getDoseEventsByMedicationIdsAndScheduledAtBetween(medicationIds, startOfToday, endOfToday);

        LocalDateTime startOfWeek = today.minusDays(6).atStartOfDay();
        List<EventDose> weeklyDoses = eventDoseService.getDoseEventsByMedicationIdsAndScheduledAtBetween(medicationIds, startOfWeek, endOfToday);

        // 1. KPIs
        int totalDosesToday = todayDoses.size();
        int takenDosesToday = (int) todayDoses.stream().filter(d -> d.getStatus() == DoseStatus.TAKEN).count();
        int missedDosesToday = (int) todayDoses.stream().filter(d -> d.getStatus() == DoseStatus.MISSED || d.getStatus() == DoseStatus.OVERDUE).count();

        long weeklyTotal = weeklyDoses.stream().filter(d -> d.getStatus() != DoseStatus.PENDING).count();
        long weeklyTaken = weeklyDoses.stream().filter(d -> d.getStatus() == DoseStatus.TAKEN).count();
        double overallAdherence = weeklyTotal == 0 ? 100.0 : Math.round(((double) weeklyTaken / weeklyTotal) * 10000.0) / 100.0;

        // 2. Weekly Trend
        List<CaregiverOverviewResponse.AdherenceTrendDay> weeklyAdherence = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            LocalDateTime startOfDay = date.atStartOfDay();
            LocalDateTime endOfDay = date.atTime(LocalTime.MAX);

            List<EventDose> dayDoses = weeklyDoses.stream()
                    .filter(d -> !d.getScheduledAt().isBefore(startOfDay) && !d.getScheduledAt().isAfter(endOfDay))
                    .toList();

            long dayTotal = dayDoses.stream().filter(d -> d.getStatus() != DoseStatus.PENDING).count();
            long dayTaken = dayDoses.stream().filter(d -> d.getStatus() == DoseStatus.TAKEN).count();
            double dayAdherence = dayTotal == 0 ? 100.0 : Math.round(((double) dayTaken / dayTotal) * 10000.0) / 100.0;

            weeklyAdherence.add(CaregiverOverviewResponse.AdherenceTrendDay.builder()
                    .date(date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")))
                    .adherencePercent(dayAdherence)
                    .taken((int) dayTaken)
                    .total((int) dayTotal)
                    .build());
        }

        // 3. Attention Relatives list & Warnings list
        List<CaregiverOverviewResponse.AttentionRelative> attentionRelatives = new ArrayList<>();
        List<CaregiverOverviewResponse.CaregiverWarning> recentWarnings = new ArrayList<>();

        LocalDateTime past24h = LocalDateTime.now().minusHours(24);
        List<EventDose> recentMissedDoses = weeklyDoses.stream()
                .filter(d -> d.getScheduledAt().isAfter(past24h) && (d.getStatus() == DoseStatus.MISSED || d.getStatus() == DoseStatus.OVERDUE))
                .toList();

        for (String patientId : patientIds) {
            UserProfile patient = patientMap.get(patientId);
            if (patient == null) continue;

            Relationship relationship = relationshipMap.get(patientId);
            String relationLabel = relationship != null ? resolveRelationLabel(relationship) : "Người thân";
            String avatarUrl = patient.getAvatarUrl();

            List<Medication> patientMeds = medications.stream()
                    .filter(m -> patientId.equals(m.getPatientId()))
                    .toList();

            List<String> patientMedIds = patientMeds.stream().map(Medication::getId).toList();

            List<EventDose> patientTodayDoses = todayDoses.stream()
                    .filter(d -> patientMedIds.contains(d.getMedicationId()))
                    .toList();

            long patientMissedToday = patientTodayDoses.stream()
                    .filter(d -> d.getStatus() == DoseStatus.MISSED || d.getStatus() == DoseStatus.OVERDUE)
                    .count();

            List<Medication> lowStockMeds = patientMeds.stream()
                    .filter(m -> m.isActive() && m.getTotalQuantity() != null && m.getTotalQuantity() <= 7)
                    .toList();

            String attentionReason = null;
            String severity = null;

            if (patientMissedToday > 0) {
                attentionReason = "Bỏ lỡ " + patientMissedToday + " liều thuốc hôm nay";
                severity = "HIGH";
            } else if (!lowStockMeds.isEmpty()) {
                String medNames = lowStockMeds.stream()
                        .map(m -> m.getNickname() != null && !m.getNickname().isBlank() ? m.getNickname() : "Thuốc")
                        .collect(Collectors.joining(", "));
                attentionReason = "Sắp hết thuốc: " + medNames;
                severity = "MEDIUM";
            }

            if (attentionReason != null) {
                attentionRelatives.add(CaregiverOverviewResponse.AttentionRelative.builder()
                        .patientId(patientId)
                        .patientName(patient.getFirstName() + " " + patient.getLastName())
                        .relationLabel(relationLabel)
                        .avatarUrl(avatarUrl)
                        .reason(attentionReason)
                        .severity(severity)
                        .build());
            }
        }

        // Warnings mapping
        for (EventDose dose : recentMissedDoses) {
            Medication med = medicationMap.get(dose.getMedicationId());
            if (med == null) continue;

            UserProfile patient = patientMap.get(med.getPatientId());
            if (patient == null) continue;

            Relationship relationship = relationshipMap.get(med.getPatientId());
            String relationLabel = relationship != null ? resolveRelationLabel(relationship) : "Người thân";

            String pillName = med.getNickname() != null && !med.getNickname().isBlank() ? med.getNickname() : "Thuốc";

            recentWarnings.add(CaregiverOverviewResponse.CaregiverWarning.builder()
                    .id(dose.getId())
                    .patientName(patient.getFirstName() + " " + patient.getLastName())
                    .relationLabel(relationLabel)
                    .avatarUrl(patient.getAvatarUrl())
                    .pillName(pillName)
                    .scheduledAt(dose.getScheduledAt())
                    .status(dose.getStatus().name())
                    .build());
        }

        // 4. Today's Timeline
        List<CaregiverOverviewResponse.CaregiverTodayDose> todayTimeline = todayDoses.stream()
                .map(dose -> {
                    Medication med = medicationMap.get(dose.getMedicationId());
                    if (med == null) return null;

                    UserProfile patient = patientMap.get(med.getPatientId());
                    if (patient == null) return null;

                    Relationship relationship = relationshipMap.get(med.getPatientId());
                    String relationLabel = relationship != null ? resolveRelationLabel(relationship) : "Người thân";

                    String pillName = med.getNickname() != null && !med.getNickname().isBlank() ? med.getNickname() : "Thuốc";

                    return CaregiverOverviewResponse.CaregiverTodayDose.builder()
                            .eventId(dose.getId())
                            .patientId(patient.getId())
                            .patientName(patient.getFirstName() + " " + patient.getLastName())
                            .relationLabel(relationLabel)
                            .avatarUrl(patient.getAvatarUrl())
                            .pillName(pillName)
                            .scheduledAt(dose.getScheduledAt())
                            .status(dose.getStatus().name())
                            .takenAt(dose.getTakenAt())
                            .dosageAmount(med.getDosageAmount())
                            .dosageUnit(med.getDosageUnit())
                            .build();
                })
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(CaregiverOverviewResponse.CaregiverTodayDose::getScheduledAt))
                .toList();

        int attentionCount = attentionRelatives.size();

        return CaregiverOverviewResponse.builder()
                .kpi(CaregiverOverviewResponse.KpiSummary.builder()
                        .overallAdherence(overallAdherence)
                        .totalDosesToday(totalDosesToday)
                        .takenDosesToday(takenDosesToday)
                        .missedDosesToday(missedDosesToday)
                        .attentionCount(attentionCount)
                        .build())
                .weeklyAdherence(weeklyAdherence)
                .attentionRelatives(attentionRelatives)
                .todayTimeline(todayTimeline)
                .recentWarnings(recentWarnings)
                .build();
    }

    private CaregiverOverviewResponse createEmptyResponse() {
        return CaregiverOverviewResponse.builder()
                .kpi(CaregiverOverviewResponse.KpiSummary.builder()
                        .overallAdherence(100.0)
                        .totalDosesToday(0)
                        .takenDosesToday(0)
                        .missedDosesToday(0)
                        .attentionCount(0)
                        .build())
                .weeklyAdherence(List.of())
                .attentionRelatives(List.of())
                .todayTimeline(List.of())
                .recentWarnings(List.of())
                .build();
    }

    private String resolveRelationLabel(Relationship relationship) {
        FamilyRelation relation = relationship.getRelation() == null ? FamilyRelation.OTHER : relationship.getRelation();
        String customRelation = relationship.getCustomRelation();
        if (relation == FamilyRelation.OTHER && customRelation != null && !customRelation.isBlank()) {
            return customRelation.trim();
        }
        if (relation != FamilyRelation.OTHER) {
            return relation.getLabel();
        }
        String legacyTitle = relationship.getElderlyTitle();
        return legacyTitle != null && !legacyTitle.isBlank() ? legacyTitle.trim() : relation.getLabel();
    }
}
