# ADR 0003 — Aggregate in SQL; bound list loads

**Status:** Accepted · 2026-09

## Context

The dashboard loaded the entire `transactions` table and scanned it repeatedly
(month totals, balance, category breakdown, seven passes for the weekly chart).
The activity list rendered every row of a date group inside one LazyColumn
item. Neither scales past a few thousand records, and every save triggered a
full reload.

## Decision

- **Aggregate in SQL.** Room exposes indexed `Flow` aggregates: range summary
  (income/expense/counts), net balance, expense-by-category and daily expense
  (`date(..., 'localtime')` grouping, DST-safe). Room re-runs them on table
  invalidation, so the dashboard stays reactive without loading rows.
- **Indices** on `transactions(dateTimestamp)`, `(categoryId)`,
  `(type, dateTimestamp)` and `budgets(categoryId)` (migration v6→v7).
- **Bound loads** for lists: `LIMIT`-based loading for the activity feed,
  being replaced by Paging 3 (`room-paging`) with an indexed `PagingSource`.
- Budget spend is derived from the same per-period category totals rather than
  stored snapshots.

## Consequences

- Home/Insights stay fast and correct as data grows.
- `BudgetSpending.withFreshSpent` is only used where a full list already
  exists (small tables, tests); new screens should use the aggregates.
- Room emits per aggregate on invalidation; several small queries replace one
  large one. If that ever becomes hot, the queries can be merged.
