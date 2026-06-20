# Release guide — Google Play & F-Droid

App ID: `io.github.aymericferreira.motioncues` · Version `1.0.0` (code `1`)

The store **title / short / full description / changelog** live in
`fastlane/metadata/android/en-US/` and are shared by both stores. This file covers the
store-specific extras and the manual console steps.

---

## Google Play

### Listing fields
- **App name (≤30):** `Motion Cues – Car Sickness`
- **Short description (≤80):** see `fastlane/.../short_description.txt`
- **Full description (≤4000):** see `fastlane/.../full_description.txt`
- **Category:** Health & Fitness  ·  **Tags:** motion sickness, car sickness, accessibility
- **Contact email:** (your support email)
- **Privacy policy URL:** `https://github.com/AymericFerreira/MotionCues/blob/main/PRIVACY.md`

### Graphics you still need to create
- App icon 512×512 PNG
- Feature graphic 1024×500 PNG
- At least 2 phone screenshots (overlay running over an e-reader/browser)

### Data safety form
- Data collected: **None.** Data shared: **None.** No data types, no analytics, no ads.

### Content rating
- Complete the IARC questionnaire → expected rating **Everyone**.
- Avoid medical claims ("treat"/"cure"); the listing says "may help reduce" only.

### Required permission declarations (Play Console → App content)
- **Foreground service (SPECIAL_USE):** justify as *"A user-toggled accessibility
  overlay that must stay visible over other apps while travelling; no other foreground
  service type applies."* This is the most likely review question — be ready for it.
- **Display over other apps (SYSTEM_ALERT_WINDOW):** core to the feature (drawing the
  motion-cue dots over the app you're reading).

### Signing & upload
The `release` signing config is already wired up — it reads a gitignored `keystore.properties`
(see `keystore.properties.example`). To produce a signed bundle:
1. Generate an upload keystore (keep it safe, never commit it — `*.jks` is gitignored):
   `keytool -genkey -v -keystore upload.jks -keyalg RSA -keysize 2048 -validity 10000 -alias upload`
2. Copy `keystore.properties.example` → `keystore.properties` and fill in the values.
3. `./gradlew bundleRelease` → `app/build/outputs/bundle/release/app-release.aab`. Upload it and
   enroll in Play App Signing.

Without `keystore.properties`, the release build still assembles but is unsigned (fine for CI).

---

## F-Droid

F-Droid builds from source on their servers — no APK upload, no signing by you.

1. Tag the release:  `git tag v1.0.0 && git push origin v1.0.0`
2. Use `docs/fdroid-metadata.yml` as the recipe (see the steps in its header comment):
   fork `fdroiddata`, add it as `metadata/io.github.aymericferreira.motioncues.yml`,
   `fdroid lint`, then open a merge request.
3. Descriptions/changelogs are auto-pulled from `fastlane/metadata/`.
4. The dependency tree is already fully FOSS (AndroidX/Compose/DataStore only — no Firebase,
   no Play Services), so there are no anti-features to declare.

---

## Ko-fi
The in-app support button opens `https://ko-fi.com/aymericferreira` (set in
`app/src/main/res/values/strings.xml`). **Create that Ko-fi page** (or change the handle in
`strings.xml` and the two URLs above if you pick a different one) before release.
