# SpendWise Project Rules & AI Automation Protocol

## 1. AI Data Entry & Accuracy Rules
1. **Accurate Naming & Specificity**:
   - Kabhi bhi purane data ke pattern se name overwrite ya guess mat karo. User ne jo kaha hai ya receipt pe jo likha hai wahi exact name aur specific details daalo.
   - Example: Sunil ko courier charges diya toh title `Sunil - Trackon Courier` hoga, sirf `Sunil` ya sirf `trackon` nahi.

2. **Smart Categorization & Account Mapping**:
   - Har transaction me context ke hisaab se sahi Category (`Food`, `Shopping`, `Transport/Courier`, `Bills`, etc.) aur Payment Account (`Cash`, `Bank`, `UPI`) match karo.
   - Agar specific category nahi milti toh relevant category create ya assign karo.

3. **Receipt Date Precision**:
   - Agar receipt/slip di gayi hai, toh entry hamesha slip ki date/time ke according timestamp ke saath hogi, current system time ke saath nahi.

4. **Zero Accidental Delete / Safety Code**:
   - Kabhi bhi koi transaction direct delete nahi karna.
   - Delete operation ke liye explicit security code ya user confirmation zaruri hai (`ALLOW_DELETE=true` ya confirmation code).

5. **AI Entry Verification & Audit**:
   - AI dwara add kiye gaye transactions me note me `[AI Entry: Verified]` ya context details honi chahiye taaki user app me filter/verify kar sake.

---

## 2. Full App AI Access & Management
AI agent ko SpendWise me poora control hai:
- **Transactions**: Read, Add, Update (with date, exact title, category, account, tags, note).
- **Accounts**: Naye accounts banana (`UPI`, `Cash`, `Bank`), balances track karna.
- **Budgets & Goals**: Category-wise budget set karna aur monthly spending compare karna.
- **Categories**: Dynamic category categorization aur management.

---

## 3. Android Build, CI/CD & APK Delivery Workflow
1. **Persistent Keystore & Signing**:
   - `app/spendwise.keystore` configured in `signingConfigs` for debug & release.
2. **Local APK Delivery**:
   - Release APK automatic download to `/sdcard/Download/SpendWise-v<Version>.apk`.
