# CMAR Hướng Combo Cuối — H2 + H3 + HM + Top-K (v8)

**Created:** 2026-05-11 14:57
**Type:** Final combo benchmark — kết hợp 4 kỹ thuật cải tiến
**Refs:** v6 (H2+H3 winner), v7 (HM+TopK)

## Goal

Kết hợp **TẤT CẢ** 4 kỹ thuật trong 1 variant để tối đa hóa F1/Recall trên dataset imbalanced:

| Kỹ thuật | Mục đích | File |
|----------|----------|------|
| **H2** — Class-specific minSup | Sinh rules cho minority (lower sup gate) | `CrossValidator` |
| **H3** — Adaptive minConf | Giữ rules conf thấp cho minority | `CrossValidator` |
| **HM** — Harmonic Mean ranking | Balance sup vs conf trong ranking | `CMARClassifier` |
| **Top-K** — K rules per class | Cân bằng số rules giữa classes | `CMARClassifier` |

**Câu chuyện luận điểm:**
- H2+H3 sinh **được** rules cho minority (lymph fibrosis có rules)
- HM ranking → chọn rules **balanced** giữa sup và conf
- Top-K → giới hạn số rules cuối mỗi class, tránh majority áp đảo

## Hypothesis

H2+H3 alone rescue được fibrosis (F1 0→0.67). Thêm HM+TopK có thể:
- Cải thiện thêm precision của minority (nhờ select balanced rules)
- Hoặc ngược lại: cắt mất rules tốt → giảm F1

→ Cần benchmark để xác định.

## Test setup

- 5 imbalanced datasets: lymph, glass, vehicle, hepatitis, german
- K = {3, 5, 7, 10}
- H2: `classMinSupFraction = supPct`
- H3: `floor = 0.3, lift = 5.0`
- HM ranking enabled trong Top-K
- 10-fold stratified CV, seed=42

## Phases

| # | Phase | File |
|:-:|------|------|
| 01 | Create BenchmarkAll | [phase-01.md](./phase-01.md) |
| 02 | Run + compare with v6, v7, baseline | [phase-02.md](./phase-02.md) |
| 03 | Write thesis MD | [phase-03.md](./phase-03.md) |

## Files

| Action | File |
|:------:|------|
| ADD | `src/BenchmarkAll.java` |
| ADD | `result/v8_baseline_*.csv`, `result/v8_k{3,5,7,10}_*.csv` |
| ADD | `report/huong_combo_cuoi.md` (thesis ready) |

## Success Criteria

- All compile clean
- lymph fibrosis F1 > 0 với ít nhất 1 K
- Macro-F1 ít nhất 1 dataset > v6 (H2+H3 alone)
- Honest report kể cả tệ hơn
