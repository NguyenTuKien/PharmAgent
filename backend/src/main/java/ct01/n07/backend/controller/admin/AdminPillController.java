package ct01.n07.backend.controller.admin;

import ct01.n07.backend.dto.pill.PillCreateRequest;
import ct01.n07.backend.dto.pill.PillImageRequest;
import ct01.n07.backend.dto.pill.PillRequest;
import ct01.n07.backend.dto.pill.PillResponse;
import ct01.n07.backend.service.PillService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/pills")
public class AdminPillController {
    private final PillService pillService;

    @PostMapping("")
    public ResponseEntity<PillResponse> addPill(@Valid @RequestBody PillCreateRequest request) {
        PillResponse response = pillService.createPill(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<PillResponse> updatePill(@PathVariable String id, @Valid @RequestBody PillRequest pillRequest) {
        PillResponse response = pillService.updatePill(id, pillRequest);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deletePill(@PathVariable String id) {
        pillService.deletePill(id);
        return ResponseEntity.ok("Xóa thuốc thành công!");
    }

    @PostMapping("/{pillId}/images")
    public ResponseEntity<PillResponse> addPillImage(@PathVariable String pillId, @RequestBody PillImageRequest pillImageRequest) {
        PillResponse response = pillService.addPillImage(pillId, pillImageRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @DeleteMapping("/{pillId}/images/{imageId}")
    public ResponseEntity<String> deletePillImage(@PathVariable String pillId, @PathVariable String imageId) {
        pillService.deletePillImage(pillId, imageId);
        return ResponseEntity.ok("Xóa hình ảnh cho thuốc thành công!");
    }
}
