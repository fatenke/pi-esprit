package tn.esprit.backend.DTO;

import lombok.Data;
import org.jetbrains.annotations.NotNull;
import tn.esprit.backend.Entities.DealStatus;

@Data
public class MoveCardRequest {
    //@NotBlank(message = "dealId obligatoire")
    private String dealId;

    //@NotNull(message = "newStatus obligatoire")
    private DealStatus newStatus;

    //@Min(value = 0, message = "ordre minimum 0")
    private int newColumnOrder;

}
