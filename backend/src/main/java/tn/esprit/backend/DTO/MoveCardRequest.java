package tn.esprit.backend.DTO;

import lombok.Data;
import tn.esprit.backend.enums.DealStatus;

@Data
public class MoveCardRequest {
    //@NotBlank(message = "dealId obligatoire")
    private String dealId;

    //@NotNull(message = "newStatus obligatoire")
    private DealStatus newStatus;

    //@Min(value = 0, message = "ordre minimum 0")
    private int newColumnOrder;

}
