# 🗺️ DocVault Codebase Guide: How it all Works

Welcome to the internal map of DocVault. This guide explains the project structure, the purpose of every folder, and how the "magic" (AI and Security) actually happens under the hood.

---

## 📂 The High-Level Map

Everything important is inside `app/src/main/java/com/docvault/`. Here is the breakdown:

### 1. 🔐 `security/` — The Guardian
This is the most critical part of the app. It ensures no one can see your files without permission.
*   **`EncryptionManager.kt`**: The master controller. It generates the high-level keys used to unlock the database.
*   **`PinManager.kt`**: Manages your 4-6 digit PIN. It doesn't store the real PIN; it stores a "scrambled" version (Hash) so even we can't see it.
*   **`BiometricHelper.kt`**: The bridge to your phone's Fingerprint or Face ID sensor.
*   **`EncryptedFileManager.kt`**: This is where the real muscle is. It uses **AES-256-GCM** (military grade) to encrypt every single PDF file before it's saved. Each file gets its own unique key stored in the phone's hardware "Keystore."
*   **`AutoLockManager.kt`**: A timer that watches if you leave the app. If you switch to another app for too long, it automatically locks the vault.

### 2. 🧠 `ai/` — The Brain
This folder makes the app "smart." It handles everything related to understanding what a document actually is.
*   **`OcrEngine.kt`**: Uses Google ML Kit to "read" the text inside your images and PDFs. This runs 100% offline.
*   **`MetadataExtractor.kt`**: Scans the text found by the OCR to look for patterns like Dates (12/05/2024), Amounts ($150.00), or Invoice numbers.
*   **`TitleGenerator.kt`**: Takes the data from the extractor and gives your file a pretty name like `Amazon_Receipt_May_2024.pdf` instead of `IMG_8231.jpg`.
*   **`CategoryClassifier.kt`**: Looks for keywords (like "Hospital", "Statement", "Bill") to automatically sort the file into the right folder.

### 3. 📥 `data/` — The Memory
This is where the app remembers things.
*   **`database/AppDatabase.kt`**: The main database file. It uses **SQLCipher**, which means the entire database is encrypted on the disk. If someone steals the database file, they can't read a single word inside it.
*   **`database/DocumentEntity.kt`**: Defines what info we store about a document (Title, Category, Path, OCR Text, etc.).
*   **`repository/DocumentRepository.kt`**: The "Front Desk" for the database. Any screen that wants to read or save data must go through here.
*   **`repository/ImportManager.kt`**: The "Conveyor Belt." It coordinates the whole process: Image -> PDF -> OCR -> Renaming -> Categorizing -> Encrypting -> Saving to Database.

### 4. 📱 `ui/` — The Interface
Everything the user sees and touches.
*   **`screens/`**: Individual pages like `HomeScreen`, `LockScreen`, `SearchScreen`, and `OnboardingScreen`.
*   **`viewmodel/`**: These are the "managers" for the screens. They hold the data and handle the logic (e.g., "What happens when I click the Delete button?").
*   **`navigation/NavGraph.kt`**: The "GPS" of the app. It defines how you move from the Home screen to the Search screen or Settings.

### 5. 🛠️ `util/` & `pdf/` — The Toolbox
*   **`FileScanner.kt`**: Scans your phone's storage (Downloads, WhatsApp, etc.) to find documents.
*   **`PdfConverter.kt`**: A special tool that takes your photos and turns them into professional multi-page PDF documents.
*   **`BackupManager.kt`**: Bundles everything into an encrypted ZIP file so you can move your vault to a new phone.

---

## 🔄 The "Life of a Document" (The Flow)

If you want to understand the code, follow this journey of a single receipt:

1.  **Discovery**: `FileScanner` finds `receipt.jpg` in your Downloads folder.
2.  **Selection**: You tap "Import" on the `ScanScreen`.
3.  **The Pipeline**: `ImportManager` takes the file.
4.  **Conversion**: `PdfConverter` turns the JPEG into a PDF.
5.  **Reading**: `OcrEngine` reads the text "Starbucks Coffee $5.00".
6.  **Understanding**: `CategoryClassifier` sees "Coffee" and "Receipt" and tags it as **RECEIPTS**.
7.  **Naming**: `TitleGenerator` renames it to `Starbucks_Receipt_2024.pdf`.
8.  **Locking**: `EncryptedFileManager` encrypts the PDF so only DocVault can open it.
9.  **Saving**: `DocumentRepository` saves all this info into the encrypted database.
10. **Display**: The `HomeScreen` notices a new entry in the database and shows it in your "Recent" list.

---

## 🚀 Key Technologies Used
*   **Jetpack Compose**: For the modern, smooth UI.
*   **Room + SQLCipher**: For the encrypted database.
*   **Google ML Kit**: For on-device, private AI.
*   **Coroutines**: To make sure heavy AI work doesn't make the app lag.
*   **SAF (Storage Access Framework)**: To ask for folder permissions properly.

---

## 🛠️ How to Add a New Feature
*   **Want to add a new Category?** Update `DocumentCategory.kt` and add keywords to `CategoryClassifier.kt`.
*   **Want to change the UI?** Look in `ui/screens/`.
*   **Want to change the Security?** Look in `security/`.

*Built for privacy. Powered by AI.*
