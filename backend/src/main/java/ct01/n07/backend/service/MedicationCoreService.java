package ct01.n07.backend.service;

import ct01.n07.backend.dto.medication.MedicationCreateRequest;
import ct01.n07.backend.dto.medication.MedicationResponse;
import ct01.n07.backend.dto.medication.MedicationUpdateRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface MedicationCoreService {
    MedicationResponse createMedication(MedicationCreateRequest request);

    Page<MedicationResponse> getMedications(String patientId, Boolean isActive, Pageable pageable);

    MedicationResponse updateMedication(String id, MedicationUpdateRequest request);

    MedicationResponse getMedicationById(String id);

    void deleteMedication(String id);
}
