package tn.esprit.backend.Services;

import tn.esprit.backend.Entities.InvestmentCriteria;

import java.util.List;

public interface InvestmentCriteriaService {
    InvestmentCriteria getInvestmentCriteria(String icId);
    InvestmentCriteria addInvestmentCriteria(InvestmentCriteria ic);
    InvestmentCriteria updateInvestmentCriteria (InvestmentCriteria ic);
    void deleteInvestmentCriteria(String icId);
    List<InvestmentCriteria> getAllInvestmentCriterias();


}
