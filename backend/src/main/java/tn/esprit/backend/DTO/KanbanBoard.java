package tn.esprit.backend.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tn.esprit.backend.enums.DealStatus;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KanbanBoard {

    // Map : nom de la colonne -> liste de cartes
    // ex: "NEGOTIATION" -> [card1, card2]
    private Map<DealStatus, List<DealCard>>  columns;

    private int totalDeals;   // total tous statuts confondus
    private int activeDeals;  // deals non termines
    private int alertCount;   // deals avec alertLevel != NONE
}
