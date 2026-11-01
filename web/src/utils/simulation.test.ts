import { describe, expect, it } from 'vitest';
import { buildChartData, formatMetric, parseScenarioJson } from './simulation';
import type { Scenario } from '../types';

const validScenario: Scenario = {
  schemaVersion: 1,
  name: 'Unit test scenario',
  durationMicros: 1_000_000,
  seed: 7,
  queues: [{ id: 'emails', maxDepth: 100, discipline: 'FIFO' }],
  workerPools: [{
    id: 'email-workers',
    queueId: 'emails',
    workerCount: 2,
    concurrencyPerWorker: 1,
    serviceTime: { distribution: 'CONSTANT', constantMicros: 1000, meanMicros: 1000 }
  }],
  workloads: [{
    id: 'email-arrivals',
    queueId: 'emails',
    startMicros: 0,
    endMicros: 1_000_000,
    arrivals: { mode: 'CONSTANT', ratePerSecond: 2 }
  }]
};

describe('formatMetric', () => {
  it('groups thousands and keeps at most two decimal places', () => {
    expect(formatMetric(1234.567)).toBe('1,234.57');
    expect(formatMetric(12)).toBe('12');
  });
});

describe('buildChartData', () => {
  it('groups events by virtual second and sorts buckets chronologically', () => {
    const trace = [
      { timeMicros: 2_100_000, type: 'JOB_COMPLETED', jobId: 'job-2', detail: '' },
      { timeMicros: 100_000, type: 'JOB_ARRIVED', jobId: 'job-1', detail: '' },
      { timeMicros: 900_000, type: 'JOB_COMPLETED', jobId: 'job-1', detail: '' },
      { timeMicros: 1_200_000, type: 'JOB_ARRIVED', jobId: 'job-2', detail: '' },
      { timeMicros: 1_500_000, type: 'JOB_STARTED', jobId: 'job-2', detail: '' }
    ];
    expect(buildChartData(trace)).toEqual([
      { time: 0, completed: 1, waiting: 1 },
      { time: 1, completed: 0, waiting: 1 },
      { time: 2, completed: 1, waiting: 0 }
    ]);
  });
  it('returns no buckets for an empty trace', () => {
    expect(buildChartData([])).toEqual([]);
  });
});

describe('parseScenarioJson', () => {
  it('parses a valid schema version 1 scenario', () => {
    expect(parseScenarioJson(JSON.stringify(validScenario))).toEqual(validScenario);
  });
  it('rejects unsupported or incomplete scenario shapes', () => {
    expect(() => parseScenarioJson(JSON.stringify({ schemaVersion: 2, queues: [], workerPools: [], workloads: [] })))
      .toThrow('Expected scenario schemaVersion 1');
    expect(() => parseScenarioJson('null')).toThrow('Expected scenario schemaVersion 1');
  });
  it('surfaces malformed JSON syntax errors', () => {
    expect(() => parseScenarioJson('{not-json')).toThrow(SyntaxError);
  });
});
