# Version 0.6 daily charts

Full review scope: CRM minute-sample chart, hourly weather-model chart, shared layout, source labels, and chart rendering. Native Android Views/Canvas and the existing styling remain the only rendering system.

| Category | Evidence inspected | Result |
| --- | --- | --- |
| Typography | `charts-final-v6.png` | Primary values, auto-scaled bounds, time labels and source timestamps fit; five-day forecast retained |
| Surfaces | Actual TV screenshot | Charts remain unboxed; light grids add structure without extra cards |
| Animations | Source review | Charts redraw on data updates/minute-marker changes without decorative drawing animations; prior value fades unchanged |
| Icons | Final screenshot | Existing sun and forecast icons retained |
| Performance | Native implementation, 391/25 sample device logs, series tests | No added runtime dependency or request stream; no full-day profiling |

| Severity | Location | Before | After | Why |
| --- | --- | --- | --- | --- |
| MEDIUM | `MainActivity.java:231`, `:330`; `TrendChart.java:9` | CRM current value without history | One-minute closing samples on a timestamp-proportional regular-session axis | Shows the day's progression, including history before the dashboard opened |
| MEDIUM | `MainActivity.java:313`, `:80`; `TrendChart.java:9` | Current weather alone | Hourly modeled temperatures with Now marker and explicit future styling | Separates earlier and future modeled values without implying sensor measurements |
| MEDIUM | `TrendSeries.java:4` | No chart gap handling | Reject unordered times; exclude invalid/out-of-domain values; break lines over missing samples/intervals | Prevents fabricated price continuity or zero-valued missing data |
| LOW | `MainActivity.java:49`, `:80`, `:89` | Value columns sized for text only | Wider chart columns, smaller value rows and compact forecast spacing | Preserves hierarchy and fits all requested content on one screen |
| MEDIUM | `TrendChart.java:21` | First hardware-rendered dashed Paint line appeared solid | Explicit short line segments for future weather | Makes the temporal distinction visible on this older TV |

Considered and rejected: plotting only values collected since app launch (loses morning history); smooth cubic curves (can imply extrema not present in source samples); drawing every minute as a large dot (would obscure this compact chart); looping chart entrance animation (unnecessary on a persistent display).

Verification: signed build and `adb install -r` passed. Source logs showed 391 stock and 25 weather samples. `TrendSeriesTest` passed filtering, gap, ordering, empty and flat-data checks. Actual-TV initial and final screenshots inspected; final forecast dashes visible. The second installation restored cached chart history and fetched new responses. No new animation was introduced, so new slowed-motion verification was not performed.

**Approve** for inspected states. Not independently verified: full market-day growth, market holidays/half-day rollover, DST/weather day rollover, and on-TV visual empty/gap/error states. Data gaps and out-of-domain samples were verified in pure Java tests. Quotes may be delayed and weather remains model data.

---

# Version 0.5 forecast and daytime context

Full review of the added forecast strip, daylight readout, market-session context, and their layout with existing controls. Framework: native Android Views and Canvas glyphs; existing charcoal/ivory styling retained.

| Category | Evidence inspected | Result |
| --- | --- | --- |
| Typography | `forecast-final-v5.png`, `forecast-return-v5.png` | Five days fit without clipping; high/low hierarchy and source timestamps remain readable |
| Surfaces | Main dashboard and `forecast-controls-v5.png` | Single divider organizes the outlook; existing modal controls remain accessible |
| Animations | Source review; existing transitions retained | No new motion or loops; no new 10%-speed playback needed for this static addition |
| Icons | Actual fog/cloud/sun glyphs; source for snow/storm variants | Weather types differentiated with native outlines; snow/storm appearance not captured on TV |
| Performance | Source review and successful device loads | Forecast reuses one weather request; no additional polling or dependencies |

| Severity | Location | Before | After | Why |
| --- | --- | --- | --- | --- |
| MEDIUM | `MainActivity.java:90`, `:279`, `:296` | Only current weather/day high-low | Today plus four days with conditions, temperatures, and precipitation probability | Supports planning without opening another screen; validate complete data before rendering |
| LOW | `MainActivity.java:69`, `:263` | Empty space below date | Sunrise/sunset and remaining daylight using forecast times | Useful daytime context with no decorative movement |
| LOW | `MainActivity.java:251` | Regular-session label alone | Provider-based close countdown, upcoming opening, or session end time | Adds context while preserving quote freshness warnings |
| LOW | `DashboardGlyph.java:40`; `MainActivity.java:339` | Generic cloud/rain treatment; overcast labeled partly cloudy | Distinct fog/snow/storm glyphs and specific cloud labels | Improves correspondence between forecast text and icons |

Considered and rejected: news ticker (adds motion and attention cost); calendar/email panels (would need a separate account/data-display decision); a second large weather card (would crowd the primary clock and CRM view).

Verification: `python3 firetv-command-center/build.py` and APK signing verification passed; `adb install -r` succeeded. Actual-TV screenshots inspected after live five-day weather retrieval. Menu opened hidden apps/inputs; Back restored the dashboard. Device showed sunset/daylight remaining and a completed regular market session. Dashboard was left visible.

**Approve** for inspected states. Not independently verified: before-sunrise/after-sunset transitions, active-session countdown, overnight or DST rollover, invalid forecast/error UI, and snow/storm icon screenshots. Existing forecast validation and source timestamps remain in place; no full-day guarantee is claimed.

---

# Version 0.4 freshness addendum

Full review scope: data-age labels, regular-session status, independent refresh scheduling, and their effect on the existing native Android UI. Prior version 0.3 review follows below.

| Category | Evidence | Result |
| --- | --- | --- |
| Typography | `verification/freshness-v4.png` | Three-line metadata fits; source timestamps and check ages are visually distinct |
| Surfaces | Actual TV dashboard screenshot | Existing restrained hierarchy preserved |
| Animations | Source inspection | Existing value-fade behavior retained; status ages update without animation |
| Icons | Screenshot | Existing weather glyph unchanged |
| Performance | FeedState tests and scheduled request logs | Independent bounded workers; no overlapping fetches per feed; five-second lightweight status callback |

| Severity | Location | Before | After | Why |
| --- | --- | --- | --- | --- |
| MEDIUM | `MainActivity.java:25`, `:181`; `FeedState.java` | Shared sequential five-minute polling | Independent 30-second stock and 60-second weather cadence, monotonic scheduling and bounded retry backoff | Removes avoidable delay and prevents one feed blocking the other |
| MEDIUM | `MainActivity.java:217` | Source timestamp alone | Source time plus last-successful-check age, explicit model/regular-session labels, stale/failure warnings | Retrieval time cannot be mistaken for data freshness |
| MEDIUM | `MainActivity.java:239`, `:250` | Partial rendering and cache write before validation | Validate all needed values and source time before updating display/cache; reject regressions | Preserves last valid content when responses are malformed or older |
| LOW | `MainActivity.java:233`, `:289` | Default HTTP cache behavior and outdated About text | Disable local caching and explain updated cadence/source limits | Keeps behavior and information consistent |

Considered and rejected: second-by-second quote polling (cannot remove provider delay and adds request pressure); labeling every successful request “Live” (does not establish source freshness); clearing the values on every request (unnecessary flicker).

Verification: signed build and install passed. FeedState behavior tests passed. Actual-TV screenshot inspected with successful quote/weather check logs and automatic time enabled. No synthetic values were displayed. Failure/backoff behavior tested as pure Java state transitions; network disconnection and market-session boundaries were not forced on the user's TV.

Verdict: **Approve** for inspected states. Not verified: sustained provider availability, end-to-end latency guarantees, visual stale/error states on the TV, and extended-hours pricing (not implemented). These limitations remain explicit.

---

# Interface verification — version 0.3

Full review of the ambient dashboard, hidden controls, remote focus, panel transitions, and reduced-motion preference. Native Android API 25 Views, Canvas icons, Java drawables, and XML theme resources remain the styling system. No new runtime dependency. HDMI playback and external streaming app screens are outside this revision's visual review.

## Coverage

| Category | Evidence inspected | Result |
| --- | --- | --- |
| Typography | `ambient-final-v3.png`, `controls-step-v3.png`, `options-v3.png` | Large stable numerals; readable labels; no clipping in captured states |
| Surfaces | Dashboard, bottom controls panel, Options | Calm information view; clear modal hierarchy and ivory remote focus |
| Animations | Native animation code; `motion-final-v3.mp4` sampled frames including the opening sequence | Short fade/translation, no loops or focus scaling; opening and closing return to stable states |
| Icons | Existing Canvas glyphs in hidden controls | Monochrome style retained; focus recolors the same geometry |
| Performance | Animation properties, timers, source review, device interactions | Alpha/translation only; no new polling; no full-day profiling |

## Resolved findings

| Severity | Location | Before | After | Why |
| --- | --- | --- | --- | --- |
| MEDIUM | `src/local/commandcenter/tv/MainActivity.java:75`, `:96` | Apps and inputs always occupied the lower dashboard | Information fills the view; controls appear on Menu or Select, with a quiet discovery hint | Progressive disclosure keeps daily information primary |
| LOW | `src/local/commandcenter/tv/MainActivity.java:126`, `:146`, `:220` | Panels appeared and disappeared immediately | 220ms, 12dp entrance and 140ms, 6dp exit, with opacity and deceleration | Restrained, context-preserving transitions; existing animation canceled before closing |
| LOW | `src/local/commandcenter/tv/MainActivity.java:120`, `:198`, `:211` | Changing numbers switched abruptly | Changed values fade from 72% opacity over 200ms, with immediate truthful values | Gentle feedback without fake intermediate prices, blanking, or repeated unchanged-value motion |
| MEDIUM | `src/local/commandcenter/tv/MainActivity.java:116`, `:234` | No app motion preference | Saved Reduce motion control; animator scale zero also disables custom motion | Accessible alternative with static focus cues preserved |
| MEDIUM | `src/local/commandcenter/tv/MainActivity.java:244` | First implementation opened on key-down and immediately closed on release | Open on key-up, consume key-down, ignore canceled keys | One deliberate press gives one predictable result |
| MEDIUM | `src/local/commandcenter/tv/MainActivity.java:151` | First animated dismissal could suppress the successor Options dialog | Clear active panel synchronously before launching its next action | Prevents async dismissal from blocking the next panel; repeated activation is guarded while closing |

## Considered and rejected

- Scrolling ticker, ambient moving background, and looping weather icons: distracting during all-day use and unnecessary ongoing rendering.
- Animated focus enlargement: immediate ivory focus is clearer during repeated remote navigation.
- Rolling quote digits: intermediate numbers could imply prices that were never supplied by the data source.
- Automatically hiding controls while the user navigates: would interrupt a deliberate action.

## Verification

- `python3 firetv-command-center/build.py` succeeded; v2/v3 APK signature checks passed. Installed with `adb install -r` on the user's TV.
- Captured actual TV default view, Menu-open controls, Down moving from Netflix to HDMI 1, horizontal navigation to Options, and final Options screen.
- Enabled Reduce motion, restarted with `am start -S -W`, opened controls with Select, and returned to Options. `reduced-motion-persisted-v3.png` shows “Enable subtle motion,” confirming persistence. Restored subtle motion afterward.
- Recorded Menu → controls → Back → Today and Select → controls → Menu → Today. Inspected sampled recording frames and a finer opening sequence. Exported `motion-10percent-v3.mp4` for slowed playback; literal real-time 10%-speed playback was not viewed through the tool. The visual review used frame sequences instead.
- `ambient-final-v3.png` confirms the dashboard was left visible, with live weather/CRM timestamps.
- An earlier combined automated sequence lost the expected UI state and reached Prime Video. Its captures (`motion-v3.mp4`, `reduced-motion-v3.png`, `ambient-return-v3.png`, and early check images) are not passing evidence. Paced checks and the final recording supersede that run.

**Approve** for the inspected revision; no actionable findings remain in those states. Not verified: live value-fade recording, system-wide animator-scale-zero behavior (code inspected only), arbitrary mid-frame remote interruption, all loading/offline states, every clock/date width, full-day performance, HDMI/audio and every streaming shortcut in this revision. Prior version verified HDMI and app launching; this update retains those implementations behind the controls panel.
