package ct01.web.backend.service.impl;

import ct01.web.backend.mapper.DoseEventMapper;
import ct01.web.backend.model.DoseEvent;
import ct01.web.backend.repository.DoseEventRepository;
import ct01.web.backend.service.DoseEventService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DoseEventServiceImpl implements DoseEventService {
    private final DoseEventRepository doseEventRepository;
    private final DoseEventMapper doseEventMapper;

    @Override
    public List<DoseEvent> getAllDoseEvents() {
        return doseEventRepository.findAll();
    }

    @Override
    public DoseEvent getDoseEventById(String id) {
        return doseEventRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Dose event not found"));
    }

    @Override
    public DoseEvent saveDoseEvent(DoseEvent doseEvent) {
        return doseEventRepository.save(doseEventMapper.toModel(doseEvent));
    }

    @Override
    public void deleteDoseEvent(String id) {
        doseEventRepository.deleteById(id);
    }
}
