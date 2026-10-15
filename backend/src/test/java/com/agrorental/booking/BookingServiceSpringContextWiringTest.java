package com.agrorental.booking;

import com.agrorental.booking.mapper.BookingMapper;
import com.agrorental.booking.repository.BookingRepository;
import com.agrorental.booking.service.BookingService;
import com.agrorental.equipment.repository.EquipmentRepository;
import com.agrorental.farmer.repository.FarmRepository;
import com.agrorental.notification.service.NotificationService;
import com.agrorental.operator.mapper.OperatorJobAssignmentMapper;
import com.agrorental.operator.repository.OperatorJobAssignmentRepository;
import com.agrorental.operator.repository.OperatorLocationRepository;
import com.agrorental.operator.repository.OperatorRepository;
import com.agrorental.operator.repository.OperatorReviewRepository;
import com.agrorental.operator.service.OperatorAssignmentService;
import com.agrorental.operator.service.OperatorEarningsService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Isolated Spring Context Test verifying that the Spring container boots cleanly
 * with the new @Lazy dependency arrangement and constructor injection for BookingService
 * and OperatorAssignmentService without connecting to any external database.
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = BookingServiceSpringContextWiringTest.TestConfig.class)
@DisplayName("E2E-001.1: Spring ApplicationContext Dependency Wiring Test")
class BookingServiceSpringContextWiringTest {

    @Configuration
    static class TestConfig {
        @Bean public BookingRepository bookingRepository() { return mock(BookingRepository.class); }
        @Bean public EquipmentRepository equipmentRepository() { return mock(EquipmentRepository.class); }
        @Bean public OperatorRepository operatorRepository() { return mock(OperatorRepository.class); }
        @Bean public FarmRepository farmRepository() { return mock(FarmRepository.class); }
        @Bean public NotificationService notificationService() { return mock(NotificationService.class); }
        @Bean public BookingMapper bookingMapper() { return new BookingMapper(); }
        @Bean public OperatorJobAssignmentRepository operatorJobAssignmentRepository() { return mock(OperatorJobAssignmentRepository.class); }
        @Bean public OperatorLocationRepository operatorLocationRepository() { return mock(OperatorLocationRepository.class); }
        @Bean public OperatorJobAssignmentMapper operatorJobAssignmentMapper() { return new OperatorJobAssignmentMapper(); }
        @Bean public OperatorEarningsService operatorEarningsService() { return mock(OperatorEarningsService.class); }
        @Bean public OperatorReviewRepository operatorReviewRepository() { return mock(OperatorReviewRepository.class); }

        @Bean
        public OperatorAssignmentService operatorAssignmentService(
                BookingRepository bookingRepository,
                OperatorRepository operatorRepository,
                OperatorJobAssignmentRepository operatorJobAssignmentRepository,
                OperatorJobAssignmentMapper operatorJobAssignmentMapper,
                NotificationService notificationService,
                OperatorEarningsService operatorEarningsService,
                OperatorReviewRepository operatorReviewRepository,
                OperatorLocationRepository operatorLocationRepository) {
            return new OperatorAssignmentService(
                    bookingRepository, operatorRepository, operatorJobAssignmentRepository,
                    operatorJobAssignmentMapper, notificationService, operatorEarningsService,
                    operatorReviewRepository, operatorLocationRepository);
        }

        @Bean
        public BookingService bookingService(
                BookingRepository bookingRepository,
                EquipmentRepository equipmentRepository,
                OperatorRepository operatorRepository,
                FarmRepository farmRepository,
                NotificationService notificationService,
                BookingMapper bookingMapper,
                OperatorJobAssignmentRepository operatorJobAssignmentRepository,
                OperatorLocationRepository operatorLocationRepository,
                @Lazy OperatorAssignmentService operatorAssignmentService) {
            return new BookingService(
                    bookingRepository, equipmentRepository, operatorRepository, farmRepository,
                    notificationService, bookingMapper, operatorJobAssignmentRepository,
                    operatorLocationRepository, operatorAssignmentService);
        }
    }

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private BookingService bookingService;

    @Autowired
    private OperatorAssignmentService operatorAssignmentService;

    @Test
    @DisplayName("Verify Spring context initializes beans and resolves @Lazy OperatorAssignmentService")
    void testSpringContextWiring_InitializesSuccessfully() {
        assertThat(applicationContext).isNotNull();
        assertThat(bookingService).isNotNull();
        assertThat(operatorAssignmentService).isNotNull();

        assertThat(applicationContext.containsBean("bookingService")).isTrue();
        assertThat(applicationContext.containsBean("operatorAssignmentService")).isTrue();
    }
}
