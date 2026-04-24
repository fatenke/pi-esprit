package tn.esprit.backend.Controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import tn.esprit.backend.Entities.InvestmentCriteria;
import tn.esprit.backend.Services.InvestmentCriteriaService;

import java.util.List;

@RestController
@RequestMapping("/api/invest-criteria")
@RequiredArgsConstructor
public class InvestmentCriteriaController {

    private final InvestmentCriteriaService investmentCriteriaService;
    @PostMapping("/add")
    public InvestmentCriteria addIC (@RequestBody InvestmentCriteria ic){
        return investmentCriteriaService.addInvestmentCriteria(ic);
    }
    @PutMapping("/update")
    public InvestmentCriteria updateIC(@RequestBody InvestmentCriteria ic){
        return investmentCriteriaService.updateInvestmentCriteria(ic);
    }
    @GetMapping("/get/{icId}")
    public InvestmentCriteria getIc(@PathVariable String icId){
        return investmentCriteriaService.getInvestmentCriteria(icId);
    }
    @GetMapping("/getAll")
    public List<InvestmentCriteria> getAllIc(){
        return investmentCriteriaService.getAllInvestmentCriterias();
    }

    @DeleteMapping("/remove/{icId}")
    public void deleteIc(@PathVariable String icId){
        investmentCriteriaService.deleteInvestmentCriteria(icId);
    }


}
