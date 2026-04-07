package ct01.n07.backend.repository;

import ct01.n07.backend.model.PatientMedication;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PatientMedicationRepository extends MongoRepository<PatientMedication, String> {
    Optional<PatientMedication> findByPatientIdAndPillIdAndNickname(String patientId, String pillId, String nickname);

    Page<PatientMedication> findByPatientId(String patientId, Pageable pageable);

    Page<PatientMedication> findByPatientIdAndIsActive(String patientId, boolean isActive, Pageable pageable);
}
