package ct01.n07.backend.service.impl;

import ct01.n07.backend.dto.pill.PillImageRequest;
import ct01.n07.backend.dto.pill.PillCatalogResponse;
import ct01.n07.backend.dto.pill.PillCreateRequest;
import ct01.n07.backend.dto.pill.PillRequest;
import ct01.n07.backend.mapper.PillMapper;
import ct01.n07.backend.model.Pill;
import ct01.n07.backend.model.PillImage;
import ct01.n07.backend.model.UserProfile;
import ct01.n07.backend.model.enums.Role;
import ct01.n07.backend.model.enums.ViewType;
import ct01.n07.backend.repository.PillRepository;
import ct01.n07.backend.service.PillService;
import ct01.n07.backend.service.UserProfileService;
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
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class PillServiceImpl implements PillService {

    private final PillRepository pillRepository;

    private final MongoTemplate mongoTemplate;

    private final PillMapper pillMapper;

    private final UserProfileService userProfileService;

    @Override
    public Page<PillCatalogResponse> getPillCatalog(String search, Pageable pageable) {
        Page<Pill> pills = (search == null || search.isBlank())
                ? pillRepository.findByIsActiveTrue(pageable)
                : pillRepository.findByIsActiveTrueAndNameContainingIgnoreCase(search.trim(), pageable);

        return pills.map(this::toPillCatalogResponse);
    }

    @Override
    public String createPill(PillCreateRequest request) {
        UserProfile currentProfile = userProfileService.getCurrentUserProfile();
        if (currentProfile.getRole() != Role.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only ADMIN can create pill catalog entries");
        }

        List<PillImage> images = request.getImages() == null
                ? new ArrayList<>()
                : request.getImages().stream()
                .filter(Objects::nonNull)
                .filter(url -> !url.isBlank())
                .map(url -> PillImage.builder()
                        .imageUrl(url.trim())
                        .viewType(ViewType.OTHER)
                        .isPrimary(false)
                        .build())
                .toList();

        Pill pill = Pill.builder()
                .name(request.getName().trim())
                .description(request.getDescription())
                .isActive(true)
                .images(images)
                .build();

        return pillRepository.save(pill).getId();
    }

    @Override
    public void addPill(PillRequest pillRequest) {
        pillRepository.save(pillMapper.toModel(pillRequest));
    }

    @Override
    public void addPillImage(String pillId, PillImageRequest pillImageRequest) {
        Pill pill = pillRepository.findById(pillId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thuốc với ID: " + pillId));

        PillImage pillImage = pillMapper.toModel(pillImageRequest);

        if (pill.getImages() == null) {
            pill.setImages(new ArrayList<>());
        }

        pill.getImages().add(pillImage);
        pillRepository.save(pill);
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
    public void updatePill(String id, PillRequest pillRequest) {
        Pill pill = pillRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thuốc với ID: " + id));

        pillMapper.updateModel(pill, pillRequest);

        pillRepository.save(pill);
    }

    @Override
    public void deletePill(String id) {
        Pill pill = pillRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thuốc với ID: " + id));

        // Xóa mềm (Soft Delete)
        pill.setActive(false);
        pillRepository.save(pill);
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
}