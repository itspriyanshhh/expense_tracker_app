# PulseLedger — Antigravity Agent System Rules

## 1. Build & Compiler Guardrails (Strict)
- **Kotlin & KSP Only:** NEVER use `kotlin-kapt`. Always use `com.google.devtools.ksp` for Room compiler annotation processing.
- **Material 3 Exclusivity:** NEVER import `androidx.compose.material.*`. Always use `androidx.compose.material3.*`. Add `@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)` at the file or composable level whenever using `ModalBottomSheet`, `TopAppBar`, or `HorizontalPager`.
- **Zero External Chart Libraries:** Do NOT add third-party Maven chart dependencies (many have JitPack resolution failures on modern Gradle). Build all Donut and Bar charts natively using `androidx.compose.foundation.Canvas` and `Animatable`.
- **Coroutine Safety:** Never perform Room DAO queries, Regex batching, or Gemini HTTP calls on `Dispatchers.Main`. Always wrap database and network operations in `withContext(Dispatchers.IO)`.
- **Self-Verification Loop:** After generating or editing any Kotlin or Gradle file, run `./gradlew compileDebugKotlin` in the terminal to verify zero unresolved references before finishing the turn.

## 2. UI/UX Quality Bar ("Dark Fintech Craft")
- **Color Tokens:**
  - App Background: `#090D16` (Obsidian Slate)
  - Elevated Card Surface: `#131B2E` with `1.dp` border `#1F2A44`
  - Verified / Income Accent: `#10B981` (Emerald Neon)
  - Expense / Debit Accent: `#F43F5E` (Crimson Coral)
  - Pending User Tap Accent: `#F59E0B` (Glowing Amber)
- **Typography & Numbers:** Format all Indian currency values using `NumberFormat.getCurrencyInstance(Locale("en", "IN"))` with tabular figures (`fontFeatureSettings = "tnum"`) so digits never jitter during animations.
- **Tactile Micro-Interactions:** Trigger `LocalHapticFeedback.current.performHapticFeedback(HapticFeedbackType.LongPress)` whenever a user taps a quick-categorize chip, scrubs a chart bar, or saves a merchant rule.

## 3. Privacy & 100% Accuracy Rules
- **Strict Data Scrubbing:** Never transmit raw SMS bodies, bank account suffixes (`A/c XX1234`), or closing balances to the Gemini API. Only transmit `{ rawMerchant, upiVpaPrefix, hourOfDay, amount }`.
- **P2P Zero-Guessing Rule:** If a payee string is a human name without business keywords (e.g., "Suresh Yadav", "Ramesh Kumar") and is not found in `merchant_memory`, NEVER guess its category. Always route it to `PENDING_USER_TAP` and fire the 1-Tap Actionable Notification.