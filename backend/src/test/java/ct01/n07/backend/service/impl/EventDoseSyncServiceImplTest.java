package ct01.n07.backend.service.impl;

import ct01.n07.backend.model.EventDose;
import ct01.n07.backend.model.MedDose;
import ct01.n07.backend.model.MedSchedule;
import ct01.n07.backend.model.Medication;
import ct01.n07.backend.model.enums.DoseStatus;
import ct01.n07.backend.model.enums.ScheduleType;
import ct01.n07.backend.repository.EventDoseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class EventDoseSyncServiceImplTest {

    @Mock
    private EventDoseRepository eventDoseRepository;

    private EventDoseSyncServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new EventDoseSyncServiceImpl(eventDoseRepository);
    }

    @Test
    void createDoseEventGeneratesDailyEventsAcrossMedicationWindow() {
        Medication medication = Medication.builder()
                .id("med-1")
                .startDate(LocalDate.of(2024, 5, 20))
                .endDate(LocalDate.of(2024, 5, 22))
                .build();
        MedSchedule schedule = MedSchedule.builder()
                .id("schedule-1")
                .scheduleType(ScheduleType.DAILY)
                .frequencyInterval(1)
                .startDate(LocalDate.of(2024, 5, 20))
                .build();
        MedDose dose = MedDose.builder()
                .id("dose-1")
                .takenTime(LocalTime.of(8, 0))
                .quantity(BigDecimal.ONE)
                .build();

        service.createDoseEvent(medication, schedule, dose);

        ArgumentCaptor<EventDose> captor = ArgumentCaptor.forClass(EventDose.class);
        verify(eventDoseRepository, org.mockito.Mockito.times(3)).save(captor.capture());
        assertThat(captor.getAllValues())
                .extracting(EventDose::getScheduledAt)
                .containsExactly(
                        LocalDateTime.of(2024, 5, 20, 8, 0),
                        LocalDateTime.of(2024, 5, 21, 8, 0),
                        LocalDateTime.of(2024, 5, 22, 8, 0)
                );
        assertThat(captor.getAllValues()).allSatisfy(event ->
                assertThat(event.getStatus()).isEqualTo(DoseStatus.PENDING));
    }

    @Test
    void createDoseEventHonorsWeeklyDaysOfWeek() {
        Medication medication = Medication.builder()
                .id("med-1")
                .startDate(LocalDate.of(2024, 5, 20))
                .endDate(LocalDate.of(2024, 6, 2))
                .build();
        MedSchedule schedule = MedSchedule.builder()
                .id("schedule-1")
                .scheduleType(ScheduleType.WEEKLY)
                .frequencyInterval(1)
                .startDate(LocalDate.of(2024, 5, 20))
                .daysOfWeek(List.of("WED", "FRI"))
                .build();
        MedDose dose = MedDose.builder()
                .id("dose-1")
                .takenTime(LocalTime.of(20, 0))
                .quantity(BigDecimal.ONE)
                .build();

        service.createDoseEvent(medication, schedule, dose);

        ArgumentCaptor<EventDose> captor = ArgumentCaptor.forClass(EventDose.class);
        verify(eventDoseRepository, org.mockito.Mockito.times(4)).save(captor.capture());
        assertThat(captor.getAllValues())
                .extracting(EventDose::getScheduledAt)
                .containsExactly(
                        LocalDateTime.of(2024, 5, 22, 20, 0),
                        LocalDateTime.of(2024, 5, 24, 20, 0),
                        LocalDateTime.of(2024, 5, 29, 20, 0),
                        LocalDateTime.of(2024, 5, 31, 20, 0)
                );
    }

    @Test
    void createDoseEventSkipsAsNeededSchedules() {
        Medication medication = Medication.builder().id("med-1").build();
        MedSchedule schedule = MedSchedule.builder()
                .id("schedule-1")
                .scheduleType(ScheduleType.AS_NEEDED)
                .startDate(LocalDate.of(2024, 5, 20))
                .build();
        MedDose dose = MedDose.builder()
                .id("dose-1")
                .takenTime(LocalTime.of(8, 0))
                .quantity(BigDecimal.ONE)
                .build();

        service.createDoseEvent(medication, schedule, dose);

        verify(eventDoseRepository, never()).save(org.mockito.ArgumentMatchers.any(EventDose.class));
    }

    @Test
    void syncDoseEventForTimeUpdateOnlyDeletesPendingEventsForThatDose() {
        Medication medication = Medication.builder()
                .id("med-1")
                .startDate(LocalDate.of(2024, 5, 20))
                .endDate(LocalDate.of(2024, 5, 20))
                .build();
        MedSchedule schedule = MedSchedule.builder()
                .id("schedule-1")
                .scheduleType(ScheduleType.DAILY)
                .startDate(LocalDate.of(2024, 5, 20))
                .build();
        MedDose dose = MedDose.builder()
                .id("dose-1")
                .takenTime(LocalTime.of(9, 30))
                .quantity(BigDecimal.ONE)
                .build();

        service.syncDoseEventForTimeUpdate(medication, schedule, dose);

        verify(eventDoseRepository).deleteByMedDoseIdAndStatus("dose-1", DoseStatus.PENDING);
        ArgumentCaptor<EventDose> captor = ArgumentCaptor.forClass(EventDose.class);
        verify(eventDoseRepository).save(captor.capture());
        assertThat(captor.getValue().getScheduledAt()).isEqualTo(LocalDateTime.of(2024, 5, 20, 9, 30));
    }
}
