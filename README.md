# Pocket Minder - Shopping List Location Reminder

A Java-based Android mobile application that helps you remember to buy items when you're near a supermarket.

## Features

### Version 1.0
- ✅ **Google Places API Integration**: Automatically finds nearby supermarkets using Google Places API
- ✅ **Shopping List Management**: Add, edit, and check off items from your shopping list
- ✅ **Location-Based Notifications**: Get notified when you're within 200m of a supermarket
- ✅ **Background Location Tracking**: Continuous location monitoring using foreground service
- ✅ **Persistent Background Tasks**: Uses WorkManager to ensure the app works even when killed
- ✅ **Boot Receiver**: Automatically restarts monitoring after device reboot
- ✅ **Battery Optimization**: Requests exemption from battery optimization for reliable background operation

## Architecture

### Components

1. **MainActivity**: Main UI for managing shopping list and controlling location tracking
2. **LocationTrackingService**: Foreground service for continuous location monitoring
3. **LocationCheckWorker**: WorkManager worker for periodic location checks (survives app kill)
4. **GooglePlacesAPI**: Integration with Google Places API for finding supermarkets
5. **ShoppingListDBHelper**: SQLite database for persistent shopping list storage
6. **NotificationHelper**: Manages all app notifications
7. **BootReceiver**: Restarts services after device reboot

### How It Works

1. **Foreground Service**: The app runs a foreground service that tracks your location every 30 seconds
2. **Proximity Detection**: When you're within 200 meters of a supermarket, you receive a notification
3. **WorkManager Backup**: Even if the app is killed, WorkManager checks your location every 15 minutes
4. **Smart Notifications**: Only notifies once per supermarket visit to avoid spam

## Setup Instructions

### Prerequisites

1. Android Studio Arctic Fox or later
2. Android SDK 24 (Android 7.0) or higher
3. Google Cloud Platform account for Google Maps API

### Getting Your Google Maps API Key

1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Create a new project or select an existing one
3. Enable the following APIs:
   - Maps SDK for Android
   - Places API
4. Go to "Credentials" and create an API key
5. Restrict the API key to Android apps (recommended)
6. Add your app's package name (`com.pocketminder`) and SHA-1 fingerprint

### Configuration

1. Clone this repository
2. Open the project in Android Studio
3. Open `gradle.properties` file
4. Replace `YOUR_GOOGLE_MAPS_API_KEY_HERE` with your actual Google Maps API key:
   ```
   GOOGLE_MAPS_API_KEY=AIzaSy...your-actual-key
   ```
5. Sync the project with Gradle files
6. Build and run the app

### Getting Your SHA-1 Fingerprint

For debug builds:
```bash
keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android
```

## Permissions

The app requires the following permissions:
- **ACCESS_FINE_LOCATION**: For precise location tracking
- **ACCESS_BACKGROUND_LOCATION**: For location tracking when app is in background
- **POST_NOTIFICATIONS**: For showing shopping reminders
- **FOREGROUND_SERVICE**: For running background location service
- **RECEIVE_BOOT_COMPLETED**: For restarting after device reboot

## Usage

1. **Add Items**: Tap "Add Item" to add items to your shopping list
2. **Start Tracking**: Tap "Start Tracking" to begin location monitoring
3. **Get Notified**: When you're near a supermarket with items on your list, you'll receive a notification
4. **Check Off Items**: Mark items as purchased by checking them off
5. **Clear Purchased**: Remove all checked items from your list

## Technical Details

### Minimum Requirements
- **Min SDK**: 24 (Android 7.0)
- **Target SDK**: 34 (Android 14)
- **Compile SDK**: 34

### Dependencies
- AndroidX AppCompat
- Google Play Services Location
- Google Play Services Maps
- WorkManager
- Material Design Components

### Database Schema

**Shopping List Table**:
- `id`: INTEGER PRIMARY KEY
- `item_name`: TEXT
- `quantity`: INTEGER
- `is_purchased`: INTEGER (0/1)
- `created_timestamp`: INTEGER

## Future Improvements

- Multiple shopping lists
- Custom notification radius
- Integration with more location types (grocery stores, pharmacies, etc.)
- Shopping list sharing
- Voice input for adding items
- Store-specific lists
- Price tracking and comparisons
- Offline mode support
- Widget support
- Dark mode theme

## Known Limitations

1. Requires constant internet connection for Google Places API
2. Battery consumption may be higher due to continuous location tracking
3. Notifications limited to supermarkets (no custom store types yet)
4. No cloud sync (all data stored locally)

## License

This project is licensed under the MIT License.

## Support

For issues, questions, or contributions, please open an issue on the GitHub repository.

## Changelog

### Version 1.0.0 (Initial Release)
- Basic shopping list functionality
- Location tracking with foreground service
- Google Places API integration
- Background persistence with WorkManager
- Boot receiver for auto-restart
- Proximity-based notifications
