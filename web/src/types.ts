export type Scenario = {
  schemaVersion: number; name: string; durationMicros: number; seed: number;
  queues: { id: string; maxDepth: number; discipline: 'FIFO' | 'PRIORITY' }[];
  workerPools: { id: string; queueId: string; workerCount: number; concurrencyPerWorker: number; serviceTime: { distribution: 'CONSTANT' | 'EXPONENTIAL'; constantMicros: number; meanMicros: number } }[];
  workloads: { id: string; queueId: string; startMicros: number; endMicros: number; arrivals: { mode: 'CONSTANT' | 'POISSON'; ratePerSecond: number } }[];
};
export type Result = {
  scenarioName: string; seed: number; simulationDurationMicros: number; processedEvents: number;
  generatedJobs: number; acceptedJobs: number; completedJobs: number; rejectedJobs: number;
  waitingAtEnd: number; activeAtEnd: number; peakBacklog: number; throughputPerSecond: number;
  averageQueueWaitMicros: number; averageEndToEndLatencyMicros: number;
  p95EndToEndLatencyMicros: number; p99EndToEndLatencyMicros: number; workerUtilization: number;
  trace: { timeMicros: number; type: string; jobId: string; detail: string }[];
};
