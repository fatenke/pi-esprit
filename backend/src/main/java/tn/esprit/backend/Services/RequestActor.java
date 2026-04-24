package tn.esprit.backend.Services;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RequestActor {
    private String userId;
    private UserRole role;
}
