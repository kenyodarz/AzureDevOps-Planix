import { describe, expect, it } from 'vitest';
import {
  CLASS_AMBER,
  CLASS_GREEN,
  CLASS_RED,
  COLOR_AMBER_MIN,
  COLOR_GREEN_MIN,
  filterStories,
  getLargeStoriesCount,
  getQualityBgClass,
  getQualityLabel,
  getStateClass,
  getUniqueMembers,
  getUniqueStates,
  LABEL_EXCELLENT,
  LABEL_EXCELLENT_MIN,
  LABEL_GOOD,
  LABEL_GOOD_MIN,
  LABEL_POOR,
  LABEL_REGULAR,
  LABEL_REGULAR_MIN,
  LARGE_STORY_POINTS,
} from './quality-score';
import { DashboardStoryItem } from '../models/devops-agent.model';

const createItem = (
  overrides: Partial<DashboardStoryItem> & { id: string },
): DashboardStoryItem => ({
  title: `Story ${overrides.id}`,
  points: 3,
  state: 'Active',
  hasAcceptanceCriteria: true,
  hasDoD: true,
  qualityScore: 100,
  linkedTasksCount: 0,
  ...overrides,
});

describe('GIVEN quality-score domain logic', () => {
  describe('WHEN getQualityLabel is called with different scores', () => {
    it(`THEN returns "${LABEL_EXCELLENT}" when score >= ${LABEL_EXCELLENT_MIN}`, () => {
      expect(getQualityLabel(LABEL_EXCELLENT_MIN)).toBe(LABEL_EXCELLENT);
      expect(getQualityLabel(100)).toBe(LABEL_EXCELLENT);
    });

    it(`THEN returns "${LABEL_GOOD}" when score is between ${LABEL_GOOD_MIN} and ${LABEL_EXCELLENT_MIN - 1}`, () => {
      expect(getQualityLabel(LABEL_EXCELLENT_MIN - 1)).toBe(LABEL_GOOD);
      expect(getQualityLabel(LABEL_GOOD_MIN)).toBe(LABEL_GOOD);
      expect(getQualityLabel(75)).toBe(LABEL_GOOD);
    });

    it(`THEN returns "${LABEL_REGULAR}" when score is between ${LABEL_REGULAR_MIN} and ${LABEL_GOOD_MIN - 1}`, () => {
      expect(getQualityLabel(LABEL_GOOD_MIN - 1)).toBe(LABEL_REGULAR);
      expect(getQualityLabel(LABEL_REGULAR_MIN)).toBe(LABEL_REGULAR);
      expect(getQualityLabel(55)).toBe(LABEL_REGULAR);
    });

    it(`THEN returns "${LABEL_POOR}" when score < ${LABEL_REGULAR_MIN}`, () => {
      expect(getQualityLabel(LABEL_REGULAR_MIN - 1)).toBe(LABEL_POOR);
      expect(getQualityLabel(0)).toBe(LABEL_POOR);
    });
  });

  describe('WHEN getQualityBgClass is called with different scores', () => {
    it(`THEN returns green class when score >= ${COLOR_GREEN_MIN}`, () => {
      expect(getQualityBgClass(COLOR_GREEN_MIN)).toBe(CLASS_GREEN);
      expect(getQualityBgClass(100)).toBe(CLASS_GREEN);
    });

    it(`THEN returns amber class when score is between ${COLOR_AMBER_MIN} and ${COLOR_GREEN_MIN - 1}`, () => {
      expect(getQualityBgClass(COLOR_GREEN_MIN - 1)).toBe(CLASS_AMBER);
      expect(getQualityBgClass(COLOR_AMBER_MIN)).toBe(CLASS_AMBER);
      expect(getQualityBgClass(75)).toBe(CLASS_AMBER);
    });

    it(`THEN returns red class when score < ${COLOR_AMBER_MIN}`, () => {
      expect(getQualityBgClass(COLOR_AMBER_MIN - 1)).toBe(CLASS_RED);
      expect(getQualityBgClass(0)).toBe(CLASS_RED);
    });
  });

  describe('WHEN getStateClass is called for Azure DevOps states', () => {
    it('THEN returns green class for done or closed state', () => {
      expect(getStateClass('Done')).toContain('#10b981');
      expect(getStateClass('closed')).toContain('#10b981');
    });

    it('THEN returns blue class for committed or active state', () => {
      expect(getStateClass('Committed')).toContain('#2563eb');
      expect(getStateClass('active')).toContain('#2563eb');
    });

    it('THEN returns yellow class for approved state', () => {
      expect(getStateClass('Approved')).toContain('#f2c94c');
      expect(getStateClass('approved')).toContain('#f2c94c');
    });

    it('THEN returns neutral class for unknown or empty states', () => {
      expect(getStateClass('New')).toContain('#9ca3af');
      expect(getStateClass('')).toContain('#9ca3af');
    });
  });

  describe('WHEN getLargeStoriesCount is evaluated', () => {
    it(`THEN counts only stories with points >= ${LARGE_STORY_POINTS}`, () => {
      const items = [
        createItem({ id: '1', points: 5 }),
        createItem({ id: '2', points: LARGE_STORY_POINTS }),
        createItem({ id: '3', points: 21 }),
      ];
      expect(getLargeStoriesCount(items)).toBe(2);
    });

    it('THEN returns 0 when items array is null, undefined or empty', () => {
      expect(getLargeStoriesCount(null)).toBe(0);
      expect(getLargeStoriesCount(undefined)).toBe(0);
      expect(getLargeStoriesCount([])).toBe(0);
    });
  });

  describe('WHEN filterStories is executed', () => {
    const items: DashboardStoryItem[] = [
      createItem({ id: '1', assignedMember: 'Alice', state: 'Active', qualityScore: 30 }),
      createItem({ id: '2', assignedMember: 'Bob', state: 'Done', qualityScore: 65 }),
      createItem({ id: '3', assignedMember: 'Alice', state: 'Active', qualityScore: 85 }),
    ];

    it('THEN returns all items when filters are empty', () => {
      expect(filterStories(items, {})).toHaveLength(3);
      expect(filterStories(null, {})).toEqual([]);
    });

    it('THEN filters by member correctly', () => {
      const result = filterStories(items, { member: 'Alice' });
      expect(result.map((i) => i.id)).toEqual(['1', '3']);
    });

    it('THEN filters by state correctly', () => {
      const result = filterStories(items, { state: 'Done' });
      expect(result.map((i) => i.id)).toEqual(['2']);
    });

    it('THEN filters by quality score buckets (critical < 50, regular 50-79, good >= 80)', () => {
      expect(filterStories(items, { quality: 'critical' }).map((i) => i.id)).toEqual(['1']);
      expect(filterStories(items, { quality: 'regular' }).map((i) => i.id)).toEqual(['2']);
      expect(filterStories(items, { quality: 'good' }).map((i) => i.id)).toEqual(['3']);
    });

    it('THEN applies combined filters as conjunction', () => {
      const result = filterStories(items, { member: 'Alice', quality: 'good' });
      expect(result.map((i) => i.id)).toEqual(['3']);
    });
  });

  describe('WHEN getUniqueStates and getUniqueMembers are called', () => {
    const items: DashboardStoryItem[] = [
      createItem({ id: '1', assignedMember: 'Alice', state: 'Active' }),
      createItem({ id: '2', assignedMember: 'Bob', state: 'Done' }),
      createItem({ id: '3', assignedMember: 'Alice', state: 'Active' }),
    ];

    it('THEN getUniqueStates deduplicates states and ignores empty ones', () => {
      expect(getUniqueStates(items)).toEqual(['Active', 'Done']);
      expect(getUniqueStates(null)).toEqual([]);
    });

    it('THEN getUniqueMembers deduplicates members and ignores empty ones', () => {
      expect(getUniqueMembers(items)).toEqual(['Alice', 'Bob']);
      expect(getUniqueMembers(null)).toEqual([]);
    });
  });
});
