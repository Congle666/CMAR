# CMAR Hướng Cải Tiến — HM Ranking + Top-K Combo

**Created:** 2026-05-11
**Type:** Algorithm improvement combining WCBA-inspired HM ranking + Top-K per class
**Refs:** Paper "WCBA: Weighted classification based on association rules" (Alwidian et al., 2018)

## Goal

Khắc phục 2 vấn đề CMAR baseline đồng thời:
1. **minConf cứng** chặn rules tốt cho minority (lymph fibrosis F1=0)
2. **Toàn bộ rules** dùng để classify → noise từ class đa số áp đảo minority

## Hai kỹ thuật kết hợp

### A. Harmonic Mean (HM) Ranking — từ WCBA 2018
```
HM(R) = 2 × sup(R) × conf(R) / (sup(R) + conf(R))
```
- Thay tiêu chí ranking từ χ² → HM
- HM cao chỉ khi CẢ sup VÀ conf đều cao
- Parameter-free (không cần tune per-dataset)

### D. Top-K Rules Per Class
- Mỗi class chọn K luật **HM cao nhất**
- K global = {3, 5, 7, 10}
- Tự cân bằng majority vs minority

### Kết hợp: Top-K by HM
1. Sau pruning 3-tier CMAR, sort rules per class **theo HM giảm dần**
2. Mỗi class lấy top K → tổng = K × số_class luật cuối
3. Classify dùng tập rules balanced

## Datasets (imbalanced focus)

| Dataset | N | Classes | Imbalance |
|---------|:--:|:--:|-----------|
| lymph | 148 | 4 | fibrosis 2.7%, normal 1.4% (extreme) |
| glass | 214 | 6 | containers 4.2%, vehicle_float 7.9% |
| vehicle | 846 | 4 | tương đối cân bằng nhưng confusion cao |
| hepatitis | 155 | 2 | DIE 20.6% |
| german | 1000 | 2 | bad 30% |

## Phases

| # | Phase | File |
|:-:|------|------|
| 01 | Add HM field to AssociationRule | [phase-01.md](./phase-01.md) |
| 02 | Modify CMARClassifier — useHMRanking flag | [phase-02.md](./phase-02.md) |
| 03 | Create BenchmarkHMTopK | [phase-03.md](./phase-03.md) |
| 04 | Run + write report | [phase-04.md](./phase-04.md) |

## Files

| Action | File |
|:------:|------|
| EDIT | `src/AssociationRule.java` — add hm field + getter |
| EDIT | `src/CMARClassifier.java` — sort top-K by HM (option) |
| ADD | `src/BenchmarkHMTopK.java` |
| ADD | `result/v7_k{K}_hm_metrics.csv` for K=3,5,7,10 |
| ADD | `report/huong_HM_TopK.md` (thesis writeup) |

## Success Criteria

- Compile clean, no regression on Main
- **lymph fibrosis F1 > 0** trên ít nhất 1 K value
- Macro-F1 ≥ baseline cho mọi dataset imbalanced
- Report rõ K nào tốt nhất per dataset
