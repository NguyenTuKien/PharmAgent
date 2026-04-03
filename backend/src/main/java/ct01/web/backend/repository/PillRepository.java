package ct01.web.backend.repository;

import ct01.web.backend.model.Pill;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import org.jspecify.annotations.NonNull;

import java.util.List;
import java.util.Optional;

@Repository
public interface PillRepository extends MongoRepository<@NonNull Pill, @NonNull String> {
    List<Pill> findByIsActiveTrue();

    Page<@NonNull Pill> findByIsActiveTrue(Pageable pageable);

    Optional<Pill> findByNameAndStrength(String name, String strength);
}