# AuraSpend User Guide

Your personal expense manager for Android — track spending, classify bank SMS, set budgets, and stay on top of your finances.

---

## Table of Contents

- [Getting Started](#getting-started)
- [Dashboard](#dashboard)
- [Managing Transactions](#managing-transactions)
- [Smart SMS Classification](#smart-sms-classification)
- [Budget Management](#budget-management)
- [Analytics](#analytics)
- [Categories](#categories)
- [Recurring Subscriptions](#recurring-subscriptions)
- [Settings](#settings)
- [Premium Features](#premium-features)
- [Troubleshooting & FAQ](#troubleshooting--faq)

---

## Getting Started

### First Launch

When you open AuraSpend for the first time, you'll see:

1. **Splash Screen** — A brief animated splash while the app initializes.
2. **Onboarding** — A 3-page carousel introducing key features. Swipe left or tap **Next** to advance. On the final page, tap **Get Started** to enter the app.
3. **Google Drive Restore** — On the last onboarding page, you can tap **Restore from Google Drive** to retrieve data from a previous backup. This replaces all local data.

After onboarding, 12 default categories are auto-seeded and you land on the Dashboard.

### Navigation

The app uses a bottom navigation bar with four tabs:

| Tab | Icon | Description |
|-----|------|-------------|
| Home | AccountBalance | Dashboard overview |
| Transactions | Receipt | Transaction list |
| Analytics | Analytics | Charts and insights |
| Settings | Settings | Preferences and data management |

Tap the **+** (FAB) button on any tab to open the add-transaction bottom sheet with two options: **Smart Add** (SMS/classification) or **Manual** (enter details).

---

## Dashboard

The Dashboard gives you a snapshot of your financial health.

### Balance Card

Shows your **Total Balance** with monthly income and expense chips. The card uses the Material 3 primary container color for visual prominence.

### Quick Stats Row

Three stat cards display:
- **Transactions** — count of recent transactions
- **Largest** — biggest expense amount
- **Subscriptions** — total monthly subscription cost

### Weekly Bar Chart

A Canvas-drawn bar chart showing current week spending (Monday–Sunday). Each bar height is proportional to that day's spending. Hover/read the day labels below the chart.

### Budget Overview

If you have active budgets, each appears as a progress card:
- **Green**: under 75% of limit
- **Orange**: 75–90% of limit
- **Red**: over 90% of limit

### Subscription Summary

A card showing your total monthly subscription cost and active subscription count.

### Spending by Category

A horizontal scrollable row showing spending amounts per category with category colors and icons.

### Recent Transactions

Shows the 5 most recent transactions. Tap **See All** to go to the full transaction list. Tap a transaction card to view its details.

---

## Managing Transactions

### Transaction List

The full transaction list groups entries by date: **Today**, **Yesterday**, **This Week**, **This Month**, **Older**. Each entry shows:
- Category color indicator
- Merchant name (or note if no merchant)
- Category name
- Amount with +/- prefix
- Date and time

**Filters**: Use the chip row at the top to filter by All, Expense, or Income.

**Search**: Tap the search icon in the top bar and type keywords (searches merchant and note fields).

**Delete**: Swipe a transaction card to the **left** to delete it. A red delete background appears as confirm.

### Add Transaction (Manual)

1. Tap the **+** FAB and select **Manual**.
2. Fill in: Amount (required), Note, Merchant (optional), date, and select Income or Expense.
3. Choose a category from the dropdown.
4. Tap **Save**. The transaction is persisted to the local database.

### Add Transaction (Smart)

See the [Smart SMS Classification](#smart-sms-classification) section below.

---

## Smart SMS Classification

AuraSpend can automatically extract transaction details from bank SMS messages.

### How It Works

1. The app parses raw SMS text using bank-specific regex patterns.
2. Extracted fields: **amount**, **transaction type** (credit/debit), **merchant**, **bank name**, **date**.
3. A confidence score (0.20–0.95) indicates how reliably the message was parsed.
4. The `TransactionClassifier` matches the merchant against 140+ known brands to suggest a category.

### Supported Banks

| Bank | Status |
|------|--------|
| HDFC | ✅ Supported |
| ICICI | ✅ Supported |
| SBI | ✅ Supported |
| Axis | ✅ Supported |
| Kotak Mahindra | ✅ Supported |
| Yes Bank | ✅ Supported |
| PNB | ✅ Supported |
| Canara Bank | ✅ Supported |
| Bank of Baroda | ✅ Supported |

### Using the Classification Screen

There are two input methods:

1. **Paste Message** tab: Copy a bank SMS and paste it into the text field. Tap **Parse**.
2. **From SMS** tab: Requires `READ_SMS` permission. Lists recent bank-related messages from your inbox.

After parsing, a result card shows:
- Parsed amount, merchant, bank, date, transaction type
- Confidence indicator
- Suggested category (editable — you can change it)
- **Add Transaction** button to save

### Manual Entry

If automatic classification isn't accurate enough, you can always add transactions manually via the Manual screen (FAB → Manual).

---

## Budget Management

### Creating a Budget

1. Go to **Settings → Budget Settings**, or navigate to Budgets from the nav.
2. Tap **Add Budget**.
3. Select a **Category** (e.g., Food & Dining).
4. Set a **Spending Limit** and choose a **Period** (Weekly, Monthly, Yearly).
5. Save.

### Viewing Progress

Budget progress bars appear on the Dashboard and the Budget screen:
- **Green**: under 75% used
- **Orange**: 75–90% used
- **Red**: over 90% used (or exceeded)

The progress card shows current spend vs. limit (e.g., ₹1,200 / ₹5,000).

### Editing / Deleting

On the Budget screen, tap a budget to edit its limit or period. Swipe to delete or use the delete action.

---

## Analytics

The Analytics screen provides visual spending insights.

### Summary Cards

Two cards at the top show your total **Income** (primary color) and total **Expense** (error color) across all time.

### Category Pie Chart

A Canvas-drawn donut chart showing spending breakdown by category:
- Each slice uses the category's assigned color
- Center shows total spending
- Legend below lists categories with color indicator, name, percentage, and amount
- Tap a legend item for more details

### Top Merchants

A ranked list of your top 10 merchants by total spend, showing merchant name and amount.

---

## Categories

### Default Categories

AuraSpend ships with 12 pre-seeded categories:

| # | Name | Icon |
|---|------|------|
| 1 | Food & Dining | Fastfood |
| 2 | Groceries | ShoppingCart |
| 3 | Transport | DirectionsBus |
| 4 | Shopping | ShoppingBag |
| 5 | Entertainment | Movie |
| 6 | Bills & Utilities | Receipt |
| 7 | Health & Medical | LocalHospital |
| 8 | Education | School |
| 9 | Rent & Housing | Home |
| 10 | Income | AccountBalance |
| 11 | Other Expense | MoneyOff |
| 12 | Other Income | AttachMoney |

### Custom Categories

1. Go to **Settings → Manage Categories**.
2. Tap **Add Category**.
3. Enter a **Name**, pick an **Icon** (Material icon), and choose a **Color**.
4. Save. The new category appears in transaction forms and filters.

You can also edit or delete custom categories from this screen. Default categories cannot be deleted.

---

## Recurring Subscriptions

### Adding a Subscription

1. Go to **Settings → Manage Subscriptions**.
2. Tap **Add Subscription**.
3. Enter: Name, Amount, Category, Billing Cycle (Daily/Weekly/Monthly/Yearly), and Next Billing Date.
4. Save.

### Managing Subscriptions

- View all active subscriptions with their billing amounts and next due dates.
- Toggle active/inactive status.
- Delete subscriptions that are no longer relevant.
- The total monthly subscription cost is displayed on the Dashboard.

### Auto-Detection

Recurring transactions can be flagged as subscriptions when adding transactions, and they appear in the Subscriptions screen automatically.

---

## Settings

### Appearance

**Theme Selector**: Choose between three modes:
- **Light** — Light background, teal primary
- **Dark** — Dark background, teal primary
- **AMOLED** — True black background (saves battery on OLED screens) — Premium only

On Android 12+, **Dynamic Color** is supported (monet theming) and takes priority over the manual palette unless AMOLED mode is explicitly selected.

### Data Management

**Export to CSV**: Saves all transactions to a `.csv` file. Use the system file picker to choose a destination.

**Import from CSV**: Load transactions from a previously exported CSV file. Replaces no existing data — imports are appended.

### Finance Management

- **Manage Categories**: Add, edit, or delete categories.
- **Manage Subscriptions**: Track recurring expenses and billing cycles.
- **Budget Settings**: Set per-category spending limits.

### Google Drive Backup (Premium)

1. Go to **Settings** → **Backup to Drive** or use the onboarding restore flow.
2. Sign in with your Google account (OAuth 2.0).
3. **Backup**: Serializes all data (transactions, categories, budgets, subscriptions) to JSON and uploads to your hidden Drive appDataFolder.
4. **Restore**: Downloads the latest backup and replaces all local data.
5. Each backup overwrites the previous one.

> **Note**: Requires `WEB_CLIENT_ID` configured in `secrets.properties` during development. Network connection required.

---

## Premium Features

### What's Included

| Feature | Free | Premium |
|---------|------|---------|
| Smart SMS Classification | ✅ | ✅ |
| Dashboard | ✅ | ✅ |
| Transaction Management | ✅ | ✅ |
| Budget Tracking | ✅ | ✅ |
| Recurring Subscriptions | ✅ | ✅ |
| CSV Export/Import | ✅ | ✅ |
| Build Flavors (free) | ✅ | — |
| **Dark & AMOLED Theme** | ❌ | ✅ |
| **Advanced Analytics** | ❌ | ✅ |
| **Google Drive Backup** | ❌ | ✅ |
| **Ad-Free Experience** | ❌ | ✅ |

### Upgrade Flow

On the Settings screen, tap the Premium upgrade section or navigate to the AuraSpend+ screen. Follow the in-app purchase flow (Play Store build) to unlock premium features.

During development, the `free` build flavor unlocks all premium features automatically.

---

## Troubleshooting & FAQ

### The app won't compile in development

Ensure you have:
- JDK 17+
- Android SDK 36
- Copied `secrets.properties.example` to `secrets.properties`
- Set `WEB_CLIENT_ID` for Drive sync (optional for non-Drive features)

### My bank SMS isn't being parsed

- Make sure the SMS is from a supported bank (see supported banks list).
- Try the **Paste Message** tab instead of **From SMS**.
- Check the confidence indicator — low confidence means some fields couldn't be extracted.
- If parsing fails, use **Manual** entry via the FAB.

### I lost my data after upgrading

The database uses destructive migration (`fallbackToDestructiveMigration`). If the schema changed between versions, data is lost. Use **CSV Export** or **Google Drive Backup** before updating.

### Drive backup/restore isn't working

- Ensure you have a stable internet connection.
- Check that `WEB_CLIENT_ID` is set correctly in your build configuration.
- Verify you've granted the required permissions during the Google sign-in flow.
- The backup file is stored in a hidden `appDataFolder` — it won't appear in your Drive UI.

### How do I reset the app?

1. Go to **Settings → Export to CSV** to save your data.
2. Clear the app data from Android Settings → Apps → AuraSpend → Storage → Clear data.
3. On relaunch, the app re-seeds default categories and shows onboarding.

### How is my data handled?

All bank SMS processing happens **on-device**. No SMS data is sent to remote servers. Drive backup stores encrypted data in your personal Google Drive account via OAuth 2.0.
