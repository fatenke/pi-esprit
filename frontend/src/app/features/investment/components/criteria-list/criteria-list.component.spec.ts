import { ComponentFixture, TestBed } from '@angular/core/testing';

import { CriteriaListComponent } from './criteria-list.component';

describe('CriteriaListComponent', () => {
  let component: CriteriaListComponent;
  let fixture: ComponentFixture<CriteriaListComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [CriteriaListComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(CriteriaListComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
