package tn.esprit.backend.Services;

import tn.esprit.backend.DTO.NextBestActionResponse;

import java.util.List;

public interface NextBestActionService {
    NextBestActionResponse generateAiAction(String requestId, RequestActor actor);
    List<NextBestActionResponse> getActions(String requestId, RequestActor actor);
    NextBestActionResponse markDone(String actionId, RequestActor actor);
    NextBestActionResponse ignore(String actionId, RequestActor actor);
}
