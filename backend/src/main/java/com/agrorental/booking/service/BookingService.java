package com.agrorental.booking.service;

import com.agrorental.booking.dto.BookingRequestDto;
import com.agrorental.booking.dto.BookingResponseDto;
import com.agrorental.booking.entity.constant.BookingStatus;

import java.util.List;

public interface BookingService {

    BookingResponseDto createBooking(BookingRequestDto request);

    BookingResponseDto getBookingById(Long id);

    List<BookingResponseDto> getAllBookings();

    List<BookingResponseDto> getBookingsByFarmer(Long farmerId);

    List<BookingResponseDto> getBookingsByEquipment(Long equipmentId);

    BookingResponseDto updateBookingStatus(Long id, BookingStatus status);
}
