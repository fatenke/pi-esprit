import {
  AfterViewInit,
  Component,
  ElementRef,
  OnDestroy,
  OnInit,
  ViewChild,
} from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { Subscription } from 'rxjs';
import { STARTUP_CATALOG_BY_ID } from '../../data/startup-catalog';
import { InvestmentRequest } from '../../models/investment-request';
import {
  CreateHoldingPayload,
  CreateMilestonePayload,
  HoldingRole,
  HoldingStatus,
  InvestmentHolding,
  InvestmentHoldingMilestone,
} from '../../models/investment-holding.model';
import { InvestmentHoldingService } from '../../services/investment-holding.service';
import { InvestmentRequestService } from '../../services/investment-request.service';

type StripeLike = {
  elements(options: { clientSecret: string; appearance?: unknown }): StripeElementsLike;
  confirmPayment(options: {
    elements: StripeElementsLike;
    confirmParams?: { return_url?: string };
    redirect?: 'always' | 'if_required';
  }): Promise<{ error?: { message?: string } }>;
};

type StripeElementsLike = {
  create(type: 'payment'): StripeElementLike;
};

type StripeElementLike = {
  mount(selectorOrElement: string | HTMLElement): void;
  unmount(): void;
};

@Component({
  selector: 'app-investment-holding',
  templateUrl: './investment-holding.component.html',
  styleUrls: ['./investment-holding.component.css']
})
export class InvestmentHoldingComponent implements OnInit, AfterViewInit, OnDestroy {
  @ViewChild('paymentElementHost') paymentElementHost?: ElementRef<HTMLDivElement>;

  requestId = '';
  request: InvestmentRequest | null = null;
  holding: InvestmentHolding | null = null;

  loading = true;
  actionBusy = false;
  message = '';
  error = '';
  stripeReady = false;
  stripePublicKey = '';

  selectedRole: HoldingRole = 'INVESTOR';
  currentUserId = 'dev-investor';

  createHoldingForm: CreateHoldingPayload = {
    amount: 0,
    currency: 'usd',
  };

  milestoneForm: CreateMilestonePayload = {
    title: '',
    amount: 0,
    dueDate: '',
  };

  readonly timeline: HoldingStatus[] = ['CREATED', 'WAITING_PAYMENT', 'FUNDS_HELD', 'RELEASED'];
  readonly roles: HoldingRole[] = ['INVESTOR', 'STARTUP', 'ADMIN'];

  private stripe: StripeLike | null = null;
  private elements: StripeElementsLike | null = null;
  private paymentElement: StripeElementLike | null = null;
  private sub = new Subscription();

  constructor(
    private route: ActivatedRoute,
    private holdingService: InvestmentHoldingService,
    private requestService: InvestmentRequestService
  ) {}

  ngOnInit(): void {
    this.requestId = this.route.snapshot.paramMap.get('requestId') ?? '';
    if (!this.requestId) {
      this.error = 'Request id is missing.';
      this.loading = false;
      return;
    }

    this.sub.add(
      this.holdingService.getStripeConfig().subscribe({
        next: (config) => {
          this.stripePublicKey = config.publishableKey;
        },
        error: () => {
          this.error = 'Stripe public key is missing or invalid.';
        }
      })
    );

    this.loadPage();
  }

  ngAfterViewInit(): void {
    this.mountStripeIfPossible();
  }

  ngOnDestroy(): void {
    this.unmountPaymentElement();
    this.sub.unsubscribe();
  }

  get startupName(): string {
    if (!this.request?.startupId) return 'Unknown startup';
    return STARTUP_CATALOG_BY_ID[this.request.startupId]?.name ?? this.request.startupId;
  }

  get startupSector(): string {
    if (!this.request?.startupId) return 'Unknown';
    return STARTUP_CATALOG_BY_ID[this.request.startupId]?.sector ?? 'Unknown';
  }

  get canCreateHolding(): boolean {
    return this.selectedRole === 'INVESTOR'
      && !!this.request
      && !this.holding;
  }

  get canPay(): boolean {
    return this.selectedRole === 'INVESTOR'
      && this.currentActorMatchesInvestor
      && !!this.holding
      && this.holding.status === 'WAITING_PAYMENT'
      && !!this.holding.stripeClientSecret;
  }

  get canOpenDispute(): boolean {
    return this.selectedRole === 'INVESTOR'
      && this.currentActorMatchesInvestor
      && !!this.holding
      && !['RELEASED', 'REFUNDED', 'CANCELLED'].includes(this.holding.status);
  }

  get canRequestRelease(): boolean {
    return this.selectedRole === 'STARTUP'
      && this.currentActorMatchesStartup
      && !!this.holding
      && this.holding.status === 'FUNDS_HELD';
  }

  get canAdminRelease(): boolean {
    return this.selectedRole === 'ADMIN'
      && !!this.holding
      && ['FUNDS_HELD', 'RELEASE_REQUESTED'].includes(this.holding.status);
  }

  get canAdminRefund(): boolean {
    return this.selectedRole === 'ADMIN'
      && !!this.holding
      && !['RELEASED', 'REFUNDED'].includes(this.holding.status);
  }

  get canCreateMilestone(): boolean {
    return !!this.holding && (this.selectedRole === 'INVESTOR' || this.selectedRole === 'ADMIN');
  }

  onRoleChange(role: HoldingRole): void {
    this.selectedRole = role;
    if (role === 'INVESTOR') {
      this.currentUserId = this.request?.investorId ?? 'dev-investor';
    } else if (role === 'STARTUP') {
      this.currentUserId = this.request?.startupId ?? '';
    } else {
      this.currentUserId = 'admin-user';
    }
  }

  async createHolding(): Promise<void> {
    if (!this.request) return;
    this.resetFeedback();
    this.actionBusy = true;

    this.sub.add(
      this.holdingService.createHolding(this.requestId, this.createHoldingForm, this.currentUserId, this.selectedRole).subscribe({
        next: (holding) => {
          this.holding = holding;
          this.message = 'Investment holding created successfully.';
          this.actionBusy = false;
          this.mountStripeIfPossible();
        },
        error: (err) => {
          this.error = err?.error?.error ?? 'Unable to create the holding.';
          this.actionBusy = false;
        }
      })
    );
  }

  async payWithStripe(): Promise<void> {
    if (!this.stripe || !this.elements || !this.holding) {
      this.error = 'Stripe test payment is not ready yet.';
      return;
    }

    this.resetFeedback();
    this.actionBusy = true;
    const result = await this.stripe.confirmPayment({
      elements: this.elements,
      confirmParams: { return_url: window.location.href },
      redirect: 'if_required',
    });

    if (result.error?.message) {
      this.error = result.error.message;
      this.actionBusy = false;
      return;
    }

    this.sub.add(
      this.holdingService.confirmPayment(this.holding.id, this.currentUserId, this.selectedRole).subscribe({
        next: (holding) => {
          this.holding = holding;
          this.message = 'Stripe test payment confirmed. Funds are now held.';
          this.actionBusy = false;
        },
        error: (err) => {
          this.error = err?.error?.error ?? 'Payment confirmation failed.';
          this.actionBusy = false;
        }
      })
    );
  }

  requestRelease(): void {
    if (!this.holding) return;
    this.runHoldingAction(
      this.holdingService.requestRelease(this.holding.id, this.currentUserId, this.selectedRole),
      'Release requested successfully.'
    );
  }

  openDispute(): void {
    if (!this.holding) return;
    this.runHoldingAction(
      this.holdingService.dispute(this.holding.id, this.currentUserId, this.selectedRole),
      'Dispute opened successfully.'
    );
  }

  adminRelease(): void {
    if (!this.holding) return;
    this.runHoldingAction(
      this.holdingService.releaseFunds(this.holding.id, this.currentUserId),
      'Funds released logically by admin.'
    );
  }

  adminRefund(): void {
    if (!this.holding) return;
    this.runHoldingAction(
      this.holdingService.refund(this.holding.id, this.currentUserId),
      'Holding marked as refunded.'
    );
  }

  createMilestone(): void {
    if (!this.holding) return;
    this.resetFeedback();
    this.actionBusy = true;
    this.sub.add(
      this.holdingService.createMilestone(this.holding.id, this.milestoneForm, this.currentUserId, this.selectedRole).subscribe({
        next: () => {
          this.message = 'Milestone created successfully.';
          this.actionBusy = false;
          this.milestoneForm = { title: '', amount: 0, dueDate: '' };
          this.reloadHolding();
        },
        error: (err) => {
          this.error = err?.error?.error ?? 'Unable to create milestone.';
          this.actionBusy = false;
        }
      })
    );
  }

  validateMilestone(milestone: InvestmentHoldingMilestone): void {
    this.resetFeedback();
    this.actionBusy = true;
    this.sub.add(
      this.holdingService.validateMilestone(milestone.id, this.currentUserId, this.selectedRole).subscribe({
        next: () => {
          this.message = 'Milestone validated successfully.';
          this.actionBusy = false;
          this.reloadHolding();
        },
        error: (err) => {
          this.error = err?.error?.error ?? 'Unable to validate milestone.';
          this.actionBusy = false;
        }
      })
    );
  }

  releaseMilestone(milestone: InvestmentHoldingMilestone): void {
    this.resetFeedback();
    this.actionBusy = true;
    this.sub.add(
      this.holdingService.releaseMilestone(milestone.id, this.currentUserId).subscribe({
        next: () => {
          this.message = 'Milestone released successfully.';
          this.actionBusy = false;
          this.reloadHolding();
        },
        error: (err) => {
          this.error = err?.error?.error ?? 'Unable to release milestone.';
          this.actionBusy = false;
        }
      })
    );
  }

  timelineState(status: HoldingStatus): 'done' | 'active' | 'todo' {
    const actual = this.timelineIndex(this.holding?.status);
    const current = this.timelineIndex(status);
    if (actual > current) return 'done';
    if (actual === current) return 'active';
    return 'todo';
  }

  private loadPage(): void {
    this.loading = true;
    this.sub.add(
      this.requestService.getById(this.requestId).subscribe({
        next: (request) => {
          this.request = request;
          this.createHoldingForm.amount = request.ticketProposed ?? 0;
          this.onRoleChange(this.selectedRole);
          this.loadExistingHolding();
        },
        error: () => {
          this.error = 'Unable to load the investment request.';
          this.loading = false;
        }
      })
    );
  }

  private loadExistingHolding(): void {
    this.sub.add(
      this.holdingService.getHoldingByRequestId(this.requestId, this.currentUserId, this.selectedRole).subscribe({
        next: (holding) => {
          this.holding = holding;
          this.loading = false;
          this.mountStripeIfPossible();
        },
        error: () => {
          this.holding = null;
          this.loading = false;
        }
      })
    );
  }

  private reloadHolding(): void {
    if (!this.holding) return;
    this.sub.add(
      this.holdingService.getHolding(this.holding.id, this.currentUserId, this.selectedRole).subscribe({
        next: (holding) => {
          this.holding = holding;
          this.mountStripeIfPossible();
        },
        error: () => {}
      })
    );
  }

  private runHoldingAction(observable: ReturnType<InvestmentHoldingService['requestRelease']>, successMessage: string): void {
    this.resetFeedback();
    this.actionBusy = true;
    this.sub.add(
      observable.subscribe({
        next: (holding) => {
          this.holding = holding;
          this.message = successMessage;
          this.actionBusy = false;
          this.mountStripeIfPossible();
        },
        error: (err) => {
          this.error = err?.error?.error ?? 'Action failed.';
          this.actionBusy = false;
        }
      })
    );
  }

  private async mountStripeIfPossible(): Promise<void> {
    if (!this.holding?.stripeClientSecret || !this.paymentElementHost || !this.stripePublicKey || !this.canPay) {
      this.unmountPaymentElement();
      return;
    }

    await this.ensureStripeLoaded();
    if (!this.stripe) {
      this.error = 'Stripe test mode could not be initialized.';
      return;
    }

    this.unmountPaymentElement();
    this.elements = this.stripe.elements({
      clientSecret: this.holding.stripeClientSecret,
      appearance: {
        theme: 'stripe',
        variables: {
          colorPrimary: '#2563eb',
        },
      },
    });
    this.paymentElement = this.elements.create('payment');
    this.paymentElement.mount(this.paymentElementHost.nativeElement);
    this.stripeReady = true;
  }

  private async ensureStripeLoaded(): Promise<void> {
    if (this.stripe || typeof window === 'undefined') return;
    const stripeFactory = (window as any).Stripe as ((publishableKey: string) => StripeLike) | undefined;
    if (stripeFactory) {
      this.stripe = stripeFactory(this.stripePublicKey);
      return;
    }

    await new Promise<void>((resolve, reject) => {
      const script = document.createElement('script');
      script.src = 'https://js.stripe.com/v3/';
      script.async = true;
      script.onload = () => resolve();
      script.onerror = () => reject(new Error('Unable to load Stripe.js'));
      document.body.appendChild(script);
    });

    const loadedStripeFactory = (window as any).Stripe as ((publishableKey: string) => StripeLike) | undefined;
    if (loadedStripeFactory) {
      this.stripe = loadedStripeFactory(this.stripePublicKey);
    }
  }

  private unmountPaymentElement(): void {
    if (this.paymentElement) {
      this.paymentElement.unmount();
      this.paymentElement = null;
      this.elements = null;
      this.stripeReady = false;
    }
  }

  private timelineIndex(status?: HoldingStatus): number {
    if (!status) return -1;
    const normalized = status === 'RELEASE_REQUESTED' ? 'FUNDS_HELD' : status;
    return this.timeline.indexOf(normalized as HoldingStatus);
  }

  private resetFeedback(): void {
    this.message = '';
    this.error = '';
  }

  private get currentActorMatchesInvestor(): boolean {
    return this.currentUserId === (this.request?.investorId ?? this.holding?.investorId);
  }

  private get currentActorMatchesStartup(): boolean {
    return this.currentUserId === (this.request?.startupId ?? this.holding?.startupId);
  }
}
