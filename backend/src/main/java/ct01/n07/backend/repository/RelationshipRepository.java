package ct01.n07.backend.repository;

import ct01.n07.backend.model.Relationship;
import ct01.n07.backend.model.enums.RelationStatus;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RelationshipRepository extends MongoRepository<Relationship, String> {
	List<Relationship> findAllByCaregiverIdAndStatus(String caregiverId, RelationStatus status);

	List<Relationship> findAllByElderlyIdAndStatus(String elderlyId, RelationStatus status);

	boolean existsByCaregiverIdAndElderlyId(String caregiverId, String elderlyId);

	boolean existsByCaregiverIdAndElderlyIdAndStatus(String caregiverId, String elderlyId, RelationStatus status);

	Optional<Relationship> findByIdAndElderlyId(String id, String elderlyId);

	List<Relationship> findAllByCaregiverIdAndElderlyId(String caregiverId, String elderlyId);

	List<Relationship> findAllByCaregiverIdAndElderlyIdAndStatus(String caregiverId, String elderlyId, RelationStatus status);
}
