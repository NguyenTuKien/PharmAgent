package ct01.web.backend.service;

import ct01.web.backend.model.DoseEvent;

import java.util.List;

public interface DoseEventService {
	List<DoseEvent> getAllDoseEvents();

	DoseEvent getDoseEventById(String id);

	DoseEvent saveDoseEvent(DoseEvent doseEvent);

	void deleteDoseEvent(String id);
}
