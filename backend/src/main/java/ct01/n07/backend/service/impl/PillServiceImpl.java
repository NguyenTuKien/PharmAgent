package ct01.n07.backend.service.impl;

import ct01.n07.backend.dto.pill.PillImageRequest;
import ct01.n07.backend.dto.pill.PillCatalogResponse;
import ct01.n07.backend.dto.pill.PillCreateRequest;
import ct01.n07.backend.dto.pill.PillRequest;
import ct01.n07.backend.dto.pill.PillResponse;
import ct01.n07.backend.mapper.PillMapper;
import ct01.n07.backend.model.Pill;
import ct01.n07.backend.model.PillImage;
import ct01.n07.backend.model.UserProfile;
import ct01.n07.backend.model.enums.Role;
import ct01.n07.backend.repository.PillRepository;
import ct01.n07.backend.security.ProfileAccessContext;
import ct01.n07.backend.service.PillService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PillServiceImpl implements PillService {

    private final PillRepository pillRepository;

    private final MongoTemplate mongoTemplate;

    private final PillMapper pillMapper;

    private final ProfileAccessContext profileAccessContext;

    @Override
    public Page<PillCatalogResponse> getPillCatalog(String search, Pageable pageable) {
        Page<Pill> pills = (search == null || search.isBlank())
                ? pillRepository.findByIsActiveTrue(pageable)
                : pillRepository.findByIsActiveTrueAndNameContainingIgnoreCase(search.trim(), pageable);

        return pills.map(this::toPillCatalogResponse);
    }

    @Override
    public PillResponse createPill(PillCreateRequest request) {
        UserProfile currentProfile = profileAccessContext.getCurrentUserProfile();
        if (currentProfile.getRole() != Role.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only ADMIN can create pill catalog entries");
        }

        Pill pill = pillMapper.toModel(request);
        pill = pillRepository.save(pill);

        return pillMapper.toResponse(pill);
    }

    @Override
    public PillResponse addPill(PillRequest pillRequest) {
        Pill pill = pillMapper.toModel(pillRequest);
        pill = pillRepository.save(pill);
        return pillMapper.toResponse(pill);
    }

    @Override
    public PillResponse addPillImage(String pillId, PillImageRequest pillImageRequest) {
        Pill pill = pillRepository.findById(pillId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thuốc với ID: " + pillId));

        PillImage pillImage = pillMapper.toModel(pillImageRequest);

        if (pill.getImages() == null) {
            pill.setImages(new ArrayList<>());
        }

        pill.getImages().add(pillImage);
        pill = pillRepository.save(pill);
        return pillMapper.toResponse(pill);
    }

    @Override
    public Page<Pill> getAllPills(Pageable pageable) {
        return pillRepository.findByIsActiveTrue(pageable);
    }

    @Override
    public Pill getPillById(String id) {
        return pillRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thuốc với ID: " + id));
    }

    @Override
    public List<Pill> searchPillsByKeyword(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            // Fix: Trả về danh sách thuốc đang active thay vì tất cả
            return pillRepository.findByIsActiveTrue();
        }

        String kw = keyword.trim();

        // 1. Điều kiện tìm kiếm theo keyword (OR)
        Criteria keywordCriteria = new Criteria().orOperator(
                Criteria.where("name").regex(kw, "i"),
                Criteria.where("genericName").regex(kw, "i"),
                Criteria.where("brandName").regex(kw, "i"),
                Criteria.where("manufacturer").regex(kw, "i"),
                Criteria.where("strength").regex(kw, "i"),
                Criteria.where("dosageForm").regex(kw, "i"),
                Criteria.where("color").regex(kw, "i")
        );

        // 2. Điều kiện bắt buộc: thuốc phải đang Active (AND)
        Criteria activeCriteria = Criteria.where("isActive").is(true);

        // 3. Gộp 2 điều kiện lại: (Tìm theo keyword) AND (isActive = true)
        Criteria finalCriteria = new Criteria().andOperator(activeCriteria, keywordCriteria);

        Query query = new Query(finalCriteria);

        return mongoTemplate.find(query, Pill.class);
    }

    @Override
    public PillResponse updatePill(String id, PillRequest pillRequest) {
        Pill pill = pillRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thuốc với ID: " + id));

        pillMapper.updateModel(pill, pillRequest);

        pill = pillRepository.save(pill);
        return pillMapper.toResponse(pill);
    }

    @Override
    public void deletePill(String id) {
        Pill pill = pillRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thuốc với ID: " + id));

        // Xóa mềm (Soft Delete)
        pill.setActive(false);
        pillRepository.save(pill);
    }

    @Override
    public void deletePillImage(String pillId, String imageId) {
        Pill pill = pillRepository.findById(pillId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thuốc với ID: " + pillId));

        if (pill.getImages() != null) {
            boolean removed = pill.getImages().removeIf(img -> img.getId().equals(imageId));
            if (removed) {
                pillRepository.save(pill);
            } else {
                throw new RuntimeException("Không tìm thấy ảnh với ID: " + imageId);
            }
        }
    }

    private PillCatalogResponse toPillCatalogResponse(Pill pill) {
        List<String> imageUrls = pill.getImages() == null
                ? List.of()
                : pill.getImages().stream().map(PillImage::getImageUrl).toList();

        return PillCatalogResponse.builder()
                .id(pill.getId())
                .name(pill.getName())
                .description(pill.getDescription())
                .imageUrls(imageUrls)
                .createdAt(pill.getCreatedAt())
                .build();
    }

    @Override
    public List<Pill> getPillsByIds(List<String> ids) {
        if (ids == null || ids.isEmpty()) return Collections.emptyList();
        Iterable<Pill> iterable = pillRepository.findAllById(ids);
        List<Pill> result = new ArrayList<>();
        iterable.forEach(result::add);
        return result;
    }
}