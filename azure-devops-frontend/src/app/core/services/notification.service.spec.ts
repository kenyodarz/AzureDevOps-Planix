import { TestBed } from '@angular/core/testing';
import { NotificationService } from './notification.service';

describe('GIVEN NotificationService', () => {
  let service: NotificationService;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [NotificationService] });
    service = TestBed.inject(NotificationService);
  });

  describe('WHEN it is freshly created', () => {
    it('THEN it has no notifications and last() is null', () => {
      expect(service.notifications()).toEqual([]);
      expect(service.last()).toBeNull();
    });
  });

  describe('WHEN error() is invoked', () => {
    it('THEN it publishes a notification with severity error and the given message', () => {
      service.error('Fallo de red');

      const last = service.last();
      expect(last).not.toBeNull();
      expect(last?.severity).toBe('error');
      expect(last?.message).toBe('Fallo de red');
      expect(service.notifications().length).toBe(1);
    });
  });

  describe('WHEN several notifications are published', () => {
    it('THEN they are kept in arrival order and last() returns the most recent one', () => {
      service.info('primero');
      service.warn('segundo');
      service.success('tercero');

      const all = service.notifications();
      expect(all.map((n) => n.message)).toEqual(['primero', 'segundo', 'tercero']);
      expect(all.map((n) => n.severity)).toEqual(['info', 'warn', 'success']);
      expect(service.last()?.message).toBe('tercero');
    });

    it('THEN every notification carries a unique id and a timestamp', () => {
      service.error('a');
      service.error('b');

      const [first, second] = service.notifications();
      expect(first.id).not.toBe(second.id);
      expect(typeof first.timestamp).toBe('number');
    });
  });

  describe('WHEN clear() is invoked', () => {
    it('THEN the history is emptied', () => {
      service.error('algo');
      expect(service.notifications().length).toBe(1);

      service.clear();

      expect(service.notifications()).toEqual([]);
      expect(service.last()).toBeNull();
    });
  });
});
