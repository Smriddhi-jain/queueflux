import { describe, it } from 'node:test';
import assert from 'node:assert/strict';
import { formatHuman, formatJson } from '../format.js';

const SNAPSHOT = {
  id: 'aabbccdd-1234-5678-90ab-cdef01234567',
  status: 'COMPLETED',
  scenarioName: 'test-scenario',
  seed: 42,
  submittedAt: '2026-01-01T00:00:00Z',
  completedAt: '2026-01-01T00:01:00Z',
  result: {
    acceptedJobs: 100,
    completedJobs: 98,
    rejectedJobs: 2,
    deadLetteredJobs: 0,
    throughputPerSecond: 9.8,
    p95EndToEndLatencyMicros: 500000,
    p99EndToEndLatencyMicros: 1200000,
    workerUtilization: 0.72,
    peakBacklog: 10,
  },
};

describe('formatHuman()', () => {
  it('includes run ID, status, and scenario name', () => {
    const out = formatHuman(SNAPSHOT);
    assert.ok(out.includes(SNAPSHOT.id));
    assert.ok(out.includes('COMPLETED'));
    assert.ok(out.includes('test-scenario'));
  });

  it('includes throughput and latency metrics', () => {
    const out = formatHuman(SNAPSHOT);
    assert.ok(out.includes('9.80'), `Expected throughput in output: ${out}`);
    assert.ok(out.includes('500.000ms'), `Expected P95 in output: ${out}`);
    assert.ok(out.includes('1.200s'), `Expected P99 in output: ${out}`);
  });

  it('includes worker utilization as percentage', () => {
    const out = formatHuman(SNAPSHOT);
    assert.ok(out.includes('72.0%'), `Expected utilization in output: ${out}`);
  });

  it('handles missing result gracefully', () => {
    const snap = { ...SNAPSHOT, result: null };
    const out = formatHuman(snap);
    assert.ok(out.includes(SNAPSHOT.id));
  });

  it('shows error message when present', () => {
    const snap = { ...SNAPSHOT, result: null, errorMessage: 'simulation crashed' };
    const out = formatHuman(snap);
    assert.ok(out.includes('simulation crashed'));
  });
});

describe('formatJson()', () => {
  it('returns valid JSON', () => {
    const out = formatJson(SNAPSHOT);
    const parsed = JSON.parse(out);
    assert.equal(parsed.id, SNAPSHOT.id);
    assert.equal(parsed.status, 'COMPLETED');
  });
});
