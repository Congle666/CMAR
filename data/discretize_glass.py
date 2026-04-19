"""
Discretize the UCI Glass dataset using 5 equal-frequency (quintile) bins.
Features with too few unique values get fewer bins automatically.
"""
import csv
import math

# Read raw data
rows = []
with open("glass.data", "r") as f:
    for line in f:
        line = line.strip()
        if not line:
            continue
        parts = line.split(",")
        # Skip column 0 (ID), columns 1-9 are features, column 10 is class
        features = [float(x) for x in parts[1:10]]
        cls = int(parts[10])
        rows.append((features, cls))

print(f"Loaded {len(rows)} records")

feature_names = ["RI", "Na", "Mg", "Al", "Si", "K", "Ca", "Ba", "Fe"]
bin_labels_5 = ["VL", "L", "M", "H", "VH"]
bin_labels_4 = ["VL", "L", "H", "VH"]
bin_labels_3 = ["L", "M", "H"]
bin_labels_2 = ["L", "H"]

class_map = {
    1: "building_float",
    2: "building_nonfloat",
    3: "vehicle_float",
    5: "vehicle_nonfloat",
    6: "containers",
    7: "headlamps",
}

n = len(rows)

# For each feature, compute quintile boundaries and discretize
discretized = [[] for _ in range(n)]

for fi in range(9):
    values = [rows[i][0][fi] for i in range(n)]
    unique_vals = sorted(set(values))
    n_unique = len(unique_vals)

    # Decide number of bins based on unique values
    if n_unique <= 3:
        n_bins = n_unique
    elif n_unique <= 6:
        n_bins = min(3, n_unique)
    elif n_unique <= 10:
        n_bins = 4
    else:
        n_bins = 5

    if n_bins == 5:
        labels = bin_labels_5
    elif n_bins == 4:
        labels = bin_labels_4
    elif n_bins == 3:
        labels = bin_labels_3
    else:
        labels = bin_labels_2

    # Compute quantile boundaries for equal-frequency bins
    sorted_vals = sorted(values)
    boundaries = []
    for b in range(1, n_bins):
        # Index for the b-th quantile
        idx = b * n / n_bins
        lower = sorted_vals[int(math.floor(idx)) - 1] if int(math.floor(idx)) > 0 else sorted_vals[0]
        upper = sorted_vals[min(int(math.ceil(idx)), n - 1)]
        boundary = (lower + upper) / 2.0
        boundaries.append(boundary)

    print(f"{feature_names[fi]}: {n_unique} unique values, {n_bins} bins, boundaries={[f'{b:.4f}' for b in boundaries]}")

    # Assign bins
    for i in range(n):
        v = values[i]
        assigned = labels[-1]  # default to highest bin
        for bi, boundary in enumerate(boundaries):
            if v <= boundary:
                assigned = labels[bi]
                break
        discretized[i].append(assigned)

# Write output
with open("glass.csv", "w", newline="") as f:
    writer = csv.writer(f)
    writer.writerow(feature_names + ["class"])
    for i in range(n):
        cls_label = class_map[rows[i][1]]
        writer.writerow(discretized[i] + [cls_label])

print(f"Wrote glass.csv with {n} records + header")

# Verify
with open("glass.csv", "r") as f:
    lines = f.readlines()
print(f"Verification: {len(lines)} lines total ({len(lines)-1} data records)")

# Show class distribution
from collections import Counter
classes = [class_map[rows[i][1]] for i in range(n)]
print("\nClass distribution:")
for cls, count in sorted(Counter(classes).items(), key=lambda x: -x[1]):
    print(f"  {cls}: {count}")

# Show bin distribution per feature
print("\nBin distributions:")
for fi in range(9):
    col = [discretized[i][fi] for i in range(n)]
    dist = Counter(col)
    print(f"  {feature_names[fi]}: {dict(sorted(dist.items()))}")
