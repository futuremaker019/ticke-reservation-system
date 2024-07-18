package com.reservation.ticket.interfaces.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.reservation.ticket.application.usecase.ReservationUsecase;
import com.reservation.ticket.domain.command.ConcertCommand;
import com.reservation.ticket.domain.command.ReservationCommand;
import com.reservation.ticket.domain.enums.PaymentStatus;
import com.reservation.ticket.domain.enums.ReservationStatus;
import com.reservation.ticket.domain.service.ConcertService;
import com.reservation.ticket.domain.service.QueueService;
import com.reservation.ticket.interfaces.controller.dto.concert.ConcertDto;
import com.reservation.ticket.interfaces.controller.dto.reservation.ReservationDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ReservationController.class)
@ExtendWith(SpringExtension.class)
class ReservationControllerTest {

    @Autowired
    MockMvc mockMvc;
    @Autowired
    ObjectMapper objectMapper;

    @MockBean
    ReservationUsecase reservationUsecase;

    @DisplayName("콘서트 스케줄 id와 자리 id를 받아 예약 생성한다.")
    @Test
    void given_when_then() throws Exception {
        // given
        Long concertScheduleId = 1L;
        List<Long> seatIds = List.of(1L, 2L, 3L);
        int price = 1000;
        String token = "734488355d85";
        ReservationDto.Request request = ReservationDto.Request.of(concertScheduleId, seatIds, price);

        Long reservationId = 1L;
        ReservationCommand.Get reservationCommand =
                ReservationCommand.Get.of(reservationId, price, PaymentStatus.NOT_PAID, ReservationStatus.ACTIVE, LocalDateTime.now());

        given(reservationUsecase.makeReservation(request.toCreate(), token)).willReturn(reservationCommand);

        // when
        mockMvc.perform(post("/api/reservation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request))
                )
                .andDo(print());

        // then
        then(reservationUsecase).should().makeReservation(request.toCreate(), token);
    }

}