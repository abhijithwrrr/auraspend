# AuraSpend Changelog

## [Unreleased] — Design System & Documentation Update

### Added
- **Design System Audit**: Comprehensive audit covering Material 3 compliance, WCAG 2.1 AA accessibility, and design tokens
- **Design Tokens**: New `Type.kt`, `Shape.kt`, `Dimens.kt`, `Elevation.kt` with brand typography scale, shape tokens, spacing scale, and elevation tokens
- **Component Library**: Reusable components extracted to `ui/components/` — BalanceCard, StatCard, TransactionCard, EmptyStateCard, CategoryChip, PieChart, WeeklyChartCard
- **User Guide**: Comprehensive end-user help covering all 12 screens
- **Design System Guide**: Developer documentation for tokens, theming, and components
- **Accessibility Checklist**: Developer checklist for WCAG 2.1 AA compliance
- **Changelog**: This file

### Changed
- Theming architecture: inline shape/spacing values migrated to token files
- All 12 screens updated to reference design tokens instead of hardcoded values

### Fixed
- Icon content descriptions improved to ~100% coverage (accessibility)
- Canvas charts (bar chart, pie chart) now have screen-reader-accessible data descriptions
- Swipe-to-delete now has accessible fallback (long-press reveals delete button)
- Added `semantics` blocks to all interactive elements

### Accessibility (WCAG 2.1 AA)
- **Perceivable**: All icons and charts described for screen readers
- **Operable**: Keyboard navigation improvements, gesture fallbacks
- **Support**: TalkBack, Switch Access, and screen magnifier testing completed

### Known Issues
- Dynamic color (Monet) may produce low-contrast combinations on some OEM skins
- Canvas charts use custom drawing; future migration to M3 `LineChart`/`PieChart` recommended when stable

## Previous Versions

### v0.1.0 — Initial Release
- Core expense tracking via SMS parsing
- Dashboard with balance, charts, and budget progress
- Support for 9 Indian banks (SBI, HDFC, ICICI, Axis, Kotak, Yes, PNB, BOB, Canara)
- 12 default spending categories with 140+ merchant keywords
- Google Drive sync for backup/restore
- CSV import/export
- Material 3 theming with dynamic color (Android 12+)
- Dark mode and AMOLED theme
- Free and Play Store product flavors
