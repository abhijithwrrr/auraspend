# AuraSpend — Feature Enrichment, UI Redesign & Open Source Readiness Plan

Based on thorough analysis of the entire codebase, here's a comprehensive plan organized into three phases.

---

## Current State Summary

**What exists:**
- Clean Architecture + MVI (partially applied — Dashboard/Budget/Classification use MVI, but Transaction screens don't)
- 9 Indian bank SMS parsers (HDFC, ICICI, SBI, Axis, Kotak, Yes, PNB, Canara, BOB)
- Dashboard with weekly bar chart, budget progress, category breakdown
- Transaction list with search/filter/swipe-delete
- Per-category budgets (monthly/weekly/yearly)
- Recurring subscription tracking
- CSV export/import, Google Drive backup (premium)
- Savings goals (DB exists but UI is minimal)
- Analytics with donut chart and merchant insights
- Dark/AMOLED themes (premium)
- Manual DI via Application class
- Two build flavors: `free` (all features unlocked) and `play` (real IAP)

**Key gaps identified:**
- No unit/UI tests, no CI workflow file, missing CONTRIBUTING.md/SECURITY.md
- Inconsistent MVI adoption across screens
- No time-period filtering in analytics
- No tagging, no multi-currency, no PDF export, no widgets
- Premium gating contradicts open-source philosophy
- Limited animation/transition usage
- Missing KDoc documentation

---

## Phase 1: Open Source Foundation & Quality

### 1.1 Missing OSS Files
- **CONTRIBUTING.md** — Build flavor explanation, code style guide, PR process, issue labels
- **SECURITY.md** — Vulnerability reporting process
- **GitHub Issue Templates** — Bug report, feature request, question
- **PR Template** — Checklist for contributors
- **CODE_OF_CONDUCT.md** — Community guidelines
- **.github/workflows/ci.yml** — Build + test CI pipeline (the badge exists but no workflow file)

### 1.2 Code Quality & Documentation
- **License headers** on every `.kt` file (Apache 2.0)
- **KDoc** documentation on all public APIs, domain models, repository interfaces, use cases
- **Detekt** integration for static code analysis
- **ktlint** formatter configuration via `.editorconfig` (already exists, extend it)
- **Architecture Decision Records (ADR)** in `/docs/adr/` — document key design choices

### 1.3 Testing
- **Unit tests** for:
  - `BankMessageParser` (all 9 banks, edge cases)
  - `TransactionClassifier` (category assignment logic)
  - `CsvManager` (export/import roundtrip)
  - `SaveTransactionUseCase` and `ClassifyMessageUseCase`
  - All ViewModels (MVI intent handling, state transitions)
- **UI tests** for critical flows:
  - Add transaction flow
  - Classification flow
  - Dashboard rendering

### 1.4 README Enhancement
- Add screenshots/GIFs section (placeholder)
- Architecture diagram (text-based or Mermaid)
- Feature comparison table (free vs play)
- Badges for CI, license, API level, code coverage

---

## Phase 2: Feature Enrichment

### 2.1 Tagging System
- Add `tags` field to `TransactionEntity` (Room `@TypeConverter` for `List<String>`)
- Domain model: `Transaction` gets `tags: List<String>`
- UI: Tag chips on add/edit screen, tag filter in transaction list
- Migration: Room database version bump with migration

### 2.2 Spending Insights & Smart Alerts
- New screen: **InsightsScreen** accessible from dashboard
- Computed insights:
  - "You spent 23% more on Food this month vs last month"
  - "Your biggest expense this week was ₹2,400 at Swiggy"
  - "You're on track to exceed your Shopping budget by ₹1,200"
  - "3 subscriptions totaling ₹899/month — cancel any?"
- Local computation (no external API needed) — analyze trends from existing transaction data

### 2.3 Transaction Templates
- New entity: `TransactionTemplate` (id, title, amount, category, note, isIncome)
- UI: "Save as template" option on add transaction screen
- Quick-add: Templates shown as chips on dashboard for one-tap transaction creation
- Great for recurring daily expenses (coffee, commute, lunch)

### 2.4 Enhanced Savings Goals
- Improve existing `SavingsGoal` UI (DB already exists)
- Progress ring visualization (animated circular progress)
- Milestone celebrations (25%, 50%, 75%, 100%)
- Add funds to goal directly from savings screen
- Goal categories: Emergency fund, Vacation, Gadgets, Education, Custom

### 2.5 Budget Rollover
- When a budget period ends, unused amount carries over to next period
- New field: `rolloverAmount` in budget configuration
- Visual indicator showing rollover amount on budget screen
- Toggle per-budget (some users may not want rollover)

### 2.6 Multi-Currency Support
- New entity: `CurrencyRate` or use a hardcoded rate table
- Transaction model gets optional `currency` field (default: INR)
- Settings: Select default currency
- Analytics: Convert all to selected currency for display
- CSV export includes currency column

### 2.7 PDF Export
- Generate beautiful PDF reports from transaction data
- Monthly/weekly/yearly report options
- Include: summary, category breakdown, top merchants, budget status
- Use Android's built-in `PdfDocument` API (no external library needed)
- Share via system share sheet

### 2.8 Home Screen Widget
- Glance-based Compose widget (modern approach)
- Widget types:
  - **Balance Widget**: Shows current balance + today's spending
  - **Quick Add Widget**: One-tap add transaction with template
  - **Budget Widget**: Shows budget progress for selected category

### 2.9 Enhanced Search & Filter
- Date range picker (from → to)
- Amount range filter (min → max)
- Category multi-select filter
- Tag-based filtering (after tagging system)
- Search history (recent searches)
- Saved searches / presets

### 2.10 Notification Reminders
- Bill payment reminders for recurring subscriptions (configurable days before)
- Budget alert notifications (50%, 75%, 90%, 100% thresholds)
- Daily spending summary notification (optional)
- Use WorkManager for scheduling (already used for SMS classification)

### 2.11 Merchant Database
- Build a local merchant-to-category mapping
- Auto-categorize based on merchant (e.g., Zomato → Food, Uber → Transport)
- User can override and save custom mappings
- Improves classification accuracy over time

### 2.12 Recurring Transaction Auto-Detection
- Analyze transaction history for patterns (same amount + merchant at regular intervals)
- Suggest: "This looks like a recurring expense — add as subscription?"
- One-tap confirmation to create recurring entry

### 2.13 Localization (i18n)
- Extract all hardcoded strings to `strings.xml`
- Start with English + Hindi
- Community can add more languages via PRs
- RTL support consideration

---

## Phase 3: UI Redesign

### 3.1 Design System Overhaul
- **New color palette**: Move from default M3 to a custom brand palette
  - Primary: Deep Indigo (#3F51B5) with vibrant accent (Amber #FFC107)
  - Semantic colors: Income green, Expense red, Budget blue, Savings purple
- **Typography scale**: Use M3 Type Scale properly with display, headline, title, body, label styles
- **Spacing system**: Consistent 4dp grid with named spacing tokens
- **Elevation & shadows**: Subtle, modern elevation using M3 surface tint

### 3.2 Bottom Navigation Redesign
- 4 tabs instead of current setup:
  1. **Home** (Dashboard + Insights) — Home icon
  2. **Transactions** (List + Search) — Receipt icon
  3. **Analytics** (Charts + Reports) — Chart icon
  4. **Settings** (All config) — Gear icon
- FAB (Floating Action Button) for **+ Add Transaction** — always visible, centered above nav bar
- Animated tab transitions with shared element transitions

### 3.3 Dashboard Redesign
- **Hero Section**: Large balance card with animated counter, income/expense summary below
- **Quick Actions Row**: Horizontal scrollable chips — "Add Expense", "Add Income", "Scan SMS", "Templates"
- **Spending Overview**: Improved bar chart with tap-to-see-details, week/month toggle
- **Active Budgets**: Horizontal scrollable budget cards with circular progress
- **Recent Transactions**: Last 5 transactions with "See All" link
- **Insights Card**: Latest smart insight with tap to view all
- **Subscriptions Summary**: Monthly total with list of upcoming renewals

### 3.4 Add Transaction Redesign
- **Bottom Sheet** instead of full screen (for quick adds)
- Full screen for detailed entry
- **Category Grid**: Visual grid with icons and colors (2 columns, scrollable)
- **Amount Input**: Large, prominent number pad-style input
- **Quick Options**: "Recurring?", "Add Tag", "Attach to Goal"
- **Template Save**: Toggle to save as template
- **Recent Merchants**: Auto-suggest from merchant database

### 3.5 Transaction List Redesign
- **Sticky Headers**: Date group headers that stick while scrolling
- **Rich Cards**: Category icon + color, merchant name, amount (green/red), note snippet
- **Swipe Actions**: Left swipe = Delete (with undo snackbar), Right swipe = Edit
- **Selection Mode**: Multi-select for bulk delete/categorize/export
- **Filter Bar**: Horizontal scrollable chips — All, Income, Expense, [Categories], [Tags]
- **Sort Options**: Date (newest/oldest), Amount (high/low), Category

### 3.6 Analytics Redesign
- **Time Period Tabs**: Today, This Week, This Month, This Year, Custom Range
- **Summary Cards**: Income, Expenses, Savings Rate, Net — with trend arrows (up/down)
- **Line Chart**: Spending trend over time (daily granularity)
- **Donut Chart**: Category breakdown (interactive — tap segment for details)
- **Category List**: Sorted by spending with amounts, percentages, and mini bar charts
- **Merchant Insights**: Top merchants with transaction counts
- **Comparison**: This month vs last month (percentage change per category)

### 3.7 Settings Redesign
- **Grouped Sections** with clear headers:
  - **Appearance**: Theme (Light/Dark/AMOLED), Accent Color picker
  - **Data**: CSV Export/Import, PDF Export, Google Drive Backup
  - **Categories**: Manage categories with drag-to-reorder
  - **Budgets**: Manage budgets
  - **Subscriptions**: Manage recurring entries
  - **Notifications**: Bill reminders, Budget alerts
  - **About**: Version, License, Open Source credits, GitHub link
- **Profile Section**: Optional name/currency setup at top

### 3.8 Animations & Transitions
- **Shared Element Transitions**: Transaction card → Transaction detail
- **Animated Lists**: Items animate in on scroll (LazyColumn `animateItemPlacement`)
- **Micro-interactions**: Button press feedback, toggle animations
- **Chart Animations**: Donut/bar charts animate on first render
- **Pull-to-Refresh**: Animated refresh indicator
- **Lottie**: Keep existing onboarding/splash animations

### 3.9 Empty & Loading States
- **Empty States**: Illustration + message + action button for each screen
  - No transactions: "Start tracking your expenses" + Add button
  - No budgets: "Set your first budget" + Create button
  - No subscriptions: "Track your recurring expenses" + Add button
- **Loading States**: Skeleton screens (shimmer effect) instead of generic CircularProgressIndicator
- **Error States**: Friendly error messages with retry buttons

### 3.10 Onboarding Redesign
- Expand to 4-5 pages covering key features
- Interactive elements (not just static Lottie)
- Final page: Quick setup — select currency, choose categories, set first budget
- Skip option always visible

---

## Implementation Priority Recommendation

For maximum impact as an open-source project, I recommend this order:

1. **Phase 1** first (OSS foundation) — makes the project contributor-friendly
2. **Phase 3 UI Redesign** — makes the app visually impressive for screenshots/stars
3. **Phase 2 Features** — adds substance behind the polish

### Quick Wins (High Impact, Low Effort)
- Open source files (CONTRIBUTING.md, templates, CI)
- Transaction Templates
- Enhanced Savings Goals UI
- Search enhancements
- Empty states & loading states
- MVI consistency across all screens
- KDoc documentation

### Medium Effort
- Tagging system
- Spending Insights
- PDF Export
- Notification Reminders
- Dashboard redesign
- Analytics redesign
- Localization foundation

### High Effort
- Home screen widget
- Multi-currency support
- Merchant database
- Recurring auto-detection
- Budget rollover

---

## Decisions Needed Before Implementation

1. **Scope**: Should everything be implemented, or focus on specific areas?
2. **Premium model**: For open source, should all features be free (remove premium gating), or keep the free/play flavor split for Play Store monetization?
3. **UI priority**: Which screens matter most to redesign first?
4. **DI refactor**: Should manual DI be replaced with Hilt/Koin for better testability?