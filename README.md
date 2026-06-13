# skip
A auto ad skipper.
# Skip ⏭️ 🧮

A clever 2-in-1 native Android utility. On the surface, it functions as a standard, fully operational calculator. In the background, it utilizes Android Accessibility Services to automatically skip YouTube Music advertisements.

## ✨ Features
* **Stealth Frontend:** A clean, functional calculator UI that leaves no footprint of its secondary purpose.
* **Automated Backend:** Silently detects and interacts with the "Skip Ad" buttons on YouTube Music using the Accessibility API.
* **Lightweight:** Highly optimized native Android build (approx. 5MB).
* **Privacy First:** Operates entirely locally on your device with no unnecessary network permissions or background tracking.

## 🚀 Installation & Setup
Since this application utilizes the Accessibility API for non-standard purposes, it is not available on the Google Play Store.

1. Go to the [Releases](../../releases) tab of this repository.
2. Download the latest `app-release.apk` file.
3. Install the APK on your Android device (you may need to allow installations from "Unknown Sources").
4. **Crucial Step:** Open your device **Settings > Accessibility > Installed Apps > Skip** and toggle the service **ON**. The backend will not function without this permission.

## 🛠️ Build it Yourself
To build this project from source:
1. Clone the repository.
2. Open the project in Android Studio.
3. Sync Gradle and build the project.
4. Note: Keystore files and local configurations have been excluded for security purposes.

## ⚠️ Disclaimer
This application was created for educational purposes to explore Android Accessibility Services and background processing. It is not affiliated with or endorsed by YouTube or Google.

---
**Developer:** Beast
**License:** [MIT License](LICENSE)
