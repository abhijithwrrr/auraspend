# Changelog

All notable changes to this project will be documented in this file.

## [0.2.1] - 2026-06-08
- Fixed theme switching: explicit LIGHT/DARK/AMOLED choice is now always respected and no longer overridden by system dark mode.
- Fixed onboarding showing on every launch: completion flag is now persisted in SharedPreferences.
- Fixed onboarding screen background: added explicit background to prevent transparent/black overlay in light mode.
- Redesigned splash screen: shows app icon (purple circle with white "A") centered with fade + scale animation.
- Changed to Material 3 expressive: custom typography with bold headlines, semi-bold titles, generous sizing. Disabled dynamic color by default so brand color schemes are always used.

## [0.2.0] - 2026-06-07
- Completely redesigned website (landing page, privacy policy, terms of service).
- Industry-standard Privacy Policy with GDPR/CCPA coverage, data retention, and security sections.
- Industry-standard Terms of Service with 14 comprehensive sections including arbitration, class action waiver, and indemnification.
- Modern Material 3 landing page with 9 feature cards, hero stats, open-source section, and call-to-action.
- Added `.nojekyll` for GitHub Pages deployment support.
- Removed empty `assets/` directory from docs.

## [0.1.0] - 2026-05-13
- Initial project setup and documentation.
- Implemented Android 16 Edge-to-Edge and Material 3 Theme Engine (including Pure AMOLED).
- Built Core Data layer with Room Database (Transactions, Categories, Budgets).
- Developed MVI-based Dashboard, Transaction List, and Add Transaction screens.
- Implemented Lottie-powered Splash and Onboarding flows.
- Integrated Shared Element Transitions for fluid navigation.
- Added Google Drive Sync manager.
- Removed AdMob integration (replaced with premium feature gating).
- Developed professional landing page and legal documentation in `/docs`.
