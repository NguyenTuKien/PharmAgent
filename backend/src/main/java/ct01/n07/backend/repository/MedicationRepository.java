package ct01.n07.backend.repository;

import ct01.n07.backend.model.Medication;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MedicationRepository extends MongoRepository<Medication, String> {
    Optional<Medication> findByPatientIdAndPillIdAndNickname(String patientId, String pillId, String nickname);

    Page<Medication> findByPatientId(String patientId, Pageable pageable);

    List<Medication> findByPatientIdIn(List<String> patientIds);

    Page<Medication> findByPatientIdAndIsActive(String patientId, boolean isActive, Pageable pageable);

    @Query(value = "{ 'isActive': true }", fields = "{ 'patientId': 1 }")
    List<Medication> findAllActiveMedications();

    // [REFACTOR FIX]: Added MongoDB Aggregation to securely count active users and avoid OOM
    @Aggregation(pipeline = {
        "{ '$match': { 'isActive': true } }",
        "{ '$group': { '_id': '$patientId' } }",
        "{ '$count': 'totalActivePatients' }"
    })
    Long countDistinctActivePatients();
}

