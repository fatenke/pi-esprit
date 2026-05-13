package tn.esprit.backend.Entities;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import tn.esprit.backend.enums.InvestmentStatus;

import java.time.LocalDateTime;

@Document(collection = "investment_request")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class InvestmentRequest {
    @Id
    private String id;
    /** Renseigné côté serveur (JWT / filtre d’auth) — jamais depuis le formulaire. */
    private String investorId;
    private String startupId;
    private InvestmentStatus investmentStatus;
    /** Optionnel : montant envisagé (TND), visible côté entrepreneur. */
    private Long ticketProposed;
    private String introMessage;
    /** URL ou chemin d’accès au fichier stocké (ex. GridFS). */
    private String investorDocUrl;
    /** Horodatage d’envoi, défini à la création côté serveur. */
    private LocalDateTime sentAt;
}
