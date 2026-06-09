# Bank SMS Classification System

The classification system is AuraSpend's core feature. It parses SMS messages from Indian banks using regex, classifies transactions into categories, and presents results to the user.

## Architecture

```
Raw SMS text
     │
     ▼
BankMessageParser        ← Regex extraction per bank
     │
     ▼
ParsedBankMessage        ← Amount, type, merchant, date, confidence
     │
     ▼
TransactionClassifier    ← Keyword + category matching
     │
     ▼
Transaction(category inferred)
```

## BankMessageParser

**File**: `data/classification/BankMessageParser.kt`

Supported banks (9 Indian banks):

| Bank | Detection |
|------|-----------|
| HDFC | Sent from, HDFC Bank, message patterns |
| ICICI | ICICI Bank, ICICI, iMobile |
| SBI | SBI, State Bank |
| Axis | Axis Bank, Axis |
| Kotak Mahindra | Kotak, Kotak Mahindra |
| Yes Bank | Yes Bank, YesBank |
| PNB | PNB, Punjab National Bank |
| Canara Bank | Canara Bank |
| Bank of Baroda | BOB, Bank of Baroda |

### Extraction Logic

The parser uses bank-specific regex patterns to extract:

1. **Amount**: Matches Rs/INR/₹ followed by decimal numbers
2. **Transaction type**: Detects `credited`, `debited`, `spent`, `paid`, `refund` keywords
3. **Merchant**: Extracts merchant name from "at X", "to X", "at X on" patterns
4. **Confidence score**: Based on how many fields were successfully extracted:
   - All fields: 0.95
   - Amount + merchant + date: 0.80
   - Amount + merchant: 0.65
   - Amount + type: 0.50
   - Amount only: 0.35
   - No amount: 0.20

### ParsedBankMessage

```kotlin
data class ParsedBankMessage(
    val bankName: String?,
    val amount: Double?,
    val type: TransactionType?,   // CREDIT / DEBIT
    val merchant: String?,
    val date: LocalDateTime?,
    val confidence: Double         // 0.20 to 0.95
)
```

## TransactionClassifier

**File**: `data/classification/TransactionClassifier.kt`

Takes a `ParsedBankMessage` and assigns a category using keyword matching. Looks up the merchant in `DefaultCategories.MERCHANT_KEYWORDS` which maps 140+ merchants/brands to categories.

If no merchant match is found:
- DEBIT → "Other Expense"
- CREDIT → "Income"

## DefaultCategories

**File**: `data/classification/DefaultCategories.kt`

Defines 12 default categories in a specific display order:

| # | Category ID | Name | Icon | Color |
|---|-------------|------|------|-------|
| 1 | cat_food | Food & Dining | Fastfood | Red |
| 2 | cat_groceries | Groceries | ShoppingCart | Orange |
| 3 | cat_transport | Transport | DirectionsBus | Blue |
| 4 | cat_shopping | Shopping | ShoppingBag | Pink |
| 5 | cat_entertainment | Entertainment | Movie | Purple |
| 6 | cat_bills | Bills & Utilities | Receipt | Teal |
| 7 | cat_health | Health & Medical | LocalHospital | Green |
| 8 | cat_education | Education | School | Indigo |
| 9 | cat_rent | Rent & Housing | Home | Brown |
| 10 | cat_salary | Income | AccountBalance | Green |
| 11 | cat_other_expense | Other Expense | MoneyOff | Grey |
| 12 | cat_other_income | Other Income | AttachMoney | Green |

The `MERCHANT_KEYWORDS` map contains 140+ entries mapping merchants to categories, including:
- Food: swiggy, zomato, dominos, mcdonalds, starbucks, subway, pizza hut, kfc
- Groceries: bigbasket, blinkit, zepto, dmart, reliance fresh, grocery, supermarket
- Transport: uber, ola, rapido, metro, irctc, indigo, redbus, makemytrip
- Shopping: amazon, flipkart, myntra, ajio, meesho, nykaa
- Entertainment: netflix, amazon prime, hotstar, spotify, bookmyshow, youtube premium
- Bills: electricity, airtel, jio, vi, broadband, water, gas
- Health: pharmacy, hospital, clinic, practo, 1mg
- Education: udemy, coursera, byjus, unacademy, vedantu

## Classification Screen

**File**: `ui/classification/ClassificationScreen.kt`

Two-tab interface:

1. **"Paste Message" tab**: User pastes a raw SMS text → auto-parses and classifies
2. **"From SMS" tab**: Reads from device SMS inbox (`Telephony.Sms.Inbox`) filtered by bank-related keywords. Requires `READ_SMS` permission.

Both paths show a classification result card with:
- Parsed details (amount, merchant, bank, date, type)
- Confidence indicator
- Suggested category (editable)
- "Add Transaction" button to save
