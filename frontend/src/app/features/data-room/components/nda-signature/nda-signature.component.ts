import {
  AfterViewInit,
  Component,
  ElementRef,
  EventEmitter,
  Input,
  OnChanges,
  Output,
  SimpleChanges,
  ViewChild,
} from '@angular/core';
import SignaturePad from 'signature_pad';
import {
  NdaAgreement,
  SignNdaPayload,
} from '../../models/data-room.models';

@Component({
  selector: 'app-nda-signature',
  templateUrl: './nda-signature.component.html',
  styleUrls: ['./nda-signature.component.css'],
})
export class NdaSignatureComponent implements AfterViewInit, OnChanges {
  @Input() agreement: NdaAgreement | null = null;
  @Input() busy = false;
  @Output() submitSignature = new EventEmitter<SignNdaPayload>();
  @Output() accessRequested = new EventEmitter<void>();

  @ViewChild('signatureCanvas') signatureCanvas?: ElementRef<HTMLCanvasElement>;

  acceptedTerms = false;
  signerFullName = '';
  error = '';

  private signaturePad: SignaturePad | null = null;

  ngAfterViewInit(): void {
    this.initSignaturePad();
  }

  ngOnChanges(_: SimpleChanges): void {
    queueMicrotask(() => this.initSignaturePad());
  }

  get isSigned(): boolean {
    return this.agreement?.status === 'SIGNED';
  }

  get canSubmit(): boolean {
    if (this.busy || !this.agreement || this.isSigned) return false;
    if (!this.acceptedTerms || !this.signerFullName.trim()) return false;
    return !!this.signaturePad && !this.signaturePad.isEmpty();
  }

  clearSignature(): void {
    this.signaturePad?.clear();
    this.error = '';
  }

  sign(): void {
    this.error = '';
    if (!this.acceptedTerms) {
      this.error = 'Vous devez accepter les conditions du NDA.';
      return;
    }
    if (!this.signerFullName.trim()) {
      this.error = 'Le nom complet du signataire est obligatoire.';
      return;
    }

    if (!this.signaturePad || this.signaturePad.isEmpty()) {
      this.error = 'Veuillez dessiner votre signature avant de continuer.';
      return;
    }

    this.submitSignature.emit({
      signerFullName: this.signerFullName.trim(),
      signatureType: 'DRAWN',
      signatureImageBase64: this.signaturePad.toDataURL('image/png'),
      typedSignature: null,
      acceptedTerms: this.acceptedTerms,
    });
  }

  private initSignaturePad(): void {
    if (typeof window === 'undefined') return;
    const canvas = this.signatureCanvas?.nativeElement;
    if (!canvas) return;

    const ratio = Math.max(window.devicePixelRatio || 1, 1);
    const width = canvas.offsetWidth || 640;
    const height = canvas.offsetHeight || 220;
    canvas.width = width * ratio;
    canvas.height = height * ratio;

    const context = canvas.getContext('2d');
    if (!context) return;
    context.scale(ratio, ratio);

    this.signaturePad = new SignaturePad(canvas, {
      minWidth: 0.8,
      maxWidth: 2.2,
      penColor: '#0f172a',
      backgroundColor: 'rgba(255,255,255,0)',
    });
  }
}
