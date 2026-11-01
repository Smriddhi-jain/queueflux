import type { Result, Scenario } from '../types';

export type ChartBucket = { time: number; completed: number; waiting: number };

export function formatMetric(value: number): string {
  return new Intl.NumberFormat('en-US', { maximumFractionDigits: 2 }).format(value);
}

export function buildChartData(trace: Result['trace']): ChartBucket[] {
  const buckets = new Map<number, ChartBucket>();
  for (const event of trace) {
    const second = Math.floor(event.timeMicros / 1_000_000);
    const row = buckets.get(second) ?? { time: second, completed: 0, waiting: 0 };
    if (event.type === 'JOB_COMPLETED') row.completed += 1;
    if (event.type === 'JOB_ARRIVED') row.waiting += 1;
    buckets.set(second, row);
  }
  return [...buckets.values()].sort((a, b) => a.time - b.time);
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null && !Array.isArray(value);
}

export function parseScenarioJson(raw: string): Scenario {
  const parsed: unknown = JSON.parse(raw);
  if (
    !isRecord(parsed) ||
    parsed.schemaVersion !== 1 ||
    !Array.isArray(parsed.queues) ||
    !Array.isArray(parsed.workerPools) ||
    !Array.isArray(parsed.workloads)
  ) {
    throw new Error('Expected scenario schemaVersion 1 with queues, workerPools and workloads arrays.');
  }
  return parsed as unknown as Scenario;
}
