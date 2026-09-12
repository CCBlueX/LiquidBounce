#!/usr/bin/env node
/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2026 CCBlueX
 *
 * LiquidBounce is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LiquidBounce is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LiquidBounce. If not, see <https://www.gnu.org/licenses/>.
 */

import { readFileSync, readdirSync } from "node:fs";
import { join, dirname } from "node:path";
import { fileURLToPath } from "node:url";

const __dirname = dirname(fileURLToPath(import.meta.url));
const LANG_DIR = join(__dirname, "..", "src", "main", "resources", "resources", "liquidbounce", "lang");
const BASELINE = "en_us.json";

// Extract the format placeholders (%s, %d, %1$s, %%) from a translated string.
// `%%` is an escaped literal percent sign and must not be counted as a placeholder.
function extractPlaceholders(str) {
    return [...str.matchAll(/%((\d+)\$)?[sd]/g)].map((m) => m[0]);
}

function loadJson(path) {
    return JSON.parse(readFileSync(path, "utf-8"));
}

function readArgs() {
    const args = process.argv.slice(2);
    return {
        json: args.includes("--json"),
        strict: args.includes("--strict"),
        help: args.includes("--help") || args.includes("-h"),
    };
}

function printHelp() {
    console.log(`Usage: node scripts/verify-i18n.mjs [options]

Validates all translation files in ${LANG_DIR} against ${BASELINE} (the baseline).

Options:
  --json     Emit a machine-readable JSON report instead of a human-readable one.
  --strict   Treat extra keys (keys present in a translation but not in the
             baseline) as a failure. By default extra keys are only warnings.
  -h, --help Show this help.

Exit codes:
  0  No problems (or only extra-key warnings without --strict).
  1  Missing keys or placeholder mismatches found (or extra keys with --strict).
  2  Baseline file not found or a translation file failed to parse.
`);
}

function checkLanguage(baseline, baselineName, fileName, strict) {
    const name = fileName.replace(/\.json$/, "");
    const data = loadJson(join(LANG_DIR, fileName));
    const problems = [];

    const missingKeys = Object.keys(baseline).filter((key) => !(key in data));
    const extraKeys = Object.keys(data).filter((key) => !(key in baseline));
    const placeholderMismatches = [];

    for (const [key, baselineValue] of Object.entries(baseline)) {
        if (!(key in data) || typeof baselineValue !== "string") {
            continue;
        }
        const targetValue = data[key];
        if (typeof targetValue !== "string") {
            continue;
        }
        const baselinePh = extractPlaceholders(baselineValue);
        const targetPh = extractPlaceholders(targetValue);
        if (baselinePh.length !== targetPh.length) {
            placeholderMismatches.push({ key, baseline: baselineValue, target: targetValue, baselinePh, targetPh });
        }
    }

    if (missingKeys.length > 0) {
        problems.push({ type: "missing", count: missingKeys.length, keys: missingKeys });
    }
    if (extraKeys.length > 0) {
        problems.push({ type: "extra", count: extraKeys.length, keys: extraKeys });
    }
    if (placeholderMismatches.length > 0) {
        problems.push({ type: "placeholder", count: placeholderMismatches.length, entries: placeholderMismatches });
    }

    const hasErrors = missingKeys.length > 0 || placeholderMismatches.length > 0 || (strict && extraKeys.length > 0);

    return { name, fileName, hasErrors, problems };
}

function main() {
    const args = readArgs();
    if (args.help) {
        printHelp();
        process.exit(0);
    }

    let baseline;
    let baselinePath = join(LANG_DIR, BASELINE);
    try {
        baseline = loadJson(baselinePath);
    } catch {
        console.error(`[error] Baseline file not found or invalid: ${baselinePath}`);
        process.exit(2);
    }

    const files = readdirSync(LANG_DIR)
        .filter((f) => f.endsWith(".json"))
        .sort();

    const results = [];
    let fatal = false;

    for (const fileName of files) {
        if (fileName === BASELINE) {
            continue;
        }
        try {
            results.push(checkLanguage(baseline, BASELINE, fileName, args.strict));
        } catch (err) {
            fatal = true;
            console.error(`[error] Failed to parse ${fileName}: ${err.message}`);
        }
    }

    if (args.json) {
        console.log(JSON.stringify(results, null, 2));
    } else {
        for (const result of results) {
            const totalProblems = result.problems.reduce((sum, p) => sum + p.count, 0);
            const status = result.hasErrors ? "FAIL" : result.problems.length > 0 ? "WARN" : "OK  ";
            console.log(`${status} ${result.name.padEnd(8)} ${totalProblems} issue(s)`);

            for (const problem of result.problems) {
                if (problem.type === "missing") {
                    console.log(`      missing ${problem.count} key(s): ${problem.keys.slice(0, 5).join(", ")}${problem.count > 5 ? ", ..." : ""}`);
                } else if (problem.type === "extra") {
                    console.log(`      extra   ${problem.count} key(s): ${problem.keys.slice(0, 5).join(", ")}${problem.count > 5 ? ", ..." : ""}`);
                } else if (problem.type === "placeholder") {
                    for (const entry of problem.entries.slice(0, 3)) {
                        console.log(`      placeholder mismatch in ${entry.key}`);
                        console.log(`        baseline: ${entry.baseline}`);
                        console.log(`        target:   ${entry.target}`);
                    }
                    if (problem.count > 3) {
                        console.log(`        ... and ${problem.count - 3} more`);
                    }
                }
            }
        }
    }

    const anyErrors = results.some((r) => r.hasErrors);
    process.exit(fatal ? 2 : anyErrors ? 1 : 0);
}

main();
