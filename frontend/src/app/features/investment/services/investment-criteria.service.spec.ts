import { TestBed } from '@angular/core/testing';

import { InvestmentCriteriaService } from './investment-criteria.service';

describe('InvestmentCriteriaService', () => {
  let service: InvestmentCriteriaService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(InvestmentCriteriaService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
