package ct01.n07.backend.service;

import ct01.n07.backend.dto.pill.PillImageRequest;
import ct01.n07.backend.dto.pill.PillCatalogResponse;
import ct01.n07.backend.dto.pill.PillCreateRequest;
import ct01.n07.backend.dto.pill.PillRequest;
import ct01.n07.backend.model.Pill;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface PillService {
    Page<PillCatalogResponse> getPillCatalog(String search, Pageable pageable);

    String createPill(PillCreateRequest request);

    void addPill(PillRequest pillRequest);

    void addPillImage(String pillId, PillImageRequest pillImageRequest);

    Page<Pill> getAllPills(Pageable pageable);

    Pill getPillById(String id);

    List<Pill> searchPillsByKeyword(String keyword);

    void updatePill(String id, PillRequest pillRequest);

    void deletePill(String id);
}

