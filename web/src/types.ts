export type RetryPolicy = { maxAttempts: number; initialBackoffMicros: number; maxBackoffMicros: number; multiplier: number; jitterFraction: number };
export type RateLimitPolicy = { ratePerSecond: number; burstCapacity: number };
export type WorkerOutage = { id: string; startMicros: number; endMicros: number; unavailableWorkers: number };
export type Scenario = {
  schemaVersion: number; name: string; durationMicros: number; seed: number;
  queues: { id: string; maxDepth: number; discipline: 'FIFO' | 'PRIORITY'; retryPolicy?: RetryPolicy; rateLimit?: RateLimitPolicy }[];
  workerPools: { id: string; queueId: string; workerCount: number; concurrencyPerWorker: number; serviceTime: { distribution: 'CONSTANT' | 'EXPONENTIAL'; constantMicros: number; meanMicros: number }; failureProbability?: number; outages?: WorkerOutage[] }[];
  workloads: { id: string; queueId: string; startMicros: number; endMicros: number; priority?: number; arrivals: { mode: 'CONSTANT' | 'POISSON'; ratePerSecond: number } }[];
};
export type Result = {
  scenarioName: string; seed: number; simulationDurationMicros: number; processedEvents: number;
  generatedJobs: number; acceptedJobs: number; completedJobs: number; rejectedJobs: number;
  retriedJobs: number; failedAttempts: number; deadLetteredJobs: number;
  waitingAtEnd: number; delayedRetryAtEnd: number; activeAtEnd: number; peakBacklog: number;
  throughputPerSecond: number; averageQueueWaitMicros: number; averageEndToEndLatencyMicros: number;
  p95EndToEndLatencyMicros: number; p99EndToEndLatencyMicros: number; workerUtilization: number;
  queues: { queueId: string; acceptedJobs: number; completedJobs: number; rejectedJobs: number; retriedJobs: number; deadLetteredJobs: number; waitingAtEnd: number; delayedRetryAtEnd: number; activeAtEnd: number; peakBacklog: number; throughputPerSecond: number; averageQueueWaitMicros: number; p95LatencyMicros: number; p99LatencyMicros: number; workerUtilization: number }[];
  trace: { timeMicros: number; type: string; jobId: string; detail: string }[];
};
export type RunSnapshot = {
  id: string; status: 'QUEUED'|'RUNNING'|'COMPLETED'|'FAILED'; scenarioName: string; seed: number;
  submittedAt: string; completedAt: string|null; virtualTimeMicros: number; processedEvents: number;
  scenario: Scenario; result: Result|null; errorMessage: string|null;
};
