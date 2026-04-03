package ct01.web.backend.repository;

import ct01.web.backend.model.PatientMedication;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PatientMedicationRepository extends MongoRepository<PatientMedication, String> {
    Optional<PatientMedication> findByPatientIdAndPillIdAndNickname(String patientId, String pillId, String nickname);
}
