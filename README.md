# Vialo

**One-tap navigation for the people who need it most.**

Vialo is a mobile app designed for elderly users and anyone with limited technology exposure. It removes the friction of searching, verifying, and navigating through Google Maps by turning frequently visited places into simple, large home-screen shortcuts that open turn-by-turn navigation in a single tap.

## The Problem

Using Google Maps today requires multiple steps:

1. Open the app and type a destination into the search bar.
2. Pick the correct result from a list of suggestions.
3. Tap "Directions," choose a travel mode, and then tap "Start."

For many elderly users, each of these steps is a potential point of confusion — small text, unfamiliar UI patterns, and the fear of tapping the wrong thing. The result is that they either avoid going to new places or rely on someone else to set up navigation every time.

## The Solution

Vialo lets users (or their family and friends) pre-configure a set of named shortcuts — **Home, Hospital, Bank, Grocery Store, Temple**, or any place that matters — displayed as large, clearly labeled buttons on the home screen. Tapping a shortcut immediately opens Google Maps with navigation to that destination. No searching, no verifying, no extra taps.

### Key Features

- **One-Tap Navigation** — Each shortcut opens Google Maps directions directly, skipping search and confirmation entirely.
- **Home-Screen Widget** — A grid of large, labelled tiles you can pin on the home screen. The widget reflows the tiles to fit the size you give it. Two palettes to choose from: **Bold** (colourful) and **Grey**.
- **Simple, Senior-Friendly UI** — Large buttons, high-contrast text, and a minimal interface designed for readability and ease of use.
- **Family & Friend Sharing** — Shortcuts can be shared via WhatsApp or any messaging app as a simple link. When the recipient taps the link, Vialo opens with a one-tap confirmation to add the shortcut to their home screen.
- **Easy Setup** — Adding a new shortcut is as simple as searching for a place once and giving it a name. Family members can set up shortcuts remotely by sharing them.
- **Customizable Labels & Icons** — Each shortcut can have a friendly name and Phosphor duotone icon (e.g., a house for Home, a hospital cross for Hospital) so users recognize destinations at a glance.

### How Sharing Works

1. A family member creates a shortcut (e.g., "City Hospital") in their own app.
2. They tap **Share** and send it via WhatsApp or any messaging app.
3. The elderly user receives a link, taps it, and confirms with a single tap to add the shortcut to their home screen.
4. From that point on, one tap is all it takes to navigate there.

This means a son or daughter in another city can set up their parent's entire shortcut collection without being physically present.

## Who Is This For?

- **Elderly users** who find multi-step app navigation confusing or intimidating.
- **Caregivers and family members** who want to simplify technology for their loved ones.
- **Anyone** who wants faster, friction-free navigation to their regular destinations.

## Tech Stack

Vialo is a native Android app (Kotlin + Jetpack Compose):

- **Kotlin & Jetpack Compose** with Material 3 — UI, navigation, theming
- **Room** for the shortcut database; **DataStore** for app preferences
- **WorkManager** for scheduled expiry-warning notifications
- **Ktor** HTTP client over **OpenStreetMap Nominatim** for place search and reverse geocoding (no API key)
- **Phosphor duotone icons** throughout the app and home-screen widget (`com.adamglin:phosphor-icon`)
- **Native AppWidget** (`RemoteViews`) for the home-screen widget — a fixed 6×2 grid of tiles, column count chosen at runtime from the widget's current width, in Bold or Grey palettes
- Android **share sheet** + a GitHub Pages redirect page (`docs/index.html`) for shareable links
- **Verified App Links** via `assetlinks.json` on the GitHub Pages domain so shared HTTPS links open the app directly

The Android Studio project lives under [`android/`](android/).

## Building

```bash
cd android
./gradlew assembleDebug
```

Or open `android/` in Android Studio and let Gradle sync — the wrapper at `android/gradlew` will fetch Gradle 8.10.2 on first run.

Min SDK: 26 (Android 8). Target SDK: 35.

## License

This project is open source. License details to be added.
