
# Sumador – Count bills and coins quickly

[![Version](https://img.shields.io/badge/version-1.3.0-blue.svg)](https://github.com/Christianrvdv/Sumador/releases/tag/v1.3.0)
[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](https://www.gnu.org/licenses/gpl-3.0)
[![Android](https://img.shields.io/badge/Android-8.0%2B-brightgreen)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9-purple)](https://kotlinlang.org)

**Sumador** is a simple and intuitive Android app for counting bills and coins.  
It is designed for merchants, cashiers, and anyone who handles cash – helping you keep track of totals quickly and accurately.

> ✨ **New in v1.3.0:** History with search & advanced filters, share sums, coin support, and keep screen on option.

---

## 📸 Screenshots

| Main screen | History | Filters |
|-------------|---------|---------|
| ![Main](screenshots/main.png) | ![History](screenshots/history.png) | ![Filters](screenshots/filters.png) |

*(Replace with actual screenshots)*

---

## 🚀 Features

| Feature | Description |
|---------|-------------|
| 💱 **Multiple currencies** | Cuban Peso ($), US Dollar (USD), Euro (€) |
| 🪙 **Coin support** | Toggle bills/coins on/off – perfect for detailed counting |
| 🔢 **Bill/coin order** | Ascending or descending order of denominations |
| 💾 **Auto‑save** | Amounts are automatically saved and restored on app restart |
| 🌗 **Themes** | Light, dark, and system default |
| 🌐 **Languages** | English and Spanish (system default also supported) |
| 📜 **History** | All saved sums with **search by name** and **advanced filters** (date range, amount range) |
| 📤 **Share** | Share sums with others via any app (WhatsApp, email, etc.) – includes detailed breakdown |
| 📱 **Keep screen on** | Prevents screen from turning off while using the app |
| 🧹 **Clear confirmation** | Optional confirmation before clearing all amounts |

---

## 🛠️ Technologies

- **Kotlin** – 100% Kotlin
- **Jetpack Compose** – Modern UI toolkit
- **Material 3** – Latest Material Design
- **Room** – Local database for saved sums
- **DataStore** – Persistent storage for user settings and auto‑saved amounts
- **Hilt** – Dependency injection
- **Coroutines & Flow** – Asynchronous operations and reactive UI

---

## 📥 Installation & Build

### Prerequisites
- Android Studio Iguana or newer
- JDK 17
- Android SDK (minimum API 26)

### Clone the repository
```bash
git clone https://github.com/Christianrvdv/Sumador.git
cd Sumador
```

### Build and run
1. Open the project in Android Studio.
2. Wait for Gradle sync to finish.
3. Connect an Android device or start an emulator.
4. Click **Run** (▶) or use the Gradle task:

```bash
./gradlew installDebug
 ```

### Generate a signed APK / AAB

```bash
./gradlew assembleRelease
```
The output will be in `app/build/outputs/apk/release/`.

---

## 🎮 Usage

1. **Counting** – Tap the **+** button next to each denomination to add bills/coins, or use the **-** button to remove.
2. **Total** – The total amount is updated in real time at the bottom.
3. **Clear** – Tap the trash icon to reset all counts (with confirmation if enabled).
4. **Save** – Save the current sum to history with a custom name.
5. **Share** – Share the current sum or any saved sum as text.
6. **History** – View, search, filter, edit or delete saved sums.
7. **Settings** – Access themes, currencies, language, sort order, auto‑save, confirmation, keep screen on, and coin toggle.

---

## 🤝 Contributing

Contributions are welcome! If you have an idea, find a bug, or want to improve the code:

1. Fork the repository.
2. Create a new branch (`git checkout -b feature/your-feature`).
3. Commit your changes (`git commit -m 'Add some feature'`).
4. Push to the branch (`git push origin feature/your-feature`).
5. Open a Pull Request.

Please ensure your code follows the project style and includes appropriate tests.

---

## 📄 License

This project is licensed under the **GNU General Public License v3.0** – see the [LICENSE](LICENSE) file for details.

---

## 👨‍💻 Author

**Christian R Vazquez**
- [GitHub](https://github.com/Christianrvdv)
- [Portfolio](https://christianrvdv.github.io/christianrvdv)

---

## ⭐ Support

If you find this app useful, please consider giving it a star ⭐ on GitHub to show your support.

---

## 📦 Download

You can download the latest APK from the [Releases](https://github.com/Christianrvdv/Sumador/releases) page or get it from the Google Play Store (if available).

---

*Built with ❤️ in Cuba.*
