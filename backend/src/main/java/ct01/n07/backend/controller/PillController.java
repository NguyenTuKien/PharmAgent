package ct01.n07.backend.controller;

import ct01.n07.backend.dto.pill.PillCatalogResponse;
import ct01.n07.backend.model.Pill;
import ct01.n07.backend.service.PillService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

    @GetMapping("/{id}")
    public ResponseEntity<Pill> getPillById(@PathVariable String id) {
        return ResponseEntity.ok(pillService.getPillById(id));
    }

    @GetMapping("/search")
    public ResponseEntity<List<Pill>> searchPills(@RequestParam String keyword) {
        return ResponseEntity.ok(pillService.searchPillsByKeyword(keyword));
    }

}
