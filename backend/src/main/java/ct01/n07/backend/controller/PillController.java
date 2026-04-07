package ct01.n07.backend.controller;

import ct01.n07.backend.dto.pill.PillImageRequest;
import ct01.n07.backend.dto.pill.PillCatalogResponse;
import ct01.n07.backend.dto.pill.PillCreateRequest;
import ct01.n07.backend.dto.pill.PillRequest;
import ct01.n07.backend.model.Pill;
import ct01.n07.backend.service.PillService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/pills")
public class PillController {
    private final PillService pillService;

    @GetMapping("")
    public ResponseEntity<Page<PillCatalogResponse>> getPillCatalog(
            @RequestParam(required = false) String search,
            @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(pillService.getPillCatalog(search, pageable));
    }

    @PostMapping("")
    public ResponseEntity<Map<String, String>> addPill(@Valid @RequestBody PillCreateRequest request) {
        String id = pillService.createPill(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("id", id));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Pill> getPillById(@PathVariable String id) {
        return ResponseEntity.ok(pillService.getPillById(id));
    }

    @GetMapping("/search")
    public ResponseEntity<List<Pill>> searchPills(@RequestParam String keyword) {
        return ResponseEntity.ok(pillService.searchPillsByKeyword(keyword));
    }

    @PutMapping("/{id}")
    public ResponseEntity<String> updatePill(@PathVariable String id, @Valid @RequestBody PillRequest pillRequest) {
        pillService.updatePill(id, pillRequest);
        return ResponseEntity.ok("Cập nhật thông tin thuốc thành công!");
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deletePill(@PathVariable String id) {
        pillService.deletePill(id);
        return ResponseEntity.ok("Xóa thuốc thành công!");
    }

    @PostMapping("/{pillId}/images")
    public ResponseEntity<String> addPillImage(@PathVariable String pillId, @RequestBody PillImageRequest pillImageRequest) {
        pillService.addPillImage(pillId, pillImageRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body("Thêm hình ảnh cho thuốc thành công!");
    }


}
