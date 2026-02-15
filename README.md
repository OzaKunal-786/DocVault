🔒 DocVault — AI Document Organizer
Your documents. Organized by AI. Encrypted on your phone.

DocVault scans your phone storage and gallery, finds documents,
reads them with OCR, auto-categorizes them, converts everything
to PDF, and stores it all locally with AES-256 encryption.

No cloud. No accounts. No data leaves your device.

Features (v1 MVP)
📸 Scan phone storage and gallery for documents
🤖 AI-powered auto-categorization (8 categories)
📝 OCR text extraction (Google ML Kit)
📄 Auto-convert images to PDF
🔐 AES-256 encrypted storage
🔑 Biometric + PIN lock
🔍 Full-text search across all documents
📤 Share to/from any app
📷 Camera capture for paper documents
💾 Encrypted backup & restore
🚫 100% offline — no internet required
Document Categories
CategoryExamples
📋 ID & PersonalPassport, License, Aadhaar
💰 FinancialBank statements, Tax docs
🧾 ReceiptsShopping, Restaurant, Online orders
🏥 MedicalPrescriptions, Lab reports
🎓 EducationCertificates, Marksheets
🚗 VehicleRC, Insurance, PUC
🏠 PropertyRent agreement, Utility bills
📄 OtherEverything else
Tech Stack
ComponentTechnology
LanguageKotlin
UIJetpack Compose (Material 3)
DatabaseRoom + SQLCipher
OCRGoogle ML Kit
SecurityAndroidX Biometric + Security-Crypto
Min SDK26 (Android 8.0)
Target SDK34 (Android 14)
Project Structure
com.docvault
├── ui/              # Jetpack Compose screens
│   ├── theme/       # Colors, typography, theme
│   ├── screens/     # All app screens
│   ├── components/  # Reusable UI components
│   └── navigation/  # Navigation graph
├── data/            # Data layer
│   ├── database/    # Room entities, DAOs, database
│   ├── models/      # Data classes
│   └── repository/  # Repository pattern
├── scanner/         # File scanning logic
├── ai/              # OCR + classification
├── security/        # Encryption, biometric, PIN
├── pdf/             # PDF conversion
└── util/            # Helper functions

Building
Clone the repo
Open in Android Studio Hedgehog or newer
Sync Gradle
Run on device (API 26+)
License
MIT
