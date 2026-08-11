# SpendWise — Native Android Expense & Budget Tracker

SpendWise is a personal finance management and expense tracking Android application built with modern **Kotlin** and **Jetpack Compose (Material 3)**.

---

## Key Features

1. **Transaction Timeline & Dashboard**
   - Live Total Balance, Monthly Income, and Monthly Expense summary cards.
   - Date-grouped transactions list (Today, Yesterday, full dates).
   - Fast instant search across titles, categories, and notes.
   - Quick filter chips (All, Expense, Income).

2. **Navigation Drawer (Sidebar)**
   - Material 3 `ModalNavigationDrawer` containing secondary destinations:
     - **Timeline & Home**
     - **Analytics & Charts**
     - **Monthly Budgets**
     - **Categories Manager**
     - **Backup & Export (CSV)**
     - **Settings & Preferences**

3. **Visual Analytics & Interactive Charts**
   - Month-by-month historical switcher.
   - Donut / Pie Chart category breakdown with percentage legend.
   - Daily spending trend bar chart.
   - Net savings calculation.

4. **Monthly Category Budgets**
   - Set monthly spending thresholds per expense category.
   - Visual progress bars color-coded green, amber, and red.
   - Instant over-budget warnings and remaining allowance calculation.

5. **Custom Category Management**
   - Create custom income and expense categories.
   - Color picker with curated fintech tones.
   - Icon picker with Material Design symbols.

6. **Data Privacy, Backup & CSV Tools**
   - 100% Offline & Private — all data stored locally on device with SQLite via Room.
   - Export all transactions to CSV via Android Storage Access Framework.
   - Import transactions from CSV.
   - One-tap data reset with confirmation.

7. **Preferences & Customization**
   - Multi-currency support (USD `$`, EUR `€`, GBP `£`, INR `₹`, JPY `¥`, CAD `CA$`, AUD `AU$`, BRL `R$`, NGN `₦`, PHP `₱`).
   - Theme Mode toggle (System Default, Light, Dark).

---

## Tech Stack & Architecture

- **Language**: Kotlin 2.0+
- **UI Framework**: Jetpack Compose (Material 3)
- **Architecture**: Clean MVVM + Repository Pattern
- **Local Persistence**: Room Database (SQLite) + Jetpack DataStore
- **Dependency Injection**: Manual DI via `AppContainer` (Service Locator)
- **Asynchrony**: Kotlin Coroutines & StateFlow

---

## How to Build & Run

1. Open the `SpendWise` project in **Android Studio Ladybug / Iguana / Koala** or later.
2. Ensure JDK 17 or 21 is configured.
3. Sync Gradle and run on an Android device or emulator running Android 8.0+ (API 26+).
