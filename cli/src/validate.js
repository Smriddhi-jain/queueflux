import Ajv from 'ajv';
import { SCENARIO_SCHEMA } from './schema.js';

const ajv = new Ajv({ allErrors: true });

/** @type {import('ajv').ValidateFunction} */
const validateFn = ajv.compile(SCENARIO_SCHEMA);

/**
 * Validates a parsed scenario object against the JSON Schema.
 * @param {unknown} scenario
 * @returns {{ valid: boolean, errors: string[] }}
 */
export function validate(scenario) {
  const valid = validateFn(scenario);
  if (valid) return { valid: true, errors: [] };
  const errors = (validateFn.errors ?? []).map(e => {
    const path = e.instancePath || '(root)';
    return `${path}: ${e.message}`;
  });
  return { valid: false, errors };
}
