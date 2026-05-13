package tn.esprit.backend.Services;

import tn.esprit.backend.DTO.NextBestActionAiResult;

public interface NextBestActionAIService {
    NextBestActionAiResult generateAction(NextBestActionPromptContext context);
}
