import { Component, EventEmitter, Input, Output } from '@angular/core';

@Component({
  selector: 'app-nda',
  templateUrl: './nda.component.html',
  styleUrls: ['./nda.component.css'],
})
export class NdaComponent {
  @Input() busy = false;
  @Output() sign = new EventEmitter<void>();
}
