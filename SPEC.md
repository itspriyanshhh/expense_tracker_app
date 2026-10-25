# PulseLedger — Architecture & System Specification

## 1. Executive Summary & Core Mission
**PulseLedger** is a zero-cost, 100% accurate, privacy-first automated Android expense tracking application designed for the Indian fintech ecosystem. It intercepts transactions in real-time from UPI applications (Google Pay, PhonePe, Paytm, Amazon Pay, BHIM) and banking SMS messages, categorizing them with zero user friction through a deterministic 4-tier waterfall architecture.

---

## 2. System Architecture Overview

```
                      ┌───────────────────────────────────────────────┐
                      │              Transaction Sources              │
                      │  • NotificationListenerService (UPI Apps/SMS) │
                      │  • SMS_RECEIVED BroadcastReceiver (Optional)  │
                      │  • WorkManager (30-day Historical Backfill)   │
                      └───────────────────────┬───────────────────────┘
                                              │
                                              ▼
                      ┌───────────────────────────────────────────────┐
                      │        3-Minute Sliding Deduplication         │
                      │  Key: [Amount + RefId/UPI_UTR + Timestamp±180s│
                      │  Status: Discard duplicates / update enrich   │
                      └───────────────────────┬───────────────────────┘
                                              │
                                              ▼
                      ┌───────────────────────────────────────────────┐
                      │         4-Tier Categorization Waterfall       │
                      │                                               │
                      │  [Tier 1: Local Room DB merchant_memory]     │
                      │       ├── Match found ──> Tag Verified (1.00) │
                      │       └── No match                            │
                      │               ▼                               │
                      │  [Tier 2: On-Device Keyword / Prefix Engine] │
                      │       ├── Match found ──> Tag Verified (0.95) │
                      │       └── No match                            │
                      │               ▼                               │
                      │  [Tier 3: Scrubbed Gemini REST API Client]    │
                      │       ├── Conf >= 0.85 & !is_p2p ──> Tagged   │
                      │       └── Conf < 0.85 OR is_p2p == true       │
                      │               ▼                               │
                      │  [Tier 4: 1-Tap Actionable Android Push]      │
                      │       └── Tap saves txn + commits to Tier 1   │
                      └───────────────────────────────────────────────┘
```

---

## 3. Ingestion Engine

### 3.1 NotificationListenerService (`PulseNotificationListener`)
- Listens to incoming status bar notifications from target packages:
  - `com.google.android.apps.nbu.paisa.user` (Google Pay)
  - `com.phonepe.app` (PhonePe)
  - `net.one97.paytm` (Paytm)
  - `in.amazon.mShop.android.shopping` / `com.amazon.mShop.android.shopping` (Amazon Pay)
  - Default SMS Apps (`com.google.android.apps.messaging`, `com.samsung.android.messaging`, etc.)
- Extracts transaction primitives:
  - Amount (INR `₹` or `Rs.` / `INR`)
  - Direction (Debit / Expense vs Credit / Income / Refund)
  - Raw Merchant / Payee / VPA string (e.g. `swiggy@icici`, `Zomato`, `Suresh Yadav`)
  - UPI UTR / Ref number (12 digits) or Bank reference
  - Account identifier suffix (`XX1234`)
  - Timestamp (epoch millis)

### 3.2 SMS Ingestion & Backfill
- **SMS_RECEIVED BroadcastReceiver (`SmsReceiver`):** Intercepts real-time transactional SMS from standard banking senders (e.g., `VM-HDFCBK`, `AX-SBIINB`, `ICICIB`, `KOTAKB`).
- **WorkManager Backfill (`HistoricalSmsBackfillWorker`):**
  - Runs once upon user permission grant.
  - Queries `Telephony.Sms.CONTENT_URI` for the past 30 days.
  - Runs deduplication against existing database transactions to prevent double counting.

### 3.3 3-Minute Sliding Window Deduplication
- UPI apps often fire a notification simultaneously with a bank debit SMS for the same transaction.
- Deduplication Key:
  $$\text{Key} = \text{Amount} + (\text{UTR} \parallel \text{Last 4 digits of A/c}) + \lfloor \text{Timestamp} / 180000 \rfloor$$
- If duplicate is detected within $\pm 180$ seconds:
  - Consolidates transaction details (enriching SMS bank data with UPI recipient name if missing).
  - Skips redundant categorization and avoids double debit entries.

---

## 4. 4-Tier Categorization Waterfall

### Tier 1: Local Room DB `merchant_memory`
- **Rule:** Exact match on clean UPI VPA (e.g., `starbucks@okhdfcbank`) or normalized merchant string (`swiggy`, `blinkit`).
- **Accuracy:** 100%. Instant, zero network latency, zero token cost.
- **Learning Mechanism:** Whenever a user categorizes a transaction manually (or via Tier 4 notification), it is upserted into `merchant_memory`.

### Tier 2: Deterministic On-Device Keyword & Prefix Matcher
- **Target Indian Merchants:**
  - *Food & Dining:* Swiggy, Zomato, McDonald's, KFC, Starbucks, Burger King, Domino's, Chaayos, Chai Point.
  - *Groceries & Quick Commerce:* Zepto, Blinkit, Instamart, BigBasket, DMart, Nature's Basket.
  - *Transit & Travel:* DMRC, Metro, IRCTC, Uber, Ola, Rapido, MakeMyTrip, Cleartrip, Yatra, Indigo.
  - *Shopping & E-Commerce:* Amazon, Flipkart, Myntra, Nykaa, Ajio, Tata CLiQ, Meesho.
  - *Bills & Utilities:* BESCOM, Tata Power, Airtel, Jio, Vi, ACT Fibernet, Indane, Bharat Petroleum, HPCL, IOCL.
  - *Entertainment:* Netflix, Spotify, BookMyShow, PVR, INOX, Prime Video, Disney+ Hotstar, YouTube.
  - *Medical & Pharmacy:* Apollo, 1mg, Pharmeasy, Netmeds, MedPlus.
  - *Education:* Coursera, Udemy, Unacademy, Byju's.
- **Accuracy:** > 98% for branded entities. Processed on `Dispatchers.Default` in < 2ms.

### Tier 3: Gemini REST API Structured JSON Classifier
- **Trigger:** Only executed when Tier 1 and Tier 2 yield no confident match and payee is not clearly a personal P2P name.
- **Privacy & Scrubbing Guardrail (Strict):**
  - **NEVER** transmit raw SMS text, account numbers, card numbers, or closing balances.
  - Payload sent:
    ```json
    {
      "merchant": "R K Provision Store",
      "upiVpaPrefix": "rkprovision",
      "hourOfDay": 19,
      "amount": 450.00
    }
    ```
- **Gemini Response Schema:**
  ```json
  {
    "category": "GROCERIES",
    "confidence": 0.92,
    "is_p2p_person_name": false,
    "cleaned_merchant_name": "R K Provision Store"
  }
  ```

### Tier 4: Instant 1-Tap Android Actionable Notification
- **Trigger Condition:**
  - `is_p2p_person_name == true` (Zero-Guessing Rule for humans like "Suresh Yadav", "Ankit Verma"), OR
  - Gemini classification confidence $< 0.85$, OR
  - Offline fallback when Gemini API is unreachable.
- **Notification UX:**
  - Displays transaction amount and sender/payee.
  - Provides quick action buttons directly in the notification:
    `[ Food ]` `[ Groceries ]` `[ Shopping ]` `[ More... ]`
  - Tapping an action immediately:
    1. Updates the transaction category in Room DB.
    2. Upserts payee into Tier 1 `merchant_memory`.
    3. Dismisses the notification with tactile haptic feedback.

---

## 5. Privacy, Security & P2P Zero-Guessing Rule
1. **P2P Zero-Guessing Rule:** If a payee string resembles a personal name (e.g. "Ramesh Kumar") and has no historical entry in `merchant_memory`, the system **never guesses**. Guessing personal transfers degrades trust. Such transactions are tagged `PENDING_USER_TAP` and placed in the user's review queue with an Amber badge (`#F59E0B`).
2. **Local-First Storage:** All transaction ledgers, analytics, and merchant mappings reside exclusively on-device in SQLite/Room DB with SQLCipher encryption readiness.
3. **Network Boundary:** Network calls are strictly confined to the scrubbed Gemini REST endpoint. No analytics, tracking SDKs, or third-party ad networks.

---

## 6. UI/UX Design System ("Dark Fintech Craft")

### 6.1 Color Palette Tokens
| Token Name | Hex Code | Purpose |
| :--- | :--- | :--- |
| **Obsidian Slate** | `#090D16` | App Background Primary |
| **Glass Slate Surface** | `#131B2E` | Elevated Card Surfaces |
| **Border Slate** | `#1F2A44` | Card 1dp Subtle Borders |
| **Emerald Neon** | `#10B981` | Verified Badges, Incomes, Positive Cash Flow |
| **Crimson Coral** | `#F43F5E` | Expenses, Debits, Alerts |
| **Glowing Amber** | `#F59E0B` | Pending User Tap / Uncategorized P2P |
| **Text Primary** | `#F8FAFC` | High-contrast body and headers |
| **Text Muted** | `#94A3B8` | Subtitles, timestamps, metadata |

### 6.2 Typography & Number Formatting
- Strict Indian Rupee formatting via `NumberFormat.getCurrencyInstance(Locale("en", "IN"))`.
- Font feature settings: `fontFeatureSettings = "tnum"` (Tabular Figures) to guarantee zero jitter during ticker animations and rolling counter transitions.

### 6.3 Tactile Haptics
- Haptic feedback is triggered via `LocalHapticFeedback.current.performHapticFeedback(HapticFeedbackType.LongPress)` upon:
  - Tapping quick-categorize chips
  - Scrubbing chart bars or donut slices
  - Saving merchant memory rules

---

## 7. Technology Stack & Dependencies

- **Language & Platform:** Kotlin 2.0+ & Android SDK 34 (Android 14 UpsideDownCake)
- **UI Toolkit:** Jetpack Compose (BOM), Material 3 (`androidx.compose.material3.*` exclusively)
- **Database:** Android Room 2.6+ with KSP annotation processing (`com.google.devtools.ksp`)
- **Networking:** OkHttp 4.12+ / Ktor Client for lightweight Gemini REST API calls
- **Serialization:** Kotlinx Serialization JSON (`kotlinx-serialization-json`)
- **Concurrency:** Kotlin Coroutines & Flow (Room DAO queries wrapped in `Dispatchers.IO`)
- **Background Tasks:** AndroidX WorkManager (`androidx.work:work-runtime-ktx`)
- **Visuals:** Native Compose Canvas & `Animatable` (zero external Maven chart dependencies)
