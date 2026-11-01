import { useMemo, useState } from 'react';
import type { ReactNode } from 'react';
import { Activity, ArrowUpRight, Clock3, Gauge, Play, RotateCcw, Server, Zap } from 'lucide-react';
import { Area, AreaChart, CartesianGrid, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts';
import type { Result, Scenario } from './types';

const initial: Scenario = {
  schemaVersion: 1, name: 'Email notification workload', durationMicros: 60_000_000, seed: 12345,
  queues: [{ id: 'email-queue', maxDepth: 5000, discipline: 'FIFO' }],
  workerPools: [{ id: 'email-workers', queueId: 'email-queue', workerCount: 5, concurrencyPerWorker: 1, serviceTime: { distribution: 'EXPONENTIAL', constantMicros: 250000, meanMicros: 250000 } }],
  workloads: [{ id: 'email-arrivals', queueId: 'email-queue', startMicros: 0, endMicros: 60_000_000, arrivals: { mode: 'POISSON', ratePerSecond: 20 } }]
};
const fmt = (value: number) => new Intl.NumberFormat('en-US', { maximumFractionDigits: 2 }).format(value);

export default function App() {
  const [scenario, setScenario] = useState(initial);
  const [result, setResult] = useState<Result | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const apiUrl = import.meta.env.VITE_API_URL ?? 'http://localhost:8080';
  const chartData = useMemo(() => {
    if (!result) return [];
    const buckets = new Map<number, { time: number; completed: number; waiting: number }>();
    for (const event of result.trace) {
      const second = Math.floor(event.timeMicros / 1_000_000);
      const row = buckets.get(second) ?? { time: second, completed: 0, waiting: 0 };
      if (event.type === 'JOB_COMPLETED') row.completed += 1;
      if (event.type === 'JOB_ARRIVED') row.waiting += 1;
      buckets.set(second, row);
    }
    return [...buckets.values()].sort((a, b) => a.time - b.time);
  }, [result]);

  function updatePool(patch: Partial<Scenario['workerPools'][number]>) {
    setScenario(current => ({ ...current, workerPools: [{ ...current.workerPools[0], ...patch }] }));
  }
  function updateWorkload(patch: Partial<Scenario['workloads'][number]['arrivals']>) {
    setScenario(current => ({ ...current, workloads: [{ ...current.workloads[0], arrivals: { ...current.workloads[0].arrivals, ...patch } }] }));
  }
  async function runSimulation() {
    setLoading(true); setError(''); setResult(null);
    try {
      const response = await fetch(`${apiUrl}/api/v1/simulations/run`, {
        method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(scenario)
      });
      if (!response.ok) throw new Error((await response.json()).message ?? `Simulation failed (${response.status})`);
      setResult(await response.json() as Result);
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : 'Could not reach the QueueFlux API.');
    } finally { setLoading(false); }
  }

  return <main className="shell">
    <header className="topbar"><div className="brand"><div className="brand-mark"><Activity size={19}/></div><span>queue<span className="brand-accent">flux</span></span><span className="version">LAB / 0.1</span></div><div className="top-meta"><span className="live-dot"/> LOCAL EXPERIMENT</div></header>
    <section className="hero"><div><div className="eyebrow"><span className="eyebrow-line"/> DISCRETE-EVENT WORKLOAD LAB</div><h1>Find the breaking point.<br/><em>Before production does.</em></h1><p className="hero-copy">Design a workload, tune worker capacity, and see how your queue behaves under pressure — with repeatable, seeded experiments.</p></div><div className="hero-stamp"><div className="stamp-orbit"><Zap size={25}/></div><span>VIRTUAL TIME</span><strong>NO SLEEP. JUST EVENTS.</strong></div></section>
    <div className="workspace"><section className="panel config-panel"><div className="panel-head"><div><div className="section-kicker">01 / EXPERIMENT SETUP</div><h2>Configure workload</h2></div><button className="icon-button" title="Reset defaults" onClick={() => {setScenario(initial);setResult(null);setError('');}}><RotateCcw size={16}/></button></div>
      <label className="field-label">SCENARIO NAME<input value={scenario.name} onChange={e=>setScenario({...scenario,name:e.target.value})}/></label>
      <div className="field-grid"><label className="field-label">ARRIVAL RATE <span>jobs / sec</span><input type="number" min="0.1" step="0.5" value={scenario.workloads[0].arrivals.ratePerSecond} onChange={e=>updateWorkload({ratePerSecond:Math.max(0.1,Number(e.target.value))})}/></label><label className="field-label">WORKERS<input type="number" min="1" max="500" value={scenario.workerPools[0].workerCount} onChange={e=>updatePool({workerCount:Math.max(1,Math.min(500,Number(e.target.value)))})}/></label></div>
      <div className="field-grid"><label className="field-label">CONCURRENCY / WORKER<input type="number" min="1" max="100" value={scenario.workerPools[0].concurrencyPerWorker} onChange={e=>updatePool({concurrencyPerWorker:Math.max(1,Math.min(100,Number(e.target.value)))})}/></label><label className="field-label">MEAN SERVICE TIME <span>ms</span><input type="number" min="1" value={scenario.workerPools[0].serviceTime.meanMicros/1000} onChange={e=>updatePool({serviceTime:{distribution:'EXPONENTIAL',constantMicros:Math.max(1,Number(e.target.value))*1000,meanMicros:Math.max(1,Number(e.target.value))*1000}})}/></label></div>
      <div className="field-grid"><label className="field-label">DURATION <span>seconds</span><input type="number" min="1" max="3600" value={scenario.durationMicros/1_000_000} onChange={e=>{const duration=Math.max(1,Number(e.target.value))*1_000_000;setScenario(current=>({...current,durationMicros:duration,workloads:current.workloads.map(workload=>({...workload,endMicros:duration}))}));}}/></label><label className="field-label">RANDOM SEED<input type="number" value={scenario.seed} onChange={e=>setScenario({...scenario,seed:Number(e.target.value)})}/></label></div>
      <div className="config-note"><div className="note-icon"><Clock3 size={15}/></div><p><strong>Deterministic run</strong><br/>The same inputs and seed produce the same logical result.</p></div>
      <button className="run-button" onClick={runSimulation} disabled={loading}>{loading ? <><span className="spinner"/> RUNNING EXPERIMENT</> : <><Play size={16} fill="currentColor"/> RUN SIMULATION <ArrowUpRight size={16}/></>}</button>
      {error && <div className="error-box">{error}</div>}
      <div className="engine-foot"><span>ENGINE</span><strong>Discrete-event / virtual clock</strong><span className="engine-status">● READY</span></div>
    </section>
    <section className="results-column"><div className="results-head"><div><div className="section-kicker">02 / SIMULATION OUTPUT</div><h2>{result ? 'Run analysis' : 'Awaiting experiment'}</h2></div><span className="result-tag">{result ? 'COMPLETED' : 'NO RUN'}</span></div>
      {!result ? <div className="empty-state"><div className="empty-graphic"><div className="empty-ring ring-one"/><div className="empty-ring ring-two"/><div className="empty-core"><Activity size={26}/></div></div><h3>Your next insight starts here.</h3><p>Configure a workload and run your first experiment. Metrics and event traces will appear here.</p><div className="empty-hint"><span>TIP</span> Try 20 jobs/sec with 5 workers to start.</div></div> : <>
        <div className="metric-grid">
          <Metric icon={<Gauge size={16}/>} label="THROUGHPUT" value={`${fmt(result.throughputPerSecond)}`} unit="jobs / sec"/>
          <Metric icon={<Clock3 size={16}/>} label="P95 LATENCY" value={`${fmt(result.p95EndToEndLatencyMicros/1000)}`} unit="milliseconds"/>
          <Metric icon={<Server size={16}/>} label="PEAK BACKLOG" value={fmt(result.peakBacklog)} unit="waiting jobs"/>
          <Metric icon={<Activity size={16}/>} label="WORKER UTILIZATION" value={`${fmt(result.workerUtilization*100)}%`} unit="of available capacity"/>
        </div>
        <div className="panel chart-panel"><div className="chart-head"><div><h3>Event activity</h3><p>Captured events across virtual time</p></div><span className="chart-legend"><i/> completions</span></div><div className="chart-wrap">{chartData.length > 0 ? <ResponsiveContainer width="100%" height="100%"><AreaChart data={chartData}><defs><linearGradient id="completeFill" x1="0" y1="0" x2="0" y2="1"><stop offset="0%" stopColor="#64e5bf" stopOpacity={0.28}/><stop offset="100%" stopColor="#64e5bf" stopOpacity={0}/></linearGradient></defs><CartesianGrid stroke="#202b42" strokeDasharray="3 5" vertical={false}/><XAxis dataKey="time" stroke="#66738c" tickLine={false} axisLine={false} tickFormatter={v=>`${v}s`}/><YAxis stroke="#66738c" tickLine={false} axisLine={false} width={34}/><Tooltip contentStyle={{background:'#111a2b',border:'1px solid #293753',borderRadius:10,color:'#eaf0ff'}}/><Area type="monotone" dataKey="completed" name="Completions" stroke="#64e5bf" fill="url(#completeFill)" strokeWidth={2.2}/></AreaChart></ResponsiveContainer> : <div className="no-trace">Trace capture disabled or no events recorded.</div>}</div></div>
        <div className="panel summary-panel"><div className="chart-head"><div><h3>Execution summary</h3><p>Seed {result.seed} · {fmt(result.processedEvents)} events processed</p></div></div><div className="summary-grid"><Summary label="Generated" value={result.generatedJobs}/><Summary label="Completed" value={result.completedJobs}/><Summary label="Rejected" value={result.rejectedJobs}/><Summary label="Waiting at end" value={result.waitingAtEnd}/><Summary label="Active at end" value={result.activeAtEnd}/><Summary label="p99 latency" value={`${fmt(result.p99EndToEndLatencyMicros/1000)} ms`}/></div></div>
      </>}
    </section></div>
    <footer><span>QUEUEFLUX <i>·</i> OPEN SOURCE WORKLOAD EXPERIMENTS</span><span>BUILT FOR REPEATABILITY <i>·</i> APACHE 2.0</span></footer>
  </main>;
}
function Metric({icon,label,value,unit}:{icon:ReactNode;label:string;value:string;unit:string}) { return <div className="metric-card"><div className="metric-top">{icon}<span>{label}</span></div><div className="metric-value">{value}</div><div className="metric-unit">{unit}</div></div>; }
function Summary({label,value}:{label:string;value:string|number}) { return <div className="summary-item"><span>{label}</span><strong>{value}</strong></div>; }
