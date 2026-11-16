#!/usr/bin/env node
import { readFileSync } from 'node:fs';
import { resolve } from 'node:path';
import { program } from 'commander';
import { validate } from '../src/validate.js';
import { submitRun, NetworkError, ApiError, TimeoutError } from '../src/run.js';
import { formatHuman, formatJson } from '../src/format.js';

const EXIT = {
  OK: 0,
  VALIDATION_ERROR: 1,
  RUN_FAILED: 1,
  IO_OR_NETWORK_ERROR: 2,
  TIMEOUT: 3,
};

program
  .name('qf')
  .description('QueueFlux CLI — validate and run simulation scenarios')
  .version('0.1.0');

program
  .command('validate <file>')
  .description('Validate a scenario file against the JSON Schema without submitting it')
  .action((file) => {
    const scenario = readScenarioOrExit(file);
    const { valid, errors } = validate(scenario);
    if (valid) {
      console.log(`✓ ${file} is valid`);
      process.exit(EXIT.OK);
    } else {
      console.error(`✗ ${file} has ${errors.length} validation error(s):`);
      for (const err of errors) {
        console.error(`  • ${err}`);
      }
      process.exit(EXIT.VALIDATION_ERROR);
    }
  });

program
  .command('run <file>')
  .description('Validate, submit, and optionally await a simulation run')
  .option('--api <url>', 'API base URL', 'http://localhost:8080')
  .option('--no-wait', 'Submit and exit without polling for completion')
  .option('--timeout <seconds>', 'Max seconds to wait for completion', '60')
  .option('--json', 'Output results as JSON')
  .action(async (file, opts) => {
    const scenario = readScenarioOrExit(file);

    const { valid, errors } = validate(scenario);
    if (!valid) {
      console.error(`✗ ${file} has ${errors.length} validation error(s):`);
      for (const err of errors) {
        console.error(`  • ${err}`);
      }
      process.exit(EXIT.VALIDATION_ERROR);
    }

    const timeoutSeconds = parseInt(opts.timeout, 10);
    if (isNaN(timeoutSeconds) || timeoutSeconds < 1) {
      console.error('Error: --timeout must be a positive integer');
      process.exit(EXIT.IO_OR_NETWORK_ERROR);
    }

    let outcome;
    try {
      outcome = await submitRun(scenario, {
        apiUrl: opts.api,
        wait: opts.wait,
        timeoutSeconds,
      });
    } catch (err) {
      if (err instanceof NetworkError) {
        console.error(`Network error: ${err.message}`);
        process.exit(EXIT.IO_OR_NETWORK_ERROR);
      }
      if (err instanceof ApiError) {
        console.error(`API error: ${err.message}`);
        process.exit(EXIT.IO_OR_NETWORK_ERROR);
      }
      if (err instanceof TimeoutError) {
        console.error(`Timeout: ${err.message}`);
        process.exit(EXIT.TIMEOUT);
      }
      throw err;
    }

    if (!opts.wait) {
      const out = { id: outcome.id, status: outcome.status };
      if (opts.json) {
        console.log(JSON.stringify(out, null, 2));
      } else {
        console.log(`Submitted run ${outcome.id} (status: ${outcome.status})`);
      }
      process.exit(EXIT.OK);
    }

    if (opts.json) {
      console.log(formatJson(outcome.snapshot));
    } else {
      console.log(formatHuman(outcome.snapshot));
    }

    if (outcome.status === 'COMPLETED') {
      process.exit(EXIT.OK);
    } else {
      process.exit(EXIT.RUN_FAILED);
    }
  });

program.parse();

function readScenarioOrExit(file) {
  const absPath = resolve(file);
  let raw;
  try {
    raw = readFileSync(absPath, 'utf8');
  } catch (err) {
    console.error(`Error reading ${file}: ${err.message}`);
    process.exit(EXIT.IO_OR_NETWORK_ERROR);
  }
  try {
    return JSON.parse(raw);
  } catch (err) {
    console.error(`Error parsing ${file}: ${err.message}`);
    process.exit(EXIT.IO_OR_NETWORK_ERROR);
  }
}
