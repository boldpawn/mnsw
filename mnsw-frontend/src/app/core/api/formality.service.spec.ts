import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import {
  FormalityService,
  FormalityFilters,
  SubmitFormalityRequest,
  CorrectFormalityRequest,
  RejectRequest,
} from './formality.service';
import { Formality, Page } from '../models/formality.model';
import { environment } from '../../../environments/environment';

describe('FormalityService', () => {
  let service: FormalityService;
  let httpMock: HttpTestingController;

  const mockFormality: Formality = {
    id: 'formality-1',
    visitId: 'visit-1',
    type: 'NOA',
    version: 1,
    status: 'SUBMITTED',
    submitterId: 'user-1',
    messageIdentifier: 'MSG-001',
    submittedAt: '2026-04-15T10:00:00Z',
    channel: 'WEB',
  };

  const mockPage: Page<Formality> = {
    content: [mockFormality],
    totalElements: 1,
    totalPages: 1,
    page: 0,
    size: 20,
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [FormalityService],
    });

    service = TestBed.inject(FormalityService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  describe('list', () => {
    it('moet een GET sturen naar /formalities', () => {
      service.list().subscribe(result => {
        expect(result).toEqual(mockPage);
      });

      const req = httpMock.expectOne(`${environment.apiUrl}/formalities`);
      expect(req.request.method).toBe('GET');
      req.flush(mockPage);
    });

    it('moet filterparameters als query-string meesturen', () => {
      const filters: FormalityFilters = {
        type: 'NOA',
        status: 'SUBMITTED',
        page: 0,
        size: 10,
      };

      service.list(filters).subscribe();

      const req = httpMock.expectOne(r => r.url === `${environment.apiUrl}/formalities`);
      expect(req.request.params.get('type')).toBe('NOA');
      expect(req.request.params.get('status')).toBe('SUBMITTED');
      expect(req.request.params.get('page')).toBe('0');
      expect(req.request.params.get('size')).toBe('10');
      req.flush(mockPage);
    });
  });

  describe('get', () => {
    it('moet een GET sturen naar /formalities/:id', () => {
      service.get('formality-1').subscribe(result => {
        expect(result).toEqual(mockFormality);
      });

      const req = httpMock.expectOne(`${environment.apiUrl}/formalities/formality-1`);
      expect(req.request.method).toBe('GET');
      req.flush(mockFormality);
    });
  });

  describe('submit', () => {
    it('moet een POST sturen naar /formalities', () => {
      const command: SubmitFormalityRequest = {
        visitId: 'visit-1',
        type: 'NOA',
        payload: {
          expectedArrival: '2026-04-20T08:00:00Z',
        },
      };

      service.submit(command).subscribe();

      const req = httpMock.expectOne(`${environment.apiUrl}/formalities`);
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual(command);
      req.flush({ formalityId: 'formality-1', messageIdentifier: 'MSG-001', status: 'SUBMITTED', statusUrl: '/api/v1/formalities/formality-1' });
    });
  });

  describe('correct', () => {
    it('moet een POST sturen naar /formalities/:id/corrections', () => {
      const command: CorrectFormalityRequest = {
        payload: {
          expectedArrival: '2026-04-21T08:00:00Z',
        },
      };

      service.correct('formality-1', command).subscribe();

      const req = httpMock.expectOne(`${environment.apiUrl}/formalities/formality-1/corrections`);
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual(command);
      req.flush({ formalityId: 'formality-2', messageIdentifier: 'MSG-002', status: 'SUBMITTED', statusUrl: '/api/v1/formalities/formality-2' });
    });
  });

  describe('approve', () => {
    it('moet een PUT sturen naar /formalities/:id/approve', () => {
      service.approve('formality-1').subscribe();

      const req = httpMock.expectOne(`${environment.apiUrl}/formalities/formality-1/approve`);
      expect(req.request.method).toBe('PUT');
      req.flush({ ...mockFormality, status: 'ACCEPTED' });
    });
  });

  describe('reject', () => {
    it('moet een PUT sturen naar /formalities/:id/reject met reden', () => {
      const rejectRequest: RejectRequest = {
        reasonCode: 'INVALID_IMO',
        reasonDescription: 'Het opgegeven IMO-nummer is ongeldig',
      };

      service.reject('formality-1', rejectRequest).subscribe();

      const req = httpMock.expectOne(`${environment.apiUrl}/formalities/formality-1/reject`);
      expect(req.request.method).toBe('PUT');
      expect(req.request.body).toEqual(rejectRequest);
      req.flush({ ...mockFormality, status: 'REJECTED' });
    });
  });

  describe('setUnderReview', () => {
    it('moet een PUT sturen naar /formalities/:id/review', () => {
      service.setUnderReview('formality-1').subscribe();

      const req = httpMock.expectOne(`${environment.apiUrl}/formalities/formality-1/review`);
      expect(req.request.method).toBe('PUT');
      req.flush({ ...mockFormality, status: 'UNDER_REVIEW' });
    });
  });
});
