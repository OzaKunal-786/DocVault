package com.docvault.ui.navigation

import androidx.compose.runtime.Composable

/**
 * Navigation graph for the app.
 *
 * For now (Week 1), navigation is handled directly
 * in MainActivity using the AppViewModel state.
 *
 * We will add proper Compose Navigation in Week 2
 * when we have more screens to navigate between
 * (category detail, document viewer, etc.)
 *
 * WHY NOT USE IT NOW?
 * Our current flow is simple:
 *   PinSetup → Lock → Home
 * This is handled by AppViewModel switching states.
 * Adding Navigation framework for just 3 screens
 * would be overcomplicating things.
 */

// Navigation route names (we'll use these in Week 2)
object Routes {
    const val PIN_SETUP = "pin_setup"
    const val LOCK = "lock"
    const val HOME = "home"
    const val SEARCH = "search"
    const val SETTINGS = "settings"
    const val DOCUMENT_VIEWER = "document_viewer/{documentId}"
    const val CATEGORY_DETAIL = "category/{categoryName}"
    const val SCAN_RESULTS = "scan_results"
}