# ExpenseTracker — Master Context File
# Paste this at the START of EVERY AI conversation before asking anything.

---

## PROJECT OVERVIEW
- **App Name:** ExpenseTracker
- **Package:** `com.dhiraj.expensetracker`
- **Developer:** Dhiraj
- **Purpose:** Personal finance tracker for Indian users
  - Auto-detects bank transactions from notifications/SMS
  - Manual expense/income entry
  - Budget planning, loan tracking, spending analysis
- **Status:** Functional but incomplete — several screens are stubs or partially wired

---

## TECH STACK
- **Language:** Kotlin
- **UI:** Jetpack Compose (Material 3) — NO XML layouts anywhere
- **Database:** Room (SQLite) — version 3, with migrations 1→2 and 2→3
- **Navigation:** Jetpack Navigation Compose
- **Async:** Kotlin Coroutines + Flow
- **Architecture:** Mostly direct DB access from Composables (no ViewModel layer yet)
- **Min SDK:** 24 (Android 7.0), Target SDK: 34
- **Build:** Gradle KTS, KSP for Room annotation processing
- **Kotlin version:** 1.9.20, Compose compiler: 1.5.4

---

## DATABASE SCHEMA (Room, version 3)

### Table: `transactions`
| Column | Type | Notes |
|---|---|---|
| id | Long (PK autoincrement) | |
| amount | Double | Always stored positive; sign inferred from rawSmsText |
| merchantName | String? | Null if unknown |
| category | String | Free text, maps to CategoryEntity.name |
| date | Date | Stored via TypeConverter as Long |
| bankName | String? | |
| upiId | String? | UPI reference for deduplication |
| rawSmsText | String? | Also used as type flag: "MANUAL_INCOME", "MANUAL_EXPENSE", "MANUAL_TRANSFER" |
| isManualEntry | Boolean | true = user entered, false = auto-detected |
| notes | String? | "Account:X;notes" format. "AUTO_CONFIRMED" flag appended here |

### Table: `notification_logs`
| Column | Type | Notes |
|---|---|---|
| id | Long (PK) | |
| notificationId | Int | Android notification ID |
| merchant | String | |
| amount | Double | Can be negative (debit) or positive (credit) |
| timestamp | Long | Epoch millis |
| status | String | "PENDING" or "LOGGED" |
| source | String | "SMS" or "NOTIFICATION" |
| upiReference | String? | For dedup only |

### Table: `categories`
| Column | Type | Notes |
|---|---|---|
| id | Long (PK) | |
| name | String | Display name, case-insensitive unique |
| emoji | String | |
| isCustom | Boolean | false = system default, true = user-created |
| createdAt | Long | |

**Default categories (seeded on DB create):** Food 🍔, Transport 🚕, Shopping 🛍️, Bills 💡, Entertainment 🎬, Health 💊

### Table: `financial_plan_entries`
| Column | Type | Notes |
|---|---|---|
| id | Long (PK) | |
| templateId | Long | FK to plan_templates (default 1) |
| title | String | |
| category | String | |
| bucket | String | One of: Bills, Essential, Food, Transport, Shopping, Savings, Fun |
| amount | Double | |
| recurrence | String | "ONE_TIME", "WEEKLY", "MONTHLY", "YEARLY" |
| weeklyDay | Int? | Day of week (1=Mon…7=Sun), only for WEEKLY |
| isCompleted | Boolean | |
| completedAt | Long? | |
| createdAt | Long | |

### Table: `loan_entries`
| Column | Type | Notes |
|---|---|---|
| id | Long (PK) | |
| friendName | String | |
| amount | Double | |
| dateGiven | Long | |
| status | String | "OPEN" or "SETTLED" |
| settledAt | Long? | |
| settledBy | String? | |
| notes | String? | |

### Table: `plan_templates`
| Column | Type | Notes |
|---|---|---|
| id | Long (PK) | |
| name | String | e.g. "Default Monthly" |
| createdAt | Long | |

**Default template:** id=1, name="Default Monthly" (seeded on DB create)

---

## FILE STRUCTURE

```
app/src/main/java/com/dhiraj/expensetracker/
├── data/
│   ├── AppDatabase.kt          — Singleton Room DB, version 3, migrations, seeding
│   ├── Transaction.kt          — @Entity for transactions table
│   ├── TransactionDao.kt       — DAO: insert, update, getAllTransactions (Flow), queries
│   ├── Category.kt             — ENUM (legacy, not used for DB) — kept for notification buttons
│   ├── CategoryEntity.kt       — @Entity for categories table
│   ├── CategoryDao.kt          — DAO: insert, update, delete, getAllCategories (Flow), exists
│   ├── CategoryCount.kt        — Data class: category + count (used in queries)
│   ├── Converters.kt           — TypeConverter: Date ↔ Long
│   ├── NotificationLog.kt      — @Entity for notification_logs table
│   ├── NotificationLogDao.kt   — DAO: insert, getAllLogs, updateStatus, existsPendingByUpi
│   ├── FinancialPlanEntry.kt   — @Entity for financial_plan_entries table
│   ├── FinancialPlanDao.kt     — DAO: insert, update, delete, getEntriesForTemplate
│   ├── LoanEntry.kt            — @Entity for loan_entries table
│   ├── LoanEntryDao.kt         — DAO: insert, update, delete, getAllLoans
│   ├── PlanTemplate.kt         — @Entity for plan_templates table
│   └── PlanTemplateDao.kt      — DAO: insert, update, delete, getAllTemplates
│
├── notifications/
│   ├── BankNotificationListener.kt     — NotificationListenerService; parses notifications via SmsParser
│   └── TransactionNotificationManager.kt — Shows/cancels "Quick Categorize" notification with action buttons
│
├── receivers/
│   └── CategoryActionReceiver.kt       — BroadcastReceiver; handles category button taps from notification
│
├── ui/
│   ├── MainActivity.kt                 — Entry point; handles permissions, theme, palette
│   ├── MainScaffold.kt                 — Navigation scaffold; bottom nav; FAB for add transaction
│   ├── CategorizeTransactionActivity.kt — Launched from notification "More" button; shows CategoryOverlaySheet
│   │
│   ├── screens/
│   │   ├── RecordsScreen.kt            — Transaction list with filter/sort; edit/delete via long press
│   │   ├── AnalysisScreen.kt           — Pie chart + weekly bar chart + category progress bars
│   │   ├── BudgetsScreen.kt            — Hardcoded budget limits per category with progress bars ⚠️
│   │   ├── AccountsScreen.kt           — Notification log view; pending→logged flow; category assignment
│   │   ├── PlannerScreen.kt            — Financial planner: safe-to-spend, bills, loan tracker, plan entries
│   │   ├── CategoriesScreen.kt         — Manage categories (add/edit/delete)
│   │   └── AddTransactionScreen.kt     — Manual entry screen with calculator keypad
│   │
│   ├── components/
│   │   ├── CategoryOverlaySheet.kt     — Bottom sheet: pick/create category for a pending notification log
│   │   ├── CategoryPickerDialog.kt     — AlertDialog version of category picker (older, used in AccountsScreen)
│   │   ├── EmojiPickerSheet.kt         — Grid of common emojis + custom input
│   │   ├── MainTopBar.kt               — Top app bar with title, balance, theme toggle
│   │   ├── NotificationAccessDialog.kt — Dialog asking user to grant notification access
│   │   ├── OptionPickerSheet.kt        — Generic bottom sheet for picking from a list of strings
│   │   ├── PermissionWarningBanner.kt  — Yellow banner shown when permissions missing
│   │   ├── SettingsTopBar.kt           — Top bar with back button (used in settings-style screens)
│   │   ├── TotalBalanceView.kt         — Shows net balance (credits - debits) in header
│   │   ├── TransactionFilterDropdown.kt — "Filter & Sort" dropdown (Merchant/Category/Date) — NOT fully wired
│   │   ├── TransactionRow.kt           — Card showing one transaction; long press → Edit/Delete
│   │   └── TransactionSortBar.kt       — Row of AssistChips (Merchant/Category/Date) — stub, onClick = {}
│   │
│   ├── config/
│   │   ├── ScreenConfig.kt             — Data class: title + showBalance flag
│   │   └── ScreenConfigMap.kt          — Map of route → ScreenConfig for each bottom nav tab
│   │
│   ├── navigation/
│   │   └── BottomNavItem.kt            — Sealed class defining 5 nav tabs
│   │
│   ├── theme/
│   │   ├── AppPalette.kt               — Enum: CALM, OCEAN, SUNSET
│   │   ├── AppPreferences.kt           — SharedPreferences wrapper; income, palette, template, smart category
│   │   ├── Theme.kt                    — ExpenseTrackerTheme; palette-based color schemes
│   │   └── CategorizeTransactionActivity.kt — (misplaced in theme folder) — "More" action activity
│   │
│   └── utils/
│       ├── InputFilters.kt             — Input sanitization: amount, name, category field limits
│       └── TransactionClassifier.kt    — isCredit() logic based on rawSmsText and amount sign
│
└── utils/
    ├── ParsedTransaction.kt            — Data class for parsed bank SMS/notification result
    ├── SmsParser.kt                    — Main parser: detects bank, amount, merchant, UPI ref from SMS text
    └── TransactionMapper.kt            — Extension: ParsedTransaction.toTransaction(category)
```

---

## NAVIGATION (Bottom Nav — 5 tabs)

| Tab | Route | Screen | Description |
|---|---|---|---|
| Records | `records` | RecordsScreen | Transaction history list |
| Analysis | `analysis` | AnalysisScreen | Charts and category breakdown |
| Budgets | `budgets` | BudgetsScreen | Budget progress per category |
| Accounts | `accounts` | AccountsScreen | Notification log / pending transactions |
| Categories | `categories` | CategoriesScreen | Manage categories |

**FAB** (floating button) on MainScaffold → navigates to `AddTransactionScreen`

**PlannerScreen** is NOT in the bottom nav — it's linked from MainScaffold as a separate route called `planner`

---

## KEY LOGIC

### Transaction Classification (TransactionClassifier.isCredit)
- `rawSmsText` contains "MANUAL_INCOME" → credit
- `rawSmsText` contains "MANUAL_TRANSFER" → credit
- `rawSmsText` contains "credited" (case-insensitive) → credit
- `amount` < 0 → credit (negative stored amounts are credits)
- Everything else → debit/expense

### Notification Flow
1. Bank app sends notification
2. `BankNotificationListener.onNotificationPosted()` receives it
3. `SmsParser.parse()` extracts amount, merchant, UPI ref
4. If UPI ref not already pending in DB → show notification
5. Notification has action buttons (Food, Transport, Shopping, Bills, + More)
6. Tapping a button → `CategoryActionReceiver` → saves to `transactions`, updates `notification_logs` status to LOGGED
7. Tapping "More" → `CategorizeTransactionActivity` → `CategoryOverlaySheet`

### Deduplication
- Only works if `upiReference` is present
- Checks `notification_logs` for existing PENDING with same UPI ref
- **Bug:** If `upiReference` is null, `return@launch` exits early — transaction is silently dropped

### SMS Parser (SmsParser.kt)
- Supports: HDFC, ICICI, SBI (UPI), Axis, Kotak, PhonePe, Google Pay, Paytm
- Detects: UPI, Card, NetBanking, Account transfers
- Returns `ParsedTransaction` with confidence score (0.3–0.95)

### Manual Entry (AddTransactionScreen)
- Types: INCOME, EXPENSE, TRANSFER
- Has built-in calculator keypad (supports +, -, *, /)
- Saves with: `rawSmsText = "MANUAL_INCOME"/"MANUAL_EXPENSE"/"MANUAL_TRANSFER"`
- Notes format: `"Account:CashName;user notes"`

---

## THEME SYSTEM
- **Dark/Light:** toggled via switch in top bar, saved to SharedPreferences
- **Palettes:** CALM (default blue-purple), OCEAN (teal), SUNSET (orange-red)
- **Dynamic color:** DISABLED (`dynamicColor = false`)
- Colors defined in `Theme.kt` per palette × dark/light
- Accent colors for planner: `FintechAccent`, `FintechDanger`, `FintechWarning` (defined in theme)

---

## KNOWN ISSUES / INCOMPLETE FEATURES

1. **BudgetsScreen — hardcoded limits:** Budget limits (e.g., Food=3000, Transport=2000) are hardcoded in the composable. Users cannot set their own limits.

2. **TransactionSortBar / TransactionFilterDropdown — not wired:** The filter chips and dropdown in RecordsScreen have `onClick = {}` — clicking them does nothing.

3. **TransactionSortBar stub:** `TransactionSortBar.kt` has chips with empty onClick handlers. Not connected to any filter logic.

4. **Silent drop bug:** In `BankNotificationListener`, if `upiReference` is null, the transaction is silently dropped (`return@launch`). Non-UPI transactions (card, netbanking) are never saved.

5. **Duplicate saveTransactionFromLog:** This private function is duplicated in both `AccountsScreen.kt` and `CategoryPickerDialog.kt`. Should be a shared utility.

6. **Category.kt enum vs CategoryEntity:** There are TWO category systems — the old `Category` enum (used in notification buttons) and the new `CategoryEntity` Room table. They can get out of sync.

7. **No ViewModel:** Database is accessed directly from Composables using `remember { AppDatabase.getDatabase(context) }`. This is an anti-pattern and makes testing hard.

8. **PlannerScreen not in bottom nav:** PlannerScreen exists and is fully built but is only reachable via the route `planner` — there's no nav item for it.

9. **CategorizeTransactionActivity.kt misplaced:** File is inside `ui/theme/` folder but its package declares `com.dhiraj.expensetracker.ui`. Should be in `ui/` folder.

10. **Emoji rendering issue:** Some emojis in `categoryIcon()` and `legendColor()` in screen files render as `??` — Unicode escape codes not applied correctly.

11. **BudgetsScreen — no real budget setting UI:** Users cannot add/edit/delete budgets. No budget data is stored in Room.

12. **AnalysisScreen — only shows last 7 days in weekly chart:** Monthly/yearly view not implemented.

---

## PREFERENCES (SharedPreferences — key: `expense_tracker_prefs`)
| Key | Type | Default | Purpose |
|---|---|---|---|
| smart_category_enabled | Boolean | true | Smart category recommendations |
| auto_settle_loans | Boolean | true | Auto-settle loans |
| reduce_motion | Boolean | false | Reduce animations |
| app_palette | String | "CALM" | Color palette |
| selected_template_id | Long | 1 | Active planner template |
| monthly_income_template_N | Double (as Long bits) | 0.0 | Income for template N |

---

## IMPORTANT RULES FOR AI (Do not break these)

1. **NEVER change the Room database version** without adding a proper Migration. Current version = 3.
2. **NEVER use XML layouts** — this app is 100% Jetpack Compose.
3. **NEVER add a ViewModel** unless explicitly asked — architecture is currently direct DB access from composables.
4. **NEVER rename routes** in BottomNavItem or NavHost — it will break navigation.
5. **NEVER remove the `@Volatile` or `synchronized` block** from AppDatabase.getDatabase() — it is thread-safe by design.
6. **NEVER change `isManualEntry` logic** — TransactionClassifier depends on `rawSmsText` flags, not `isManualEntry`.
7. **Category names are case-insensitive** in the DB (`COLLATE NOCASE`) — always use `.equals(name, ignoreCase = true)` in Kotlin comparisons.
8. **Do not add new dependencies** without checking compatibility with Kotlin 1.9.20 and Compose compiler 1.5.4.
9. **SmsParser is fragile** — do not refactor regex patterns without testing against real bank SMS formats.
10. **Always keep AppPreferences.ensureInitialized(context)** thread-safe — it's called from multiple places.

---

## WHAT IS DONE ✅
- Full Room DB with 6 tables, migrations, and seed data
- Auto-detect transactions from bank notifications (UPI, Card, NetBanking)
- Manual add expense/income/transfer with calculator
- Category management (add/edit/delete with emoji picker)
- RecordsScreen with edit/delete on long press
- AnalysisScreen with pie chart and weekly bar chart
- PlannerScreen with safe-to-spend, bills, loan tracker
- AccountsScreen with pending → logged flow
- Dark/Light theme toggle
- 3 color palettes (CALM, OCEAN, SUNSET)
- Deduplication via UPI reference
- Permission flow with dialog and warning banner

## WHAT IS NOT DONE / NEEDS WORK ❌
- Budget limits are hardcoded — needs user-editable budget setting UI
- Filter/sort in RecordsScreen is not wired
- PlannerScreen not accessible from bottom nav
- Non-UPI transactions silently dropped (card/netbanking bug)
- No ViewModel layer
- Emoji rendering bug in a few places
- AnalysisScreen only shows current week — no monthly/yearly selector
