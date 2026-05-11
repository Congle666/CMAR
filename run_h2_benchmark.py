#!/usr/bin/env python3
import subprocess
import re
import csv
from pathlib import Path

datasets = [
    'agaricus-lepiota', 'breast-w', 'car', 'cleve', 'crx',
    'diabetes', 'german_disc', 'german', 'glass', 'heart',
    'hepatitis', 'horse', 'iris_disc', 'iris', 'labor',
    'led7', 'lymph', 'mushroom_full', 'sonar', 'tic-tac-toe',
    'vehicle', 'video_trends_h', 'video_trends', 'waveform',
    'weather', 'wine', 'zoo_h', 'zoo'
]

print("Running H2 Benchmark on all 20 datasets...")
print("=" * 90)

results = []

for dataset in datasets:
    print(f"\n>>> Running {dataset}...")
    try:
        result = subprocess.run(
            ['java', '-Xmx512m', '-cp', 'out', 'BenchmarkClassSup', dataset],
            capture_output=True,
            text=True,
            timeout=120,
            cwd='d:\\CMAR'
        )
        
        # Find the summary line with "  => dataset_name"
        for line in result.stdout.split('\n'):
            if f'  => {dataset}' in line:
                print(f"  Result: {line.strip()}")
                results.append(line.strip())
                break
        else:
            print(f"  ERROR: Could not find result line for {dataset}")
    except subprocess.TimeoutExpired:
        print(f"  ERROR: Timeout for {dataset}")
    except Exception as e:
        print(f"  ERROR: {e}")

print("\n" + "=" * 90)
print("SUMMARY OF ALL RESULTS:")
print("=" * 90)

for line in results:
    print(line)

# Save to file
with open('d:\\CMAR\\h2_benchmark_summary.txt', 'w') as f:
    f.write('\n'.join(results))

print(f"\nSaved {len(results)} results to h2_benchmark_summary.txt")
