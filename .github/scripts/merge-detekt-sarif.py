#!/usr/bin/env python3
"""Merge version-specific Detekt reports into one GitHub code-scanning upload."""

import copy
import json
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
REPORTS = sorted(ROOT.glob("versions/*/build/reports/detekt/*.sarif"))
OUTPUT = ROOT / "sarif-output/detekt.sarif"
GENERATED_PREFIX = "/build/generated/bloom/main/kotlin/"
PREPROCESSED_PREFIX = "/build/preprocessed/main/kotlin/"


def mc_version(report: Path) -> str:
    return report.relative_to(ROOT).parts[1]


def normalize_source_paths(result: dict) -> None:
    for location in result.get("locations", []):
        physical = location.get("physicalLocation", {})
        artifact = physical.get("artifactLocation", {})
        uri = artifact.get("uri", "")
        for prefix in (GENERATED_PREFIX, PREPROCESSED_PREFIX):
            if prefix in uri:
                _, suffix = uri.split(prefix, 1)
                artifact["uri"] = f"src/main/kotlin/{suffix}"
                break


def finding_key(result: dict) -> str:
    primary = result.get("locations", [{}])[0].get("physicalLocation", {})
    artifact = primary.get("artifactLocation", {})
    region = primary.get("region", {})
    return json.dumps(
        {
            "ruleId": result.get("ruleId"),
            "uri": artifact.get("uri"),
            "line": region.get("startLine"),
        },
        sort_keys=True,
    )


def main() -> None:
    if not REPORTS:
        return

    merged = json.loads(REPORTS[0].read_text())
    run = merged["runs"][0]
    driver = run["tool"]["driver"]
    rules = {rule["id"]: rule for rule in driver.get("rules", [])}
    results = []
    seen_versions: dict[str, set[str]] = {}

    for report in REPORTS:
        document = json.loads(report.read_text())
        version = mc_version(report)
        for source_run in document.get("runs", []):
            for rule in source_run.get("tool", {}).get("driver", {}).get("rules", []):
                rules.setdefault(rule["id"], rule)

            for original in source_run.get("results", []):
                result = copy.deepcopy(original)
                normalize_source_paths(result)
                key = finding_key(result)
                versions = seen_versions.setdefault(key, set())
                if version in versions:
                    # Preserve repeated results within one analysis version.
                    results.append(result)
                elif versions:
                    # The same rule and source line was reported by another
                    # MC version; retain its first occurrence.
                    versions.add(version)
                else:
                    versions.add(version)
                    results.append(result)

    driver["rules"] = list(rules.values())
    run["results"] = results
    OUTPUT.parent.mkdir(parents=True, exist_ok=True)
    OUTPUT.write_text(json.dumps(merged, indent=2) + "\n")
    print(f"Merged {len(REPORTS)} Detekt SARIF reports into {OUTPUT.relative_to(ROOT)}")


if __name__ == "__main__":
    main()
