package ct01.n07.backend.service;

import ct01.n07.backend.model.MedDose;
import ct01.n07.backend.model.MedSchedule;
import ct01.n07.backend.model.Medication;

public interface EventDoseSyncService {
    void createDoseEvent(Medication pm, MedSchedule schedule, MedDose time);

    void syncDoseEvents(Medication pm);

    void syncDoseEventForTimeUpdate(Medication pm, MedSchedule schedule, MedDose timeToUpdate);

    void deleteByMedicationId(String medicationId);

    void deleteByScheduleId(String scheduleId);

    void deleteByMedDoseId(String medDoseId);

    void deletePendingByMedDoseId(String medDoseId);

    void deletePendingByScheduleId(String scheduleId);

    boolean hasDoseEventForMedDose(String medDoseId);
}

