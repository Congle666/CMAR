# CMAR + WCBA Proper Implementation (v9)

**Created:** 2026-05-11 16:54
**Type:** Comprehensive improvement — implement true WCBA components + cleanup

## Goal

Áp dụng đầy đủ ý tưởng WCBA 2018 vào CMAR, kết hợp với các hướng đã có (H2, H3, HM, Top-K) để tối ưu F1/Recall trên **truly imbalanced datasets** (max/min class ratio ≥ 3x).

## Triết lý

**Honest scope:**
- ✅ **Truly imbalanced:** lymph (40x), hepatitis (3.8x), german (2.3x)
- ⚠️ **Pseudo-imbalanced/hard cases:** glass (data-scarcity), vehicle (near-balanced, semantic confusion)
- Focus thesis vào truly imbalanced — KHÔNG cố ép vehicle improve (sai bản chất bài toán)

## 4 Thành phần WCBA chuẩn

| # | Component | Status hiện tại | Cần làm |
|:-:|-----------|:---------------:|---------|
| 1 | Attribute weighting (auto) | ❌ | **Information Gain → weight ∈ [1, 10]** |
| 2 | Weighted Support | ❌ | `weighted_sup = avg(item_weights) × support` |
| 3 | HM Ranking | ✅ (v7) | Đã có — combine với weighted_sup |
| 4 | Strong + Spare rules | ❌ | Split sau coverage; fallback strong→spare→default |

**+ Bonus:** Stratified Top-K — K thích nghi theo class size

## Phases

| # | Phase | Mô tả |
|:-:|-------|-------|
| 01 | Cleanup | Xoá dead files trong src/ và root |
| 02 | AttributeWeights.java | IG-based automatic attribute weighting |
| 03 | Weighted Support trong FPGrowth | Option để dùng weighted_sup thay sup |
| 04 | Strong + Spare rules | Split sau coverage, fallback predict |
| 05 | Stratified Top-K | K(c) thích nghi theo class size |
| 06 | BenchmarkWCBA | Combine all + run trên truly imbalanced |
| 07 | Thesis MD | Báo cáo chỉn chu |

## Truly Imbalanced Datasets (Final Set)

| Dataset | N | Classes | Max/Min Ratio | Note |
|---------|:--:|:--:|:----------:|------|
| **lymph** | 148 | 4 | 40.5x | Extreme — primary target |
| **hepatitis** | 155 | 2 | 3.8x | Medical |
| **german** | 1000 | 2 | 2.3x | Financial |

Discussion-only (not improvement target):
- glass (data scarcity)
- vehicle (near-balanced, semantic confusion)

## Success Criteria

- All compile clean
- WCBA proper components implemented
- Run on 3 truly imbalanced datasets
- F1/Recall **improve trên TẤT CẢ 3 truly imbalanced** (vs baseline)
- Thesis MD đầy đủ, honest về limitations

## Files

| Action | File |
|:------:|------|
| DELETE | `src/BenchmarkPhase1.java`, `BenchmarkPhase2.java`, `BenchmarkH1H2.java`, `BenchmarkTopK.java`, `ClassDistributionReport.java` |
| DELETE | `src/benchmark_results.txt`, `src/benchmark_phase1_results.txt` |
| DELETE | `parse_h2_results.py`, `run_h2_benchmark.py`, `benchmark_h2_results.txt`, `test_topk.bat`, `results_phase1.txt`, `results_phase2.txt` |
| ADD | `src/AttributeWeights.java` |
| EDIT | `src/FPGrowth.java` — add weighted_sup option |
| EDIT | `src/CMARClassifier.java` — Strong/Spare split, Stratified TopK |
| ADD | `src/BenchmarkWCBA.java` |
| ADD | `result/v9_*.csv` |
| ADD | `report/huong_WCBA_chuan.md` |
