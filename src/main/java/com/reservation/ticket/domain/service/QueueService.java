package com.reservation.ticket.domain.service;

import com.reservation.ticket.domain.command.QueueCommand;
import com.reservation.ticket.domain.entity.Queue;
import com.reservation.ticket.domain.entity.UserAccount;
import com.reservation.ticket.domain.enums.QueueStatus;
import com.reservation.ticket.domain.repository.QueueRepository;
import com.reservation.ticket.domain.repository.UserAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class QueueService {

    private final QueueRepository queueRepository;
    private final UserAccountRepository userAccountRepository;

    @Transactional
    public QueueCommand.Get createQueue(Long userId) {
        UserAccount userAccount = userAccountRepository.findById(userId);
        Queue queue = Queue.of(userAccount, generateToken(), QueueStatus.WAIT);
        return QueueCommand.Get.from(queueRepository.save(queue));
    }

    @Transactional(readOnly = true)
    public List<QueueCommand.Get> selectQueueByStatus(QueueStatus status) {
        return queueRepository.findAllByQueueStatus(status).stream()
                .map(QueueCommand.Get::from)
                .toList();
    }

    public QueueCommand.Get getQueueCommandByUserId(Long userId) {
        Queue queue = getQueueByUserId(userId);
        return QueueCommand.Get.from(queue);
    }

    @Transactional
    public void expireQueueByChangingStatus(Long userId) {
        Queue queue = getQueueByUserId(userId);
        queue.changeStatus(QueueStatus.EXPIRED);
    }

    public QueueCommand.Get verifyQueueByUserId(Long userId) {
        Queue queue = getQueueByUserId(userId);
        // ACTIVE 가 아니면 예외발생
        queue.verifyQueueStatus();
        return QueueCommand.Get.from(queue);
    }

    @Transactional
    public void renewQueueExpirationDate(String token) {
        Queue queue = queueRepository.findByToken(token);
        queue.extendShouldExpiredAt();
    }

    public QueueCommand.Get renewExpirationDate(Long userId) {
        Queue queue = getQueueByUserId(userId);
        queue.verifyQueueStatus();
        queue.extendShouldExpiredAt();
        return QueueCommand.Get.from(queue);
    }

    /**
     * 결재시 대기열 검증 후 만료
     *   - 대기열 토큰 유효성 검증
     *   - 대기열 만료
     */
    public void expireQueueAfterValidation(String token) {
        Queue queue = queueRepository.findByToken(token);
        queue.verifyQueueStatus();
        queue.changeStatus(QueueStatus.EXPIRED);
    }

    private Queue getQueueByUserId(Long userId) {
        UserAccount userAccount = userAccountRepository.findById(userId);
        return queueRepository.findByToken(userAccount.getToken());
    }

    /**
     *  스케줄러 작업
     *   - ACTIVE 상태인 대기열을 검색하여 만료시간을 현재시간과 비교하여 5분이 초과했다면 EXPIRED 로 상태를 변경
     *   - 제한된 인원(30명)을 기준으로 현재 ACTIVE 상태인 대기열 목록과 비교하여
     *      WAIT 상태인 대기열을 ACTIVE 로 변경
     */
    @Transactional
    public void changeTokenStatusExpiredOrActive() {
        // ACTIVE 상태의 대기열을 EXPIRED 로 변경하는 작업
        List<Queue> queuesAsActive = queueRepository.findAllByQueueStatus(QueueStatus.ACTIVE);
        // ACTIVE 상태의 사용자가 한명도 없을때 리스트 확인
        if (!queuesAsActive.isEmpty()) {
            queuesAsActive.forEach(queue -> {
                // 만료시간이 현재시간 기준 5분 초과시 `EXPIRED` 로 변경
                if (queue.getShouldExpiredAt().plusMinutes(5).isBefore(LocalDateTime.now())) {
                    queue.changeStatus(QueueStatus.EXPIRED);
                }
            });
        }

        // ACTIVE 상태의 대기열을 EXPIRED 로 변경하는 작업
        // 30명으로 인원 제한
        int maxAllowedActive = 30;
        List<Queue> newQueueAsActive = queueRepository.findAllByQueueStatus(QueueStatus.ACTIVE);
        if (newQueueAsActive.size() < maxAllowedActive) {
            // 제한된 인원(30명)과 ACTIVE 대기열 목록을 비교하여 `ACTIVE`로 변경
            int searchLimit = maxAllowedActive - newQueueAsActive.size();
            List<Queue> queuesAsWait = queueRepository.findAllByQueueStatusPerLimit(QueueStatus.WAIT, searchLimit);
            queuesAsWait.forEach(queue -> queue.changeStatus(QueueStatus.ACTIVE));
        }
    }

    private String generateToken() {
        String uuid = UUID.randomUUID().toString();
        return uuid.substring(uuid.lastIndexOf("-") + 1);
    }
}

