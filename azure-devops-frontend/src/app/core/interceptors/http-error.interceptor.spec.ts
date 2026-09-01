import {
  HttpClient,
  HttpErrorResponse,
  provideHttpClient,
  withInterceptors,
} from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { NotificationService } from '../services/notification.service';
import {
  DEFAULT_HTTP_ERROR_MESSAGE,
  extractHttpErrorMessage,
  httpErrorInterceptor,
} from './http-error.interceptor';

describe('GIVEN httpErrorInterceptor', () => {
  let http: HttpClient;
  let httpMock: HttpTestingController;
  let notifications: NotificationService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([httpErrorInterceptor])),
        provideHttpClientTesting(),
        NotificationService,
      ],
    });
    http = TestBed.inject(HttpClient);
    httpMock = TestBed.inject(HttpTestingController);
    notifications = TestBed.inject(NotificationService);
  });

  afterEach(() => {
    httpMock.verify();
  });

  describe('WHEN the request succeeds', () => {
    it('THEN the response passes through untouched and nothing is notified', () => {
      let received: unknown = null;
      http.get('/api/ok').subscribe((value) => (received = value));

      httpMock.expectOne('/api/ok').flush({ ok: true });

      expect(received).toEqual({ ok: true });
      expect(notifications.notifications()).toEqual([]);
    });
  });

  describe('WHEN the request fails', () => {
    it('THEN it RE-THROWS the error so existing subscribe({error}) keep working', () => {
      let caught: unknown = null;
      http.get('/api/boom').subscribe({
        next: () => {
          throw new Error('no debería emitir next');
        },
        error: (error: unknown) => (caught = error),
      });

      httpMock.expectOne('/api/boom').flush('Explotó', { status: 500, statusText: 'Server Error' });

      expect(caught).toBeInstanceOf(HttpErrorResponse);
    });

    it('THEN it publishes the readable message in the NotificationService', () => {
      http.get('/api/boom').subscribe({ error: () => undefined });

      httpMock.expectOne('/api/boom').flush('Explotó', { status: 500, statusText: 'Server Error' });

      expect(notifications.last()?.severity).toBe('error');
      expect(notifications.last()?.message).toBe('Explotó');
    });
  });
});

describe('GIVEN extractHttpErrorMessage', () => {
  const build = (body: unknown, status = 500): HttpErrorResponse =>
    new HttpErrorResponse({ error: body, status, statusText: 'Error', url: '/api/x' });

  describe('WHEN the error body is a non-empty string', () => {
    it('THEN it returns that string (same precedence as the current code)', () => {
      expect(extractHttpErrorMessage(build('Texto del backend'))).toBe('Texto del backend');
    });
  });

  describe('WHEN the error body is an object with a message field', () => {
    it('THEN it returns that message', () => {
      expect(extractHttpErrorMessage(build({ message: 'Detalle JSON' }))).toBe('Detalle JSON');
    });
  });

  describe('WHEN the error body carries no readable detail', () => {
    it('THEN it falls back to HttpErrorResponse.message', () => {
      const result = extractHttpErrorMessage(build(null));
      expect(typeof result).toBe('string');
      expect(result.length).toBeGreaterThan(0);
    });

    it('THEN it returns the default message when there is nothing at all', () => {
      const bare = new HttpErrorResponse({ error: null });
      Object.defineProperty(bare, 'message', { value: '' });
      expect(extractHttpErrorMessage(bare)).toBe(DEFAULT_HTTP_ERROR_MESSAGE);
    });
  });
});
