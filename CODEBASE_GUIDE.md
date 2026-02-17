# 🗺️ DocVault Codebase Guide: Advanced AI & Security Architecture

Welcome to the internal map of DocVault. This guide explains the project structure, the purpose of every folder, and how the "magic" (AI and Security) actually happens under the hood. My First ever project.

---

## 📂 The High-Level Map

Everything important is inside `app/src/main/java/com/docvault/`. Here is the breakdown:

### 1. 🔐 `security/` — The Guardian
This is the most critical part of the app. It ensures no one can see your files without permission.
*   **`EncryptionManager.kt`**: The master controller. It generates the high-level keys used to unlock the database.
*   **`PinManager.kt`**: Manages your **6-digit PIN**. It doesn't store the real PIN; it stores a "scrambled" version (Hash) so even we can't see it.
*   **`BiometricHelper.kt`**: The bridge to your phone's Fingerprint or Face ID sensor. Supports detailed error handling for lockouts.
*   **`EncryptedFileManager.kt`**: Uses **AES-256-GCM** (military grade) to encrypt every single PDF file before it's saved. Each file gets its own unique key stored in the phone's hardware "Keystore." Properly flushes streams to prevent file corruption.
*   **`AutoLockManager.kt`**: A timer that watches if you leave the app. If you switch to another app for too long, it automatically locks the vault.

### 2. 🧠 `ai/` — The Brain
This folder makes the app "smart." It handles everything related to understanding what a document actually is.
*   **`OcrEngine.kt`**: Uses Google ML Kit to "read" the text inside your images **and PDFs**. This runs 100% offline.
*   **`MetadataExtractor.kt`**: Scans the text found by the OCR to look for patterns like Dates, Amounts, or Invoice numbers.
*   **`TitleGenerator.kt`**: Uses a priority-based system to give your file a pretty name like `Amazon_Invoice_2024.pdf`.
*   **`CategoryClassifier.kt`**: Uses **High-Confidence Exclusive Keywords**. It prioritizes terms like "Passport" or "Prescription" to ensure accurate sorting into folders like ID & Personal or Medical.

### 3. 📥 `data/` — The Memory
This is where the app remembers things.
*   **`database/AppDatabase.kt`**: The main database file. It uses **SQLCipher**, which means the entire database is encrypted on the disk.
*   **`database/DocumentEntity.kt`**: Defines document info (Title, Category, Path, OCR Text, etc.) with indexes for performance.
*   **`database/DocumentFtsEntity.kt`**: A dedicated Full-Text Search table for instant, powerful search across all documents.
*   **`database/CategoryEntity.kt`**: Supports user-defined **Custom Categories**.
*   **`repository/ImportManager.kt`**: The "Conveyor Belt." It coordinates the whole process: Image -> PDF -> OCR -> Renaming -> Categorizing -> Encrypting -> Saving. Uses a semaphore to handle high-volume imports safely without crashing.

### 4. 📱 `ui/` — The Interface
Everything the user sees and touches.
*   **`screens/`**: Individual pages like `HomeScreen`, `LockScreen`, `PdfViewerScreen` (internal reader), and `OnboardingScreen`.
*   **`viewmodel/`**: `AppViewModel` handles global state, selection mode, search query, and monitored folders.
*   **`navigation/NavGraph.kt`**: The "GPS" of the app. Manages transitions between Home, Category Details, and Settings.

---

## 🔄 The "Life of a Document" (The Flow)

1.  **Discovery**: You pick a folder or snap a photo.
2.  **Import**: `ImportManager` processes the file in a background chunked queue.
3.  **Enhancement**: `PdfConverter` enhances the image (scanned document look) while preserving color and fitting it to A4.
4.  **AI Analysis**: `OcrEngine` reads the text (even from PDFs). `CategoryClassifier` and `TitleGenerator` assign metadata.
5.  **Security**: `EncryptedFileManager` locks the PDF with AES-256.
6.  **Storage**: The info is saved to the encrypted SQL database.

---

## 🚀 Key Technologies Used
*   **Jetpack Compose (Material 3)**: Modern, fluid UI with a Bottom AppBar layout.
*   **Room + SQLCipher**: Encrypted SQLite storage.
*   **FTS4**: High-performance full-text search.
*   **Google ML Kit**: Private, offline AI for OCR.
*   **SAF (Storage Access Framework)**: Proper, recursive folder permissions.

*Built for privacy. Powered by AI.*
