#!/usr/bin/env python3
import re

# Read log file
with open('d:\\CMAR\\h2_full_benchmark.log', 'r', encoding='utf-8') as f:
    content = f.read()

# Extract all result lines with "  => dataset_name"
results = re.findall(r'  => (.+)', content)

print(f"Found {len(results)} dataset results\n")
print("=" * 120)
print("Dataset Results (H2 - Class-Specific MinSup):")
print("=" * 120)

for line in results:
    print(line)

print("\n" + "=" * 120)

# Parse and calculate average accuracy
accuracies = []
for line in results:
    parts = line.split('|')
    if len(parts) >= 2:
        # Find accuracy in first part after dataset name
        match = re.search(r'(\d+\.\d+)\s+(\d+\.\d+)', parts[1])
        if match:
            acc_str = match.group(1)
            acc = float(acc_str) / 100
            accuracies.append(acc)

if accuracies:
    avg_acc = sum(accuracies) / len(accuracies)
    print(f"\nAverage Accuracy across {len(accuracies)} datasets: {avg_acc*100:.2f}%")
    print(f"Min: {min(accuracies)*100:.2f}%, Max: {max(accuracies)*100:.2f}%")

# Save to file for markdown report
with open('d:\\CMAR\\h2_parsed_results.txt', 'w') as f:
    for i, line in enumerate(results, 1):
        f.write(f"{i}. {line}\n")

print(f"\nSaved {len(results)} results to h2_parsed_results.txt")
