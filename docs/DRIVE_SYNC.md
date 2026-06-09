# Google Drive Backup & Restore

AuraSpend uses **Google Drive API v3** for cloud backup and restore. This is a premium feature.

## Authorization Flow

1. User taps "Backup" or "Restore" in Settings
2. `GoogleSignInClient` prompts account selection (OAuth 2.0)
3. User grants access to Google Drive (`appdata` scope)
4. Access token is obtained via `GoogleAccountCredential`
5. The app reads/writes to the hidden `appDataFolder` (per-user, not visible in Drive UI)

**Note**: `WEB_CLIENT_ID` must be set in `secrets.properties` for OAuth to work.

## DriveSyncManager

**File**: `data/remote/DriveSyncManager.kt`

### Backup

1. Serializes all data (transactions, categories, budgets, subscriptions) to JSON via `BackupSerializer`
2. Uploads to `appDataFolder` as `AuraSpend_Backup.json`
3. Google Drive API uses `Files.create()` with `application/vnd.google-apps.file` MIME type

### Restore

1. Lists files in `appDataFolder` with name `AuraSpend_Backup.json`
2. Downloads the most recent version
3. Deserializes JSON back into entity lists
4. Replaces all local data in Room database

## Data Format

Backup JSON structure:

```json
{
  "transactions": [...],
  "categories": [...],
  "budgets": [...],
  "subscriptions": [...]
}
```

## Limitations

- **One backup file**: Each backup overwrites the previous one
- **No conflict resolution**: Restore replaces all local data
- **No encryption**: Data is stored as plain JSON in the user's Drive
- **No scheduled backups**: Manual only via Settings
- **Network required**: Both backup and restore require internet access
