# 🔒 DocVault — The AI-Powered Private Document Vault

[![Platform](https://img.shields.io/badge/Platform-Android-brightgreen.svg)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9.22-blue.svg)](https://kotlinlang.org)
[![Security](https://img.shields.io/badge/Security-AES--256--GCM-orange.svg)](https://en.wikipedia.org/wiki/Galois/Counter_Mode)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

> **"Your documents. Organized by AI. Encrypted on your phone."**

DocVault is a privacy-first, local-only document manager that turns your phone into a secure, intelligent vault. It automatically finds your IDs, receipts, and bills, converts them to encrypted PDFs, and organizes them using on-device AI. 

**No Cloud. No Accounts. No Data Collection.**

---

## 🌟 Features at a Glance

### 🧠 On-Device Intelligence
*   **Auto-Categorization:** AI automatically sorts documents into 8 categories (Financial, Medical, ID, etc.).
*   **Smart Titles:** Generates meaningful names like `Amazon_Receipt_2024-05-20.pdf` instead of `IMG_0001.jpg`.
*   **Offline OCR:** Full-text recognition using Google ML Kit. Search for *any* word inside *any* document.

### 🔐 Military-Grade Security
*   **Full Encryption:** Every file is encrypted with AES-256-GCM.
*   **Secure Database:** Metadata stored in a SQLCipher-encrypted Room database.
*   **Hardware-Backed Keys:** Keys are stored in the Android Keystore system.
*   **Biometric Lock:** Secure your vault with Fingerprint or Face Unlock.

### 📄 Document Management
*   **File Scanner:** Automatically detects documents in Downloads, WhatsApp, and Gallery.
*   **PDF Engine:** High-quality conversion of images into professional PDF documents.
*   **Full-Text Search:** Instant search across all indexed text and metadata.
*   **Secure Backup:** Export an encrypted ZIP of your entire vault.

---

## 🛠️ Tech Stack

*   **UI:** Jetpack Compose (Material 3)
*   **Architecture:** MVVM + Repository Pattern
*   **Database:** Room + SQLCipher (Encrypted SQLite)
*   **AI/ML:** Google ML Kit (Text Recognition)
*   **Security:** AndroidX Biometric, Security-Crypto, AES-GCM
*   **Concurrency:** Kotlin Coroutines & Flow

---

## 📁 Project Structure

```text
com.docvault
├── ai/              # OCR, Metadata Extraction, Classification
├── data/            # Room DB, SQLCipher, Repository, Import logic
├── scanner/         # MediaStore storage scanning
├── security/        # EncryptionManager, BiometricHelper, KeyStore logic
├── ui/              # Compose Screens, ViewModels, Navigation
├── pdf/             # Image-to-PDF conversion logic
└── util/            # Camera, Backup, and File helpers
```

---

## 🚀 Getting Started

1.  **Clone the Repository**
2.  **Open in Android Studio** (Hedgehog or newer recommended)
3.  **Build & Run** on a device running Android 8.0 (API 26) or higher.
4.  **Accept Storage Permissions** to allow the AI to find your existing documents.

---

## 🤝 Contributing

Contributions are welcome! Whether it's a bug fix, a new feature, or better AI patterns, feel free to open a Pull Request.

---

## ⚖️ License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

*Built with ❤️ for privacy and organization.*
