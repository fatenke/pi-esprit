package tn.esprit.backend.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor

public class DealCard {

        private String id;
        private String startupId;
        private String requestId;
        private String status;
        private int columnOrder;
        private Long ticketProposed;
        private LocalDateTime lastStatusChangeAt;

        private long daysInStatus;
        private String alertLevel;

}
