package com.example.product_service.Repository;

import com.example.product_service.Enums.OutboxEventStatus;
import com.example.product_service.Models.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OutBoxEventRepository extends JpaRepository<OutboxEvent, Long> {

    Optional<OutboxEvent> findByEventId(UUID eventId);

    List<OutboxEvent> findByStatusOrderByCreatedAtAsc(OutboxEventStatus status);

}
