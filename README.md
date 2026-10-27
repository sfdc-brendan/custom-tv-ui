# Command Center for Insignia Fire TV

A native Android app built for the user's Insignia NS-32DF310NA19, Fire OS 6.7.1.1 (Android 7.1.2 / API 25). Package: `local.commandcenter.tv`. It runs on the TV itself. No local web server, Raspberry Pi, or computer is required after installation.

## Version 0.6 — daily trend charts

CRM now includes the regular-session intraday line using Yahoo's one-minute closing samples (`interval=1m&range=1d&includePrePost=false`). This is a minute-by-minute trend, not a trade-by-trade tape. Each refresh retrieves the day's available history, so opening the app later does not lose the morning. The provider's regular-session timestamps define the horizontal range when they match the data date; otherwise the available sample range is used and the historical date is shown. No future prices are drawn.

Weather includes hourly model temperatures from midnight through the following midnight, with a Now marker and dashed future portion. The entire weather line is model data, including earlier hours; it is not a personal station's observation history. The hourly series is requested with the existing current/five-day weather response. Reference: [Open-Meteo hourly temperature definitions](https://open-meteo.com/en/docs).

Charts use native Canvas drawing, timestamp-proportional horizontal axes, labeled auto-scaled vertical bounds, and straight segments without smoothing. Missing/null values and missing time intervals create gaps. Isolated valid samples are dots; unavailable chart data is labeled explicitly. Series are stored in the existing JSON cache. Existing source timestamps, stale/failure indicators and 30s/60s polling remain in place.

The layout gives weather/CRM more width and slightly reduces value typography and lower-strip spacing to retain the five-day outlook. Existing remote controls remain hidden behind Menu/Select.

Verification: actual TV loaded 391 stock samples and 25 weather samples. `tests/TrendSeriesTest.java` passed domain filtering, missing values/timestamps, flat/empty data and timestamp-order checks. Inspected `verification/charts-final-v6.png`, including explicit future-weather dashes. `TrendChart.java` draws dash segments itself because the first Paint-based dashed line rendered solid on this Fire TV. Charts restored after an app update/restart. Full market-day progression, session rollover/holidays, and visual network-error transitions remain unverified.

## Version 0.5 — five-day outlook and daytime context

A five-day strip shows today plus the next four days, with condition icons/text, high and low temperatures, and daily precipitation probability. It uses the existing Open-Meteo request, expanded to `forecast_days=5` and daily weather code, sunrise, and sunset. No extra service or account is required. Forecast details follow the same minute refresh, cache, failure, and stale-state behavior as current weather. Older one-day caches remain compatible while a new forecast loads. All five dates and required values are validated before replacing the display/cache.

The clock column now shows sunrise/sunset and remaining daylight. After sunset, the next available sunrise is displayed. Day names and daylight context are recalculated in America/New_York, so cached dates are not silently relabeled as today.

CRM shows time remaining until the provider-reported regular session closes, an upcoming opening time when available, or the session's end time after closing. It does not invent the next trading day or assume every weekday is a normal session. Existing delayed/stale-quote labels remain visible.

The forecast's daily condition describes the day's most severe modeled condition, so it can differ from the current weather above. Fog, snow, and storms have distinct native icons. Reference: [Open-Meteo daily field definitions](https://open-meteo.com/en/docs).

Verified on the TV: forecast loading, five-day layout, daylight remaining, post-session market label, Menu opening controls and Back returning to the forecast. Screenshot: `verification/forecast-final-v5.png`. Before-sunrise, after-sunset, active-session countdown, overnight rollover, and malformed forecast responses were not independently exercised on the TV.

## Version 0.4 — frequent checks and honest freshness

CRM is checked every 30 seconds and weather every 60 seconds while the dashboard is visible, using separate worker requests. The scheduler checks due work every five seconds using monotonic time; a slow request never overlaps another request for the same feed. Successful checks display a separate age from the provider's data timestamp. Cached content is not marked as freshly checked.

Quotes more than 60 seconds old during the provider-reported regular session receive an amber warning (evaluated every five seconds). Outside that reported session, the display explicitly identifies the last regular-session quote. Extended-hours prices are not included. Weather is labeled as 15-minute model data, with an amber stale marker after 30 minutes. This is a source-time freshness indication, not a promise of minute-by-minute weather measurements or quote delivery.

Failures retain validated saved values and show a failure/retry message. Retries back off independently up to five minutes; successful checks reset backoff. Manual Refresh respects failure backoff. Invalid or out-of-order responses do not replace current values or the saved cache. Local HTTP caching is disabled, but upstream providers may still cache or delay data.

The TV has automatic date/time enabled; the dashboard uses its clock in America/New_York. There is no guaranteed clock-accuracy or market-data latency SLA. Open-Meteo documents its model-based current conditions at https://open-meteo.com/en/docs.

Verified: `tests/FeedStateTest.java` exercises polling independence, overlap prevention, retry/recovery, cached labels and check ages. Actual-TV screenshot: `verification/freshness-v4.png`. Automatic retrieval logs: `verification/freshness-v4.log`.

## Version 0.3 — ambient dashboard and restrained motion

The default view shows date/time, Charlotte weather, and CRM, with generous spacing and a small remote hint. Apps and HDMI inputs are tucked into a bottom controls panel. Panels fade and move 12dp into place over 220ms, and exit 6dp over 140ms. Changed clock, temperature, price, and daily-change values fade from 72% opacity over 200ms; values update immediately without simulated intermediate numbers. Initial content and unchanged values do not animate. Remote focus remains immediate and high contrast.

Options includes a persisted Reduce motion preference. The app also respects Android's animator-duration setting when disabled. There are no decorative loops, continuously scrolling tickers, or new dependencies.

Preview: `verification/ambient-v3.png`. Review notes: `UI_REVIEW.md`.

## Use

- Press **Menu or Select** from Today to open apps and inputs.
- Use the arrow keys and Select to choose an app, HDMI input, Refresh, or Options.
- **Back or Menu** closes a panel and returns to Today.
- In **Options**, choose **Reduce motion** to disable the app's transitions; choose **Enable subtle motion** to restore them. This preference survives app restarts.
- Select HDMI 1, 2, or 3. While viewing an input, Left/Right cycles through all three ports. Back returns to Today.
- Home continues to open Amazon's home screen.
- Options also controls whether the dashboard stays awake and provides manual refresh and source information.
- Missing apps offer an Appstore link. Downloads and sign-in remain user actions.
- Prime Video uses `com.amazon.firebat`, Netflix uses `com.netflix.ninja`, and Silk uses `com.amazon.cloud9`.
- Reopen **Command Center** through the TV's apps, or **Settings → Applications → Manage Installed Applications → Command Center → Launch application**. It can be removed through the same settings screen.

## Data and behavior

Weather uses Open-Meteo with Charlotte city coordinates (35.22709, -80.84313), returned by its ZIP 28277 lookup. This is area weather, not a precise address forecast. Attribution: [Open-Meteo](https://open-meteo.com/) / CC BY 4.0; geocoding from GeoNames.

CRM quotes use Yahoo Finance's public chart endpoint. This is an unofficial integration, may be delayed, and could stop working if the endpoint changes. Each quote shows the provider's timestamp. Daily change uses the one-day response's previous close, not the five-day starting price. No simulated prices are used.

CRM checks every 30 seconds and weather every minute while foregrounded. Validated responses are cached on the TV. Network failures preserve prior values with an explicit failure label and retry backoff. The clock uses America/New_York. Network work runs off the UI thread with connection/read timeouts. See version 0.4 freshness limitations above.

Keeping the display awake is requested only while the dashboard/input viewer is visible. There is no daily schedule, automatic startup, system-wide overlay, custom Home-button mapping, or automatic return from streaming apps in this version. Full-day idle behavior, HDMI audio, HDCP content, and Netflix/Apple TV playback have not been verified. This is an initial working app, not a replacement OS.

## Device verification — October 26, 2026

- Signed APK built and verified; installed successfully over ADB.
- Dashboard starts and renders with readable spacing and a visible focus outline. The OS renders at 1920×1080 even though the physical panel specification is 1366×768.
- Weather and CRM requests succeed from the TV; actual timestamps render on screen.
- A regular app can enumerate HDMI inputs and tune them using `TvView` and `TvContract.buildChannelUriForPassthroughInput`. No root or privileged permissions were granted.
- HDMI 3 reported `INPUT_VIDEO_AVAILABLE` repeatedly.
- HDMI 1 and HDMI 2 reported `INPUT_VIDEO_UNAVAILABLE`, reason 2 (no signal). Active video on those ports remains untested.
- Remote-driven cycling 3→1→2→3 and 3→2→3 worked. Back returned to the dashboard.
- Silk opened its `FireTvSlateActivity`.
- Prime Video opened its `livingroom.landing.LandingActivity` after correcting the package.
- Netflix missing-app path reached Amazon's Appstore activity.
- Apple TV's missing-app dialog and its Open Appstore action reached Amazon's Appstore details activity; nothing was purchased or installed.
- Screenshots and selected application logs are saved in `verification/`.

Amazon's own input-selection activity is permission-protected on this build. The app uses Android's standard TV-input view instead. No Amazon launcher, system package, update setting, or screen-sleep setting was disabled or globally changed.

## Build and install

The build uses Java 8 bytecode, Android API 25, Google's build-tools 35.0.0, and the Mac's installed Azul JDK 21. There are no Gradle, Maven, JavaScript, or third-party Android dependencies.

`build.py` defaults to `/private/tmp/firetv-tools/sdk`, where Google's archives were extracted:

- `https://dl.google.com/android/repository/platform-25_r03.zip` → `android-7.1.1/`
- `https://dl.google.com/android/repository/build-tools_r35_macosx.zip` → `android-15/` (Google's archive directory name; not the app's target API)

Override that base directory with `FIRETV_SDK` if moved. The JDK path is defined in `build.py`.

Run `python3 build.py` from this directory. Output: `build/command-center.apk`. Retain `build/development.keystore` to sign future updates with the same development identity; it is intentionally excluded from Git. This is a local development signing key, not a production credential.

Install with Android platform-tools:

```sh
adb connect 192.168.1.192:5555
adb -s 192.168.1.192:5555 install -r build/command-center.apk
adb -s 192.168.1.192:5555 shell am start -n local.commandcenter.tv/.MainActivity
```

The TV's IP address may change. The initial debugging connection must be approved on the TV.

## Source guide

- `src/local/commandcenter/tv/MainActivity.java`: dashboard, feeds, remote buttons, cache, display options.
- `src/local/commandcenter/tv/InputActivity.java`: native HDMI view, input cycling, signal status, audio focus.
- `src/local/commandcenter/tv/DashboardGlyph.java`: lightweight native outline icons.
- `src/local/commandcenter/tv/TrendChart.java` and `TrendSeries.java`: native charts and timestamped series with explicit data gaps.
- `res/drawable/`: launcher icon and banner.
- `AndroidManifest.xml`: activities and the single requested permission, Internet access.

UI decisions follow the local `make-interfaces-feel-better` skill: native Android views, large typography, tabular price/clock numerals, consistent rounded surfaces, and a static high-contrast remote focus indicator. Repeated focus animations were omitted to keep navigation calm and light on this older TV; an embedded browser was also unnecessary. Visual verification covers the rendered dashboard, focused controls, and input transitions. Long-running performance, audio, and all offline UI states remain unverified.
