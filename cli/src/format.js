/**
 * Formats a SimulationRunSnapshot result for human-readable output.
 * @param {object} snapshot
 * @returns {string}
 */
export function formatHuman(snapshot) {
  const r = snapshot.result;
  const lines = [
    `Run ID:      ${snapshot.id}`,
    `Status:      ${snapshot.status}`,
    `Scenario:    ${snapshot.scenarioName}`,
    `Seed:        ${snapshot.seed}`,
  ];

  if (snapshot.completedAt) {
    lines.push(`Completed:   ${snapshot.completedAt}`);
  }

  if (r) {
    lines.push('');
    lines.push('--- Results ---');
    lines.push(`Accepted:    ${r.acceptedJobs}`);
    lines.push(`Completed:   ${r.completedJobs}`);
    lines.push(`Rejected:    ${r.rejectedJobs}`);
    lines.push(`Dead-lettered: ${r.deadLetteredJobs}`);
    lines.push('');
    lines.push(`Throughput:  ${r.throughputPerSecond?.toFixed(2)} jobs/s`);
    lines.push(`P95 latency: ${formatMicros(r.p95EndToEndLatencyMicros)}`);
    lines.push(`P99 latency: ${formatMicros(r.p99EndToEndLatencyMicros)}`);
    lines.push(`Worker util: ${((r.workerUtilization ?? 0) * 100).toFixed(1)}%`);
    lines.push(`Peak backlog: ${r.peakBacklog}`);
  }

  if (snapshot.errorMessage) {
    lines.push('');
    lines.push(`Error: ${snapshot.errorMessage}`);
  }

  return lines.join('\n');
}

/**
 * Formats a SimulationRunSnapshot as compact JSON.
 * @param {object} snapshot
 * @returns {string}
 */
export function formatJson(snapshot) {
  return JSON.stringify(snapshot, null, 2);
}

function formatMicros(micros) {
  if (micros == null) return 'N/A';
  if (micros >= 1_000_000) return `${(micros / 1_000_000).toFixed(3)}s`;
  if (micros >= 1_000) return `${(micros / 1_000).toFixed(3)}ms`;
  return `${micros}µs`;
}
