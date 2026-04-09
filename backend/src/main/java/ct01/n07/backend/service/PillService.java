package ct01.n07.backend.service;

import ct01.n07.backend.dto.pill.PillImageRequest;
import ct01.n07.backend.dto.pill.PillCatalogResponse;
import ct01.n07.backend.dto.pill.PillCreateRequest;
import ct01.n07.backend.dto.pill.PillRequest;
import ct01.n07.backend.dto.pill.PillResponse;
import ct01.n07.backend.dto.pill.PillScanResponse;
import ct01.n07.backend.model.Pill;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface PillService {
    Page<PillCatalogResponse> getPillCatalog(String search, Pageable pageable);

    PillResponse createPill(PillCreateRequest request);

    PillResponse addPill(PillRequest pillRequest);

    PillResponse addPillImage(String pillId, PillImageRequest pillImageRequest);

    Page<Pill> getAllPills(Pageable pageable);

    Pill getPillById(String id);

    List<Pill> searchPillsByKeyword(String keyword);

    PillResponse updatePill(String id, PillRequest pillRequest);

    void deletePill(String id);

    void deletePillImage(String pillId, String imageId);

    PillScanResponse scanPill(MultipartFile file);

    // [REFACTOR FIX]: Added for batch fetching to prevent N+1 query
    List<Pill> getPillsByIds(List<String> ids);
}
