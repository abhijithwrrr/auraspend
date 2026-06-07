# Agent Guidance for AuraSpend

This document provides context and guidelines for AI agents working on the AuraSpend codebase.

## 🏗 Architecture
AuraSpend follows **Clean Architecture** with an **MVI (Model-View-Intent)** pattern:
- **Data Layer**: `com.awbuilds.auraspend.data` (Room, DataStore, DriveSyncManager).
- **Domain Layer**: `com.awbuilds.auraspend.domain` (Use Cases, Models).
- **UI Layer**: `com.awbuilds.auraspend.ui` (ViewModels, Compose Screens).

## 🎨 UI/UX Standards
- **Material Design 3**: Follow M3 guidelines strictly.
- **Edge-to-Edge**: All screens must be edge-to-edge. Use `WindowInsets` for padding.
- **Animations**: Prefer spring physics over linear animations. Use Lottie for complex sequences.
- **AMOLED Theme**: Ensure `surface` and `background` colors are `#000000` when AMOLED mode is active.
- **Performance**: Avoid unnecessary recompositions. Use `@Stable` and `@Immutable` where appropriate.

## 🛠 Development Workflow
1. **Tasks**: Update the todo list using `TodoWrite` before starting any task.
2. **Commits**: Use descriptive commit messages.
3. **Verification**: Every feature must be verified on an Android 16 emulator.
4. **Documentation**: Keep `changelog.md` and `buglist.md` up to date.

## 🚩 Critical Constraints
- **Package Name**: `com.awbuilds.auraspend`
- **Target SDK**: Android 16
- **Branding**: Always include "Made with ❤️ by AW Builds" in appropriate locations.
