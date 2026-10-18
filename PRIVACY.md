# Privacy Policy for TirePressure

**Last Updated:** October 18, 2026

Melvin Campos Casares ("we", "our", or "developer") built **TirePressure** as an open-source, privacy-first mobile application. This Privacy Policy informs users about our policies regarding the collection, use, and disclosure of personal information when using the TirePressure Android application.

---

### 1. Zero Data Collection

**TirePressure does not collect, store, transmit, or share any personal data.**

- **No Personal Information:** We do not collect names, email addresses, phone numbers, IP addresses, or device identifiers.
- **No Location Data:** We do not track or access your GPS or network location.
- **No Telemetry or Analytics:** We do not integrate any analytics frameworks (such as Google Analytics or Firebase Analytics) to track your in-app actions.
- **No Advertising:** The application contains zero advertisements and integrates no ad networks or tracking SDKs.
- **No User Accounts:** You do not need an account, password, or profile to use any feature of the application.

---

### 2. 100% Offline Architecture

TirePressure is engineered as a completely self-contained offline application:
- The Android permission `android.permission.INTERNET` is **not requested** in the application manifest.
- The app cannot communicate with any external server or network endpoint.
- All calculations (tire pressure physics, wheel configurations, unit conversions) are performed entirely on your device.

---

### 3. Local Data Storage & Backups

- All bike specifications, wheel setups, and user preferences (such as rider weight, preferred units, theme, and language) are stored exclusively in your device's private local sandbox database (Android Room and DataStore).
- **Manual Backups:** The JSON backup and restore feature utilizes Android's native **Storage Access Framework (SAF)**. When you create or restore a backup, you choose the destination file directly. TirePressure only accesses the file you explicitly select and does not request broad storage permissions (`READ_EXTERNAL_STORAGE` or `MANAGE_EXTERNAL_STORAGE`).

---

### 4. Children’s Privacy

Because TirePressure does not collect any personal data from any user, it does not knowingly collect or solicit personal information from children under the age of 13 (or under the age of digital consent in your jurisdiction).

---

### 5. Third-Party Services & Links

TirePressure does not integrate third-party SDKs, APIs, or analytics services. External links found in the "About" screen (such as links to GitHub or scientific tire deflection papers) will open in your default web browser under the respective privacy policies of those external websites.

---

### 6. Open Source Transparency

TirePressure is licensed under the permissive **MIT License**. The complete source code is public and open for independent security and privacy audits on GitHub:
[https://github.com/melvinmajor/TirePressure](https://github.com/melvinmajor/TirePressure)

---

### 7. Changes to This Privacy Policy

We may update this Privacy Policy from time to time. Any changes will be published in this repository with an updated revision date.

---

### 8. Contact Information

If you have questions or suggestions regarding this Privacy Policy or the security of TirePressure, please contact:

- **Author:** Melvin Campos Casares
- **GitHub:** [https://github.com/melvinmajor/TirePressure](https://github.com/melvinmajor/TirePressure)
- **Issue Tracker:** [https://github.com/melvinmajor/TirePressure/issues](https://github.com/melvinmajor/TirePressure/issues)

