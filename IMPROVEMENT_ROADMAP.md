# AuraSpend: Comprehensive Improvement Roadmap
## World-Class Expense Management Platform

**Project:** AuraSpend (Open-Source Expense Manager)  
**Focus Areas:** Categorization Excellence & Core Functionality  
**Last Updated:** 2026-08-07

---

## Executive Summary

This roadmap outlines a strategic plan to elevate AuraSpend from a functional expense manager to a world-class platform. The plan prioritizes smart categorization (the differentiator), robust core functionality, and an exceptional user experience that competes with premium offerings.

### Key Principles
- **Categorization-First:** Intelligent, accurate, multi-modal transaction classification
- **Data Integrity:** Bulletproof transaction recording and state management
- **User Delight:** Friction-free workflows, beautiful UI, instant feedback
- **Extensibility:** Plugin architecture for future features (forecasting, tax integration, etc.)

---

## Phase 1: Smart Categorization (Foundation) — Priority: CRITICAL

### 1.1 Enhanced AI-Driven Classification

**Current State:** On-device Llama-based categorization with regex fallback

**Improvements:**

#### a) Multi-Modal Classification Pipeline
- **Merge multiple signals:** Combine merchant name + amount + frequency + historical patterns
- **Implement confidence scoring:** Show users "80% confident: Food" with override capability
- **Add merchant learning:** Store user corrections and auto-apply to similar transactions
- **Regex + AI + Rule-Based Fusion:** Use best-of-three approach:
  - Fast regex parsing for obvious patterns
  - LLM for edge cases and nuance
  - Explicit rules for critical categories (taxes, salary, subscriptions)

#### b) Merchant Database
- **Build internal merchant KB:** 2000+ merchant → category mappings for India
- **Location-aware categorization:** Same merchant name, different categories based on sub-merchant
- **Alias resolution:** "Swiggy" = "Food", "Swiggy Instamart" = "Groceries"
- **Integration with open data:** Link to merchant public data (GST, category tags)

**Implementation:**
- [ ] Enhance LLM prompt with contextual instructions (rules, examples, edge cases)
- [ ] Build merchant alias/category master data (CSV → SQLite at install time)
- [ ] Add A/B testing framework for classification improvements
- [ ] Create user feedback loop (swipe to correct) → model retraining pipeline
- [ ] Implement confidence thresholds (if <70%, request user input; if >90%, auto-categorize)

---

### 1.2 Subscription Detection (Recurring Transactions)

**Current State:** Basic is-subscription boolean flag

**Improvements:**

#### a) Advanced Recurrence Detection
- **Analyze historical patterns:** ML model detects 15-day, 30-day, weekly cycles
- **Amount variance tolerance:** Handle subscriptions with slight amount fluctuations (e.g., 499–529 for Spotify)
- **Smart notification:** Alert users to detected subscriptions before they accumulate
- **Subscription categories:** Separate buckets for SaaS, entertainment, insurance, utilities

#### b) Forecasting
- **Predict next billing date** with confidence
- **Annual cost projection** at transaction entry time
- **Trend analysis:** Show YoY subscription growth

**Implementation:**
- [ ] Implement FFT (Fast Fourier Transform) or simpler cycle detection for recurring patterns
- [ ] Build subscription ML classifier (historical data → is_subscription + confidence)
- [ ] Add "Mark as Recurring" quick-action when reviewing transactions
- [ ] Store subscription metadata (billing date, cycle, provider, category)

---

### 1.3 Category Hierarchy & Custom Categories

**Current State:** Flat category list

**Improvements:**

#### a) Hierarchical Categories
- **Parent → Child structure:**
  - Transport → Auto Fuel, Public Transit, Cab Services, Parking
  - Food → Restaurant, Groceries, Cafe, Food Delivery
  - Health → Medical, Pharmacy, Gym, Mental Health
- **Smart suggestions:** Show relevant sub-categories based on merchant
- **User customization:** Allow creation of personal sub-categories

#### b) Flexible Categorization
- **Multi-label support:** "Uber Eats" = both "Transport" (distance) + "Food" (good type)
- **Custom rules engine:** "If amount > 5000 AND contains 'hotel', categorize as 'Travel'"
- **Tag-based fallback:** If categorization fails, tag with merchant + let user decide

**Implementation:**
- [ ] Redesign database schema for hierarchical categories
- [ ] Add custom category CRUD endpoints
- [ ] Build category suggestion engine based on merchant + historical patterns
- [ ] Create UI for category hierarchy browser and quick-selection

---

## Phase 2: Core Functionality Excellence — Priority: HIGH

### 2.1 Transaction Management

**Current State:** Basic CRUD with SMS parsing, manual entry

**Improvements:**

#### a) Rich Transaction Data
- **Attachment support:** Receipt photos, invoice PDFs (store in encrypted cloud or local)
- **Split transactions:** "Paid 5000 for groceries + ATM fee" → auto-split or manual split
- **Tags & notes:** Structured metadata (project, person, reason codes)
- **Reimbursement tracking:** Flag as "to-collect" with balance tracking
- **Duplicate detection:** Auto-flag potential duplicates (same amount, merchant within 1 hour)

#### b) Multi-Account Management
- **Support multiple bank accounts:** Track across HDFC, ICICI, personal account, etc.
- **Account sync status:** Show last sync time, pending transactions
- **Transfer between accounts:** Internal transfers flagged separately from expenses

#### c) Data Import/Export Excellence
- **Multi-format import:** CSV, OFX, PDF bank statements (OCR-based)
- **Smart date parsing:** Handle all date formats (DD/MM/YY, MM/DD/YYYY, "2 days ago")
- **Duplicate detection on import:** Prevent re-importing already-recorded transactions
- **Export for taxes:** Generate categorized P&L reports for ITR filing

**Implementation:**
- [ ] Extend Transaction model with attachments, tags, reimbursement status
- [ ] Build receipt OCR pipeline (integrate Firebase ML Kit or Tesseract)
- [ ] Implement duplicate detection (hash by merchant + amount + date ± 1 day)
- [ ] Create import wizard with preview + mapping
- [ ] Add export to PDF/Excel with formatted reports

---

### 2.2 Budget Management (Smart Limits)

**Current State:** Simple per-category limit

**Improvements:**

#### a) Adaptive Budgets
- **Rolling budgets:** Budget resets on configurable day (1st, 15th, custom date)
- **Budget flex:** +/- 10% variance with warnings before exceeding
- **Historical comparison:** "You're at 80% of usual June spend"
- **ML-based recommendations:** "Based on your 6-month average, consider Food budget of ₹8500"
- **Budget scenarios:** Create multiple budget sets (low-spend vs. normal vs. vacation mode)

#### b) Alerts & Notifications
- **Smart thresholds:** Alert at 50%, 75%, 90%, 100% of budget
- **Category-specific nudges:** "You're on pace to exceed Entertainment by 15%"
- **Smart notifications:** Don't spam; batch alerts or deliver once daily
- **Notification preferences:** Push, SMS, digest email options

**Implementation:**
- [ ] Redesign Budget model with rolling date, flex %, budget scenarios
- [ ] Build notification engine with throttling + batching
- [ ] Implement budget recommendation engine (statistical analysis of historical spend)
- [ ] Add budget scenario switcher UI
- [ ] Create notification preference center

---

### 2.3 Analytics & Insights (Premium Feature Set)

**Current State:** Basic charts (bar chart by category)

**Improvements:**

#### a) Visual Analytics Suite
- **Spending trends:** Line chart of daily/weekly/monthly spend with MA (moving average)
- **Category breakdown:** Pie/donut chart, with drill-down capability
- **Income vs. Expense:** Dual-axis chart tracking net cashflow
- **Heatmap by time:** Show spending patterns by day-of-week and time-of-month
- **Comparison views:** Month-over-month, YoY, vs. budget

#### b) Actionable Insights
- **Smart alerts:** "Your subscriptions grew 40% YoY"
- **Spending anomalies:** "Unusually high Food spending this week"
- **Saving opportunities:** "If you reduce eating out by 20%, save ₹5000/month"
- **Cashflow forecasting:** "At this rate, you'll have ₹X in 3 months"
- **Tax insights:** Categorized spending for potential tax deductions

#### c) Reports
- **Monthly reports:** Auto-generated P&L, spending breakdown, top merchants
- **Tax-ready reports:** Categorized income/expenses for ITR filing
- **Custom reports:** User-defined date ranges and filters
- **PDF export:** Shareable, formatted reports

**Implementation:**
- [ ] Integrate charting library (MPAndroidChart or Jetpack Compose charting)
- [ ] Build analytics engine with aggregation queries (optimized indexes)
- [ ] Create insight generation service (statistical anomaly detection)
- [ ] Add forecasting model (linear regression or simple exponential smoothing)
- [ ] Implement PDF report generation (with branding)

---

### 2.4 Recurring & Subscriptions (Lifecycle Management)

**Current State:** Basic recurring transaction creation

**Improvements:**

#### a) Subscription Lifecycle
- **Setup wizard:** Guided creation of recurring transactions
- **Pause/resume:** Temporarily disable without deleting
- **Track upcoming:** Show next billing date + amount in dashboard
- **Skip instance:** Skip next occurrence without deleting the rule
- **End subscription:** Mark as ended (e.g., Netflix cancelled on 2026-03-15)

#### b) Subscription Analytics
- **Total annual spend:** Sum of all subscriptions × 12
- **Subscription audit:** "You have 23 active subscriptions, 5 unused, 2 expired"
- **Churn alerts:** "You haven't used Adobe in 60 days—cancel to save?"
- **Cost comparison:** "Spotify ₹99/mo vs. Apple Music ₹99/mo vs. Amazon Music ₹89/mo"

#### c) Integration with Bill Reminders
- **SMS notifications:** Get alerts before large subscription charges
- **Payment method tracking:** Which card is billed for each subscription
- **Automatic renewal tracking:** Know when subscriptions auto-renew

**Implementation:**
- [ ] Enhance Recurring Transaction model with pause/end dates, skip logic
- [ ] Build subscription audit engine (inactive detection, duplicate detection)
- [ ] Create setup wizard UI (frequency, start date, end condition)
- [ ] Implement notification engine for upcoming subscriptions
- [ ] Add churn prediction model

---

## Phase 3: User Experience & Engagement — Priority: HIGH

### 3.1 Onboarding & First-Time User Experience

**Current State:** Basic splash screen + SMS permission request

**Improvements:**

#### a) Intelligent Onboarding
- **Multi-step wizard:** Goals (track spending, save, budget), currency, account setup
- **Sample data:** Create sample transactions to show app flow
- **Permission flow:** Explain why each permission is needed (show data examples)
- **First transaction:** Guided creation with helpful categorization suggestions
- **Tutorial overlays:** Show key features (FAB, analytics, budget) with tooltips

#### b) Empty States
- **Dashboard empty state:** Motivational message + CTA to add first transaction
- **Analytics empty state:** Show "Add transactions to see insights"
- **Budget empty state:** Guided budget creation with recommendations

**Implementation:**
- [ ] Design multi-step onboarding flow (Jetpack Compose navigation)
- [ ] Create sample data seeding on first launch
- **[ ] Add in-app tutorial system (spotlight, tooltips)
- [ ] Build smart empty states with contextual CTAs

---

### 3.2 Transaction Entry Speed (Core UX)

**Current State:** Multiple screens (paste, SMS, manual)

**Improvements:**

#### a) One-Tap Transaction Entry
- **Quick add from widget:** Home screen widget for fast entry
- **NFC payment integration:** Tap card reader to auto-fill merchant + amount
- **Voice entry:** "Spent 500 for coffee" → auto-create transaction
- **Auto-fill:** Show recent merchants + categories in dropdown
- **Smart defaults:** If amount < 100, auto-categorize as "Food" with 95% confidence

#### b) Batch Entry
- **Import multiple receipts:** Take photo of 5 receipts, OCR all, one-tap to save
- **Copy-paste multiple lines:** "500 coffee, 100 auto fuel" → parse + create 2 transactions
- **Bill splitting:** Shared expense tracking (e.g., "Split 5000 dinner with 3 friends")

**Implementation:**
- [ ] Add home screen widget (Android AppWidgets)
- [ ] Integrate Google ML Kit for receipt OCR or Tesseract
- [ ] Build voice recognition integration (Google Speech-to-Text)
- [ ] Implement NFC payment reader integration
- [ ] Create smart parsing for batch entry

---

### 3.3 Design & Visual Polish (Already Improved)

**Current Progress:**
- ✅ Gradient hero card on dashboard
- ✅ Rounded bottom navigation
- ✅ Better empty states

**Remaining:**

#### a) Accessibility
- **Dark mode optimization:** Ensure proper contrast ratios (WCAG AA)
- **Large text support:** Test with 200% text scaling
- **Haptic feedback:** Subtle vibrations on successful actions
- **Screen reader support:** Proper content descriptions and navigation order
- **Color blindness:** Avoid red-green only indicators

#### b) Motion & Animation
- **Page transitions:** Smooth shared element transitions between screens
- **Progress animations:** Animated progress bars, skeleton loaders
- **Gesture feedback:** Visual feedback for swipe, delete, undo actions
- **Loading states:** Skeleton screens vs. spinners for different contexts

**Implementation:**
- [ ] Audit accessibility with AccessibilityService
- [ ] Implement haptic feedback (VibrationEffect)
- [ ] Add screen reader descriptions (contentDescription)
- [ ] Create reusable animation components (using Compose animations skill)
- [ ] Test with accessibility testing tools

---

## Phase 4: Data Quality & Reliability — Priority: CRITICAL

### 4.1 Data Validation & Integrity

**Current State:** Basic validation on entry

**Improvements:**

#### a) Transaction Validation
- **Amount validation:** Detect outliers (e.g., 100000 for "Coffee" → flag as unusual)
- **Date validation:** Reject future dates, flag ancient dates (>2 years old)
- **Duplicate detection:** Hash-based detection + user review
- **Referential integrity:** Ensure category exists before saving transaction
- **Soft deletes:** Archive rather than delete; allow recovery

#### b) Data Consistency
- **Atomic operations:** Use database transactions for multi-step operations
- **Audit trail:** Track who changed what, when (timestamp + user + old/new values)
- **Backup & sync:** Automatic backup to cloud with conflict resolution
- **Export integrity:** Checksums for exported data

**Implementation:**
- [ ] Add comprehensive input validation layer
- [ ] Implement soft-delete pattern (isDeleted flag + recovery UI)
- [ ] Add audit logging (Room with @Ignore fields for audit)
- [ ] Implement conflict resolution for multi-device sync
- [ ] Create data integrity checker (periodic validation + repair)

---

### 4.2 Testing Strategy

**Current State:** Unknown testing coverage

**Improvements:**

#### a) Unit Testing
- **Classification logic:** 95%+ accuracy on regex + LLM parsing
- **Category assignment:** Test edge cases (multi-label, ambiguous merchants)
- **Budget calculations:** Test rolling budgets, flex logic, edge dates (leap years)
- **Recurrence logic:** Test complex patterns (15th of month, every 2 weeks)
- **Target:** 80%+ code coverage for core business logic

#### b) Integration Testing
- **Transaction flow:** End-to-end (SMS → classification → categorization → DB)
- **Budget alerts:** Verify notifications fire at correct thresholds
- **Sync logic:** Test device-to-cloud-to-device scenarios
- **Import/export:** Verify data integrity through full cycle

#### c) UI Testing
- **Transaction entry:** Test all entry methods (paste, SMS, manual)
- **Navigation:** Verify all screen transitions, back button behavior
- **Empty states:** Test all empty state UIs
- **Permissions:** Test all permission scenarios (denied, granted, revoked)

#### d) Performance Testing
- **Large datasets:** Test with 10K+ transactions
- **Analytics:** Verify chart rendering is smooth (60 FPS)
- **LLM categorization:** Time limit categorization to <2s per message
- **Memory:** Profile memory usage under load

**Implementation:**
- [ ] Setup JUnit + Mockito for unit tests
- [ ] Create integration test suite with Room in-memory database
- [ ] Implement UI tests with Compose test framework
- [ ] Setup continuous testing in CI/CD
- [ ] Create performance benchmark suite

---

### 4.3 Error Handling & Resilience

**Current State:** Basic error messages

**Improvements:**

#### a) Graceful Degradation
- **Network failures:** Queue transactions for later sync; show offline indicator
- **LLM failures:** Fall back to regex + user selection
- **SMS read failures:** Retry with exponential backoff
- **Storage full:** Offer cloud backup as alternative

#### b) Error Recovery
- **Undo/redo:** Support undoing last N actions
- **Recovery mode:** On app crash, offer to restore previous state
- **Data repair:** Auto-detect and fix corrupted transactions
- **Clear error messages:** No tech jargon; offer actionable solutions

**Implementation:**
- [ ] Implement error recovery layer in ViewModel
- [ ] Add undo/redo stack (Command pattern)
- [ ] Create crash reporting (Firebase Crashlytics)
- [ ] Build data repair diagnostics
- [ ] Add user-friendly error dialogs

---

## Phase 5: Security & Privacy — Priority: CRITICAL

### 5.1 Data Encryption

**Current State:** Unclear encryption status

**Improvements:**

#### a) At-Rest Encryption
- **Database encryption:** Use SQLCipher for Room database
- **Sensitive fields:** Encrypt merchant name, amount, notes
- **Backup encryption:** AES-256 for cloud backups

#### b) In-Transit Encryption
- **API calls:** TLS 1.3+ for all cloud communication
- **Certificate pinning:** Pin API server certificates
- **Secure storage:** Store auth tokens in EncryptedSharedPreferences

#### c) End-to-End Encryption
- **Multi-device sync:** Encrypt data client-side before sending to cloud
- **User key management:** Secure key derivation (PBKDF2)
- **Key rotation:** Support key rotation without data loss

**Implementation:**
- [ ] Integrate SQLCipher for database encryption
- [ ] Implement EncryptedSharedPreferences for tokens
- [ ] Setup certificate pinning (OkHttp)
- [ ] Create E2E encryption layer for cloud sync
- [ ] Audit with security assessment

---

### 5.2 Privacy & Permissions

**Current State:** Requests SMS permission; consent dialog for AI

**Improvements:**

#### a) Privacy-First Design
- **Minimal permissions:** SMS read-only (not write); SMS sending only if needed
- **Permission justification:** Explain why each permission is needed
- **Selective sync:** Let users choose what to sync to cloud
- **GDPR/data residency:** Support local-only operation (no cloud)
- **Anonymization option:** Option to anonymize transaction data in backups

#### b) Transparency
- **Privacy policy:** Clear, user-friendly explanation
- **Data practices:** Show exactly what data is collected, stored, shared
- **Audit log:** Let users see access to their data
- **Deletion:** Permanent data deletion with verification

**Implementation:**
- [ ] Create privacy-first architecture (local-first by default)
- [ ] Add permission request workflow with explanations
- [ ] Implement selective sync preferences
- [ ] Write comprehensive privacy policy
- [ ] Add data deletion verification

---

## Phase 6: Infrastructure & DevOps — Priority: MEDIUM

### 6.1 Cloud Infrastructure (Optional)

**Current State:** On-device only

**Improvements:**

#### a) Sync Service (Optional)
- **Cloud backup:** Encrypted transaction backup for disaster recovery
- **Multi-device sync:** Sync transactions across mobile + web
- **Conflict resolution:** Last-write-wins or user resolution
- **API rate limiting:** Prevent abuse

#### b) Analytics Backbone (Optional)
- **Privacy-preserving:** Aggregate user stats without exposing individual data
- **Insights generation:** Run ML models for anomaly detection
- **Forecasting:** Batch processing for spending forecasts

**Implementation:**
- [ ] Design cloud sync architecture (end-to-end encrypted)
- [ ] Create API server (Node.js or Spring Boot)
- [ ] Setup database (PostgreSQL with encryption at rest)
- [ ] Implement sync conflict resolution
- [ ] Create analytics pipeline (scheduled jobs)

---

### 6.2 Release & Deployment

**Current State:** Unclear release process

**Improvements:**

#### a) CI/CD Pipeline
- **Automated testing:** Run unit + integration tests on every PR
- **Build automation:** Build APK on every merge to main
- **Release staging:** Beta testing via Google Play Beta channel
- **Automated release notes:** Generate from git commits
- **Version management:** Semantic versioning (major.minor.patch)

#### b) Monitoring
- **Crash reporting:** Firebase Crashlytics for production crashes
- **Analytics:** User engagement, feature usage, retention
- **Performance monitoring:** App startup time, memory, battery drain
- **Feedback collection:** In-app feedback widget

**Implementation:**
- [ ] Setup GitHub Actions for CI/CD
- [ ] Integrate Crashlytics + Analytics
- [ ] Create release checklist + automation
- [ ] Setup feature flag system (Firebase Remote Config)
- [ ] Create monitoring dashboard (Grafana or similar)

---

## Phase 7: Feature Roadmap (Future Enhancements)

### Near-term (3–6 months)
- [ ] Web dashboard (view-only or full sync)
- [ ] Bill splitting / group expenses
- [ ] Tax insights & ITR-ready reports
- [ ] Advanced recurring management (pause, skip, end)
- [ ] Receipt OCR for manual entry

### Mid-term (6–12 months)
- [ ] Spending forecasting & goal tracking
- [ ] Smart spending recommendations
- [ ] Integration with payment apps (Google Pay, PayTM)
- [ ] API for third-party integrations
- [ ] Community features (spending benchmarks, tips)

### Long-term (12+ months)
- [ ] Investment tracking integration
- [ ] Wealth management features
- [ ] Crypto transaction support
- [ ] Machine learning-powered budgeting
- [ ] White-label solution for fintech partners

---

## Implementation Priorities Matrix

| Phase | Feature | Priority | Effort | Impact | Timeline |
|-------|---------|----------|--------|--------|----------|
| 1.1 | Enhanced AI Classification | CRITICAL | High | Very High | Month 1–2 |
| 1.2 | Subscription Detection | HIGH | Medium | High | Month 2–3 |
| 2.1 | Transaction Management Rich Data | HIGH | Medium | High | Month 3–4 |
| 2.2 | Budget Alerts & Notifications | HIGH | Medium | High | Month 2 |
| 2.3 | Analytics Suite | HIGH | High | Very High | Month 4–6 |
| 4.1 | Data Validation & Integrity | CRITICAL | High | Very High | Month 1–3 |
| 4.2 | Testing Strategy | CRITICAL | High | High | Ongoing |
| 5.1 | Data Encryption | CRITICAL | Medium | Very High | Month 3–4 |
| 6.2 | CI/CD & Release | HIGH | Medium | Medium | Month 2 |
| 3.1 | Onboarding UX | MEDIUM | Medium | Medium | Month 1–2 |

---

## Success Metrics

### Categorization Quality
- **Accuracy:** >95% automatic categorization accuracy (measure against user corrections)
- **User override rate:** <5% (indicates high confidence)
- **Latency:** <2 seconds per transaction classification

### Core Functionality
- **Data integrity:** 0 duplicate transactions in user's lifetime
- **Budget accuracy:** 100% correct budget calculations (audited)
- **Sync success rate:** >99.9% (for cloud sync feature)

### User Engagement
- **Retention:** 30-day retention >50%, 90-day >30%
- **Daily active users:** X% of installed base
- **Transaction entry rate:** >5 transactions per active user per month
- **Analytics view:** >20% of users view insights monthly

### Reliability
- **Crash-free session rate:** >99%
- **App startup time:** <2 seconds (cold), <500ms (warm)
- **Test coverage:** ≥80% for core business logic
- **Bug escape rate:** <0.5% of bugs escape to production

---

## Getting Started

1. **Form core team:** Mobile engineer, backend engineer, QA engineer, product designer
2. **Week 1:** Review current codebase, identify technical debt
3. **Week 2:** Prioritize Phase 1.1 (AI Classification enhancement)
4. **Ongoing:** Release fortnightly updates, gather user feedback
5. **Quarterly:** Major feature releases (Phases 1–3)

---

## Appendix: Technical Stack Recommendations

- **Frontend:** Jetpack Compose (already in use), Material Design 3
- **Backend:** Node.js + Express or Spring Boot (if cloud features added)
- **Database:** Room (local), PostgreSQL (cloud)
- **Testing:** JUnit, Mockito, Espresso, Compose Test
- **Analytics:** Firebase Analytics + custom events
- **Error Tracking:** Firebase Crashlytics
- **CI/CD:** GitHub Actions
- **Encryption:** SQLCipher, BoringSSL, Conscrypt
- **ML/AI:** Llama.cpp (already integrated), TensorFlow Lite (future)

---

**Document Owner:** AuraSpend Product Team  
**Last Reviewed:** 2026-08-07  
**Next Review:** 2026-09-07
