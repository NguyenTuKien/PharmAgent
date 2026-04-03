package ct01.web.backend.service;

import ct01.web.backend.dto.pill.PillImageRequest;
import ct01.web.backend.dto.pill.PillRequest;
import ct01.web.backend.model.Pill;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface PillService {
    void addPill(PillRequest pillRequest);

    void addPillImage(String pillId, PillImageRequest pillImageRequest);

    Page<Pill> getAllPills(Pageable pageable);

    Pill getPillById(String id);

    List<Pill> searchPillsByKeyword(String keyword);

    void updatePill(String id, PillRequest pillRequest);

    void deletePill(String id);
}

