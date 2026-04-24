package tn.esprit.backend.Services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tn.esprit.backend.Entities.InvestmentCriteria;
import tn.esprit.backend.Repositories.InvestmentCriteriaRepo;

import java.util.List;

@Service
@RequiredArgsConstructor
public class InvestmentCriteriaServiceImp implements InvestmentCriteriaService{
    private final InvestmentCriteriaRepo investmentCriteriaRepo;

    @Override
    public InvestmentCriteria getInvestmentCriteria(String icId) {
        return investmentCriteriaRepo.findById(icId).orElseThrow(() -> new RuntimeException("InvestmentCriteria not found"));
    }

    @Override
    public InvestmentCriteria addInvestmentCriteria(InvestmentCriteria ic) {
        return investmentCriteriaRepo.save(ic);
    }

    @Override
    public InvestmentCriteria updateInvestmentCriteria(InvestmentCriteria ic) {
        return investmentCriteriaRepo.save(ic);
    }

    @Override
    public void deleteInvestmentCriteria(String icId) {
        investmentCriteriaRepo.deleteById(icId);

    }

    @Override
    public List<InvestmentCriteria> getAllInvestmentCriterias() {
        return investmentCriteriaRepo.findAll();
    }
}
