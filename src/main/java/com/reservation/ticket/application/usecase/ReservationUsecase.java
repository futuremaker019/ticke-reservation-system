package com.reservation.ticket.application.usecase;

import com.reservation.ticket.application.dto.criteria.ReservationCriteria;
import com.reservation.ticket.application.dto.result.ReservationResult;
import com.reservation.ticket.domain.dto.command.ReservationCommand;
import com.reservation.ticket.domain.dto.info.ReservationInfo;
import com.reservation.ticket.domain.entity.userAccount.UserAccount;
import com.reservation.ticket.domain.enums.LockType;
import com.reservation.ticket.domain.entity.queue.QueueService;
import com.reservation.ticket.domain.entity.concert.reservation.ReservationService;
import com.reservation.ticket.domain.entity.userAccount.UserAccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class ReservationUsecase {

    private final ReservationService reservationService;
    private final QueueService queueService;
    private final UserAccountService userAccountService;

    @Transactional
    public ReservationResult makeReservation(ReservationCriteria criteria, String token) {
        UserAccount userAccount = userAccountService.getUserAccountByToken(token);
        // 예약을 진행한다.
        ReservationInfo reservationInfo = reservationService.reserve(criteria.toCreate(), userAccount, LockType.NONE);
        // 대기열 토큰의 만료일을 연장한다.
        queueService.renewQueueExpirationDate(token);
        return ReservationResult.from(reservationInfo);
    }

    @Transactional
    public ReservationResult makeReservationWithPessimisticLock(ReservationCriteria criteria, String token) {
        UserAccount userAccount = userAccountService.getUserAccountByToken(token);
        // 예약을 진행한다. (비관적락)
        ReservationInfo reservation = reservationService.reserve(criteria.toCreate(), userAccount, LockType.PESSIMISTIC_READ);
        // 대기열 토큰의 만료일을 연장한다.
        queueService.renewQueueExpirationDate(token);
        return ReservationResult.from(reservation);
    }

    @Transactional
    public ReservationResult makeReservationWithDistributedLock(ReservationCriteria criteria, String token) {
        UserAccount userAccount = userAccountService.getUserAccountByToken(token);
        // 예약을 진행한다. (분산락)
        ReservationInfo reservation = reservationService.reserve(criteria.toCreate(), userAccount, LockType.PESSIMISTIC_READ);
        // 대기열 토큰의 만료일을 연장한다.
        queueService.renewQueueExpirationDate(token);
        return ReservationResult.from(reservation);
    }

    public void cancelReservation() {
        reservationService.cancelReservation();
    }
}

