# Media Control Implementation Plan

## Delivery Strategy
Implement in vertical slices that keep both mobile and Glass apps buildable at each step.

## Phase 0 - Preparation
- [x] Confirmed `MediaSessionManager` API available at `minSdkVersion 23`.
- [x] Confirmed notification-listener permission flow reused via `NotificationService` component name for session discovery.
- [ ] Add placeholder strings/resources for media UI states in `glass-ee` and `glass-xe`.

## Phase 1 - Mobile Media Extension ✅
_Completed 2026-03-28_

### 1.1 Extension class
- [x] Created `mobile/.../extensions/media/MediaExtension.kt`
  - Discovers active sessions via `MediaSessionManager`.
  - Selects best controller (prefer `STATE_PLAYING`, then `BUFFERING`, then first).
  - Registers `MediaController.Callback` for playback-state + metadata updates.
  - Re-selects controller on `OnActiveSessionsChangedListener` and `onSessionDestroyed`.
  - Throttles position-only updates to 1 s via fingerprint comparison.
  - Emits `MediaStateData` via service callback + outbound `RPCMessage(MediaAPI.ID, ...)`.
  - Executes inbound `MediaCommandData` via `TransportControls`.

### 1.2 Integration into `GlassService`
- [x] `mMedia: MediaExtension` member added.
- [x] Started on `onConnectionStarted`, stopped on `onWaiting` / `onConnectionLost` / `onShutdown` / `onDestroy`.
- [x] `mediaState: StateFlow<MediaStateData?>` exposed (null = extension inactive).
- [x] `sendMediaCommand(command)` public API added for direct local dispatch (bypasses RPC).
- [x] Inbound `MediaAPI.ID` + `MediaCommandData` routed to `mMedia.onCommand(...)`.

### 1.3 Settings toggle
- [ ] `Settings.MEDIA_ENABLED` not yet added (deferred — extension always on when connected).

## Phase 2 - Mobile UI ✅
_Completed 2026-03-28_

### 2.1 Service/controller exposure
- [x] `IServiceController` extended with `mediaState: StateFlow<MediaStateData?>` and `sendMediaCommand(command)`.
- [x] `ServiceController` collects `service.mediaState`; resets to `null` on disconnect.

### 2.2 Compose UI
- [x] `MediaStatusCard.kt` created as standalone file in `ui/mainscreen/`.
  - Hidden when `mediaState == null` (extension inactive).
  - Shows "No media playing" when `playbackState == None`.
  - Shows app / title / artist / position when media is active.
  - Buttons hidden when no media; shown with icon assets otherwise:
    - `ic_fast_rewind_24` → Previous
    - `ic_play_arrow_24` / `ic_pause_24` → TogglePlayPause
    - `ic_fast_forward_24` → Next
  - Buttons enabled/disabled via `actionsMask` bit-check.
  - Commands call `serviceController.sendMediaCommand(...)` directly (not RPC send).
- [x] `MainScreen.kt` updated to conditionally render `MediaStatusCard`.

## Phase 3 - Shared Protocol ✅
_Completed 2026-03-28_

### 3.1 Files added
- [x] `shared/.../shared/media/MediaAPI.kt` — service ID constant `"Media"`.
- [x] `shared/.../shared/media/MediaStateData.kt` — Kotlin data class + `Serializable`.
  - Fields: `playbackState`, `title`, `artist`, `album`, `sourceApp`, `sourcePackage`, `positionMs`, `durationMs`, `actionsMask`, `lastUpdatedMs`.
  - `PlaybackStateValue` enum: `None`, `Playing`, `Paused`, `Stopped`, `Buffering`.
- [x] `shared/.../shared/media/MediaCommandData.kt` — Kotlin data class + `Serializable`.
  - Fields: `command`, `seekToMs`.
  - `Command` enum: `Play`, `Pause`, `TogglePlayPause`, `Next`, `Previous`, `SeekTo`.
- [x] `shared/build.gradle` updated: added `kotlin-android` plugin + JVM target alignment (17).
- [x] Serialization: plain Gson / Java `Serializable` (no kotlinx.serialization dependency).

### 3.2 Routing rules implemented
- [x] Phone → Glass: `RPCMessage(MediaAPI.ID, MediaStateData)` sent on every state change.
- [x] Glass → Phone: `RPCMessage(MediaAPI.ID, MediaCommandData)` routed to `mMedia.onCommand(...)`.

## Phase 4 - Shared Receiver in `glass-shared` ✅
_Completed 2026-03-28_

### 4.1 Add shared media controller
- [x] Created `glass-shared/.../glass/shared/media/MediaController.kt`.
  - `getState(): StateFlow<MediaStateData?>`.
  - `onMediaStateUpdate(state: MediaStateData)`.
  - `onServiceConnected()` clears stale state.
  - Singleton `instance` pattern matching notifications.

### 4.2 Command forwarding abstraction
- [x] Added `IRPCSender` in `MediaController.kt` and wired controller-owned command forwarding.
- [x] Added `setService(...)`, `clearService()`, and `sendCommand(...)` APIs.
- [x] Host service now sets/clears sender lifecycle; UI sends commands only via `MediaController`.

## Phase 5 - Enterprise Edition Card ✅ (implementation)
_Implemented 2026-03-28; manual device validation still pending_

### 5.1 Host route
- [x] `glass-ee/.../core/HostService.kt` routes `MediaAPI.ID` (`MediaStateData`) into `MediaController`.
- [x] Calls `MediaController.instance.onServiceConnected()` on host connection start.
- [x] Registers host transport in `MediaController` on `onCreate`, clears it on `onDestroy`.
- [x] No direct `HostService` exposure to UI for media commands.

### 5.2 Timeline lifecycle
- [x] Added `addMediaModule(timeLine)` in `glass-ee/.../ui/MainActivityEx.kt`.
  - Card is added when media state becomes non-null.
  - Card is removed when media state returns to null.

### 5.3 Card implementation
- [x] Added `glass-ee/.../ui/cards/MediaCard.kt`.
  - Observes `MediaController.instance.getState()`.
  - Displays app/title/artist/state and gesture hint.
  - Control behavior:
    - single tap -> `TogglePlayPause`
    - double tap (timed second tap) -> `Next`
    - two-finger tap -> `Pause` (stop action)
  - Sends commands through `MediaController.instance.sendCommand(...)` only.
- [x] Added media card layout and strings:
  - `glass-ee/src/main/res/layout/layout_card_media.xml`
  - `glass-ee/src/main/res/values/strings.xml`

## Phase 6 - Explorer Edition Live Card 🔲

### 6.1 Host route
- [ ] `glass-xe/.../HostService.java`: route `MediaAPI.ID` `MediaStateData` into `MediaController.getInstance().onMediaStateUpdate(...)`.
- [ ] Call `MediaController.getInstance().onServiceConnected()` on connect.
- [ ] Register/clear `MediaController` sender lifecycle in XE host service.

### 6.2 Live card controller
- [ ] Create `glass-xe/.../media/MediaCardController.java` extending `ICardViewProvider`.
  - Observes `MediaController`.
  - Renders `RemoteViews` with title / artist / state text.
  - Publishes/unpublishes live card based on state presence.

### 6.3 Command interaction
- [ ] Implement controls through `MediaController.sendCommand(...)` from XE UI/menu actions.

## Phase 7 - Validation 🔲

### 7.1 Functional tests (manual)
- [ ] Start service on phone, connect Glass.
- [ ] Play media in Spotify/YouTube/other app on phone.
- [ ] Verify mobile UI updates within ~1 s.
- [ ] Verify EE card appears only while media state exists.
- [ ] Verify EE gestures:
  - single tap -> play/pause
  - double tap -> next
  - two-finger tap -> stop/pause
- [ ] Verify XE live card appears and controls work.

### 7.2 Regression checks
- [ ] GPS and notifications still function.
- [ ] Connection/disconnection and app restarts do not crash.
- [ ] No excessive RPC spam while playing (position updates throttled).

## Concrete File Touch List

### Completed ✅
- `shared/build.gradle`
- `shared/.../shared/media/MediaAPI.kt` (new)
- `shared/.../shared/media/MediaStateData.kt` (new)
- `shared/.../shared/media/MediaCommandData.kt` (new)
- `mobile/.../extensions/media/MediaExtension.kt` (new)
- `mobile/.../core/GlassService.kt`
- `mobile/.../ui/mainscreen/IServiceController.kt`
- `mobile/.../ui/mainscreen/ServiceController.kt`
- `mobile/.../ui/mainscreen/MediaStatusCard.kt` (new)
- `mobile/.../ui/mainscreen/MainScreen.kt`
- `glass-shared/.../glass/shared/media/MediaController.kt` (new)
- `glass-ee/.../core/HostService.kt`
- `glass-ee/.../ui/MainActivity.kt`
- `glass-ee/.../ui/MainActivityEx.kt`
- `glass-ee/.../ui/cards/GestureListener.java`
- `glass-ee/.../ui/cards/MediaCard.kt` (new)
- `glass-ee/src/main/res/layout/layout_card_media.xml` (new)
- `glass-ee/src/main/res/values/strings.xml`

### Remaining 🔲
- `glass-xe/.../HostService.java`
- `glass-xe/.../media/MediaCardController.java` (new)

## Risks and Mitigations
- Risk: Media app differences in available actions.
  - Mitigation: respect `actionsMask`; hide/disable unsupported controls.
- Risk: Session handoff causes stale controller.
  - Mitigation: reselect controller on each `OnActiveSessionsChangedListener` callback and `onSessionDestroyed`.
- Risk: High-frequency position updates.
  - Mitigation: 1 s throttle via fingerprint; immediate emit on semantic changes (play/pause/track).
- Risk: `MediaSessionManager` requires notification-listener permission to list sessions.
  - Mitigation: reuse existing `NotificationService` component name; log warning if not enabled.
- Risk: Double-tap emulation timing may need tuning on device.
  - Mitigation: keep timeout constant localized in EE card and tune after manual run.

## Definition of Done
- [x] Mobile extension + direct UI control working.
- [x] Shared protocol (Gson-compatible) established.
- [ ] Shared glass media receiver in `glass-shared` used by both editions.
- [x] EE card implemented with MediaController-only command flow.
- [ ] XE live card renders current state and can send core commands.
- [ ] Basic manual validation completed on at least one streaming app and one EE device.
