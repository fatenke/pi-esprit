package tn.esprit.backend.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class StripeConfigResponse {
    private String publishableKey;
}
