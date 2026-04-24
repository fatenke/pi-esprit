import { TestBed } from '@angular/core/testing';

import { InvestmentRequestService } from './investment-request.service';

describe('InvestmentRequestService', () => {
  let service: InvestmentRequestService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(InvestmentRequestService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
