# AuraSpend Database Schema

AuraSpend uses **Room** for local SQLite storage. The database is defined in `AppDatabase.kt` and has 4 entities.

## Database Version

**Current version**: 2

Migration strategy: `fallbackToDestructiveMigration()` — destructive migrations are used. On version mismatch, the database is recreated and all data is lost.

## Entity Relationship Diagram

```
categories ──┐
              ├── transactions
              ├── budgets
              └── subscriptions
```

## Entities

### `transactions`

| Column | Type | Constraints | Notes |
|--------|------|-------------|-------|
| `id` | TEXT | PRIMARY KEY | UUID string |
| `amount` | REAL | NOT NULL | Double |
| `categoryId` | TEXT | NOT NULL | FK → categories.id |
| `note` | TEXT | NOT NULL | Description |
| `merchant` | TEXT | NULLABLE | |
| `bankName` | TEXT | NULLABLE | Source bank |
| `dateTimestamp` | INTEGER | NOT NULL | Epoch milliseconds |
| `type` | TEXT | NOT NULL | "INCOME" or "EXPENSE" |
| `isRecurring` | INTEGER | NOT NULL | 0 or 1 (boolean) |
| `recurrenceFrequency` | TEXT | NULLABLE | "DAILY"/"WEEKLY"/"MONTHLY"/"YEARLY" |
| `nextDueDateTimestamp` | INTEGER | NULLABLE | Epoch milliseconds |
| `subscriptionName` | TEXT | NULLABLE | |

### `categories`

| Column | Type | Constraints | Notes |
|--------|------|-------------|-------|
| `id` | TEXT | PRIMARY KEY | e.g. "cat_food" |
| `name` | TEXT | NOT NULL | e.g. "Food & Dining" |
| `icon` | TEXT | NOT NULL | Material icon identifier |
| `color` | INTEGER | NOT NULL | ARGB color int |
| `isDefault` | INTEGER | NOT NULL | 0 or 1 |

12 default categories are seeded on first launch.

### `budgets`

| Column | Type | Constraints | Notes |
|--------|------|-------------|-------|
| `id` | TEXT | PRIMARY KEY | UUID string |
| `categoryId` | TEXT | NOT NULL | FK → categories.id |
| `limitAmount` | REAL | NOT NULL | Spending limit |
| `spentAmount` | REAL | NOT NULL | Current spend |
| `period` | TEXT | NOT NULL | "WEEKLY"/"MONTHLY"/"YEARLY" |

### `subscriptions`

| Column | Type | Constraints | Notes |
|--------|------|-------------|-------|
| `id` | TEXT | PRIMARY KEY | UUID string |
| `name` | TEXT | NOT NULL | |
| `amount` | REAL | NOT NULL | Billing amount |
| `categoryId` | TEXT | NOT NULL | FK → categories.id |
| `billingCycle` | TEXT | NOT NULL | Same as RecurrenceFrequency |
| `nextBillingDateTimestamp` | INTEGER | NOT NULL | Epoch milliseconds |
| `active` | INTEGER | NOT NULL | 0 or 1 |

## DAOs (`Daos.kt`)

Four DAO interfaces, one per entity:

| DAO | Key Operations |
|-----|---------------|
| `TransactionDao` | Insert, delete, getAllTransactions (Flow), getByDateRange, totalIncome, totalExpense |
| `CategoryDao` | Insert (replace), getAll (Flow), delete, getDefaultCategory |
| `BudgetDao` | Insert (replace), getAll (Flow), delete |
| `SubscriptionDao` | Insert (replace), getAll (Flow), delete |

All DAOs expose `Flow<T>` for reactive observation. Write operations use `@Upsert` or `@Insert(onConflict = REPLACE)`.

## Mappers (`Mappers.kt`)

Converts between Room entities and domain models:
- `LocalDateTime` ↔ epoch millisecond `Long` for date fields
- Enum strings ↔ domain enum types (TransactionType, RecurrenceFrequency)
- Entity ↔ domain model mapping functions

## Backup Data Model

`BackupData.kt` defines a serializable container:

```kotlin
data class BackupData(
    val transactions: List<TransactionEntity>,
    val categories: List<CategoryEntity>,
    val budgets: List<BudgetEntity>,
    val subscriptions: List<SubscriptionEntity>
)
```

`BackupSerializer.kt` handles Gson-based JSON serialization/deserialization for Google Drive backup and restore.
