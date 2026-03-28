# Media Control Feature Overview

## Goal
Enable AnotherGlass to control media currently playing on the Android phone (YouTube, Spotify, etc.) while `GlassService` is running, and surface media playback state on:
- Mobile app UI
- Glass Enterprise Edition timeline card
- Glass Explorer Edition live card

## Scope (Requested)
1. Add a mobile-side extension (similar to `GPSExtension` / `NotificationExtension`) that listens to active OS media sessions and attaches to the currently playing one.
2. Show playback state and basic controls in mobile app UI.
3. Introduce a shared media communication protocol in `shared/src/main/java/com/damn/anotherglass/shared`.
4. Add a shared receiver/controller in `glass-shared` for both Explorer and Enterprise apps.
5. Add an Enterprise card.
6. Add an Explorer live card.

## Non-goals (for this iteration)
- In-app media browsing/queue management.
- Album art transfer over RPC (can be added later as optional optimization).
- Per-app preference rules (forward from all apps vs allow-list).

## High-Level Architecture

### Mobile side
- `GlassService` owns a new `MediaExtension` lifecycle:
  - Start when service is connected and media forwarding is enabled.
  - Stop when disconnected or feature is disabled.
- `MediaExtension` uses Android media session APIs to:
  - Discover active sessions.
  - Track the "best" active controller (prefer `STATE_PLAYING`, then recent active).
  - Subscribe to playback/metadata updates.
  - Emit media state updates to:
    1) Local service state flow (for mobile UI)
    2) RPC outbound message to Glass host
- `GlassService.onDataReceived` handles inbound media commands from Glass (play/pause/next/prev/seek) and routes them to `MediaExtension`.

### Shared protocol layer (`shared`)
Add a `shared.media` package with serializable DTOs and API constants.
- Suggested names:
  - `MediaAPI` (service name/id)
  - `MediaStateData` (title, artist, app name, duration, position, play state, actions bitmask)
  - `MediaCommandData` (command enum + optional value for seek)

### Glass shared layer (`glass-shared`)
- Add `MediaController` (similar role to `NotificationController`) as shared in-memory receiver/state holder.
- Responsibilities:
  - Accept media state updates from host service.
  - Expose observable state flow.
  - Optionally keep last known state across temporary disconnects.

### Glass EE app
- Host service routes `MediaAPI` state messages into `MediaController`.
- Main timeline registers a `MediaCard` module, similar to notifications module:
  - Card appears when media state is available.
  - Displays now playing + play state.
  - Tap/voice actions send media command back to phone via RPC.

### Glass XE app
- Host service routes `MediaAPI` state into the same shared `MediaController`.
- Add a live-card based `MediaCardController`/provider:
  - Renders current media state in `RemoteViews`.
  - Card action triggers menu/activity for controls or direct toggle (implementation choice).
  - Sends commands back using existing RPC client.

## Proposed Data Flow

### A) Phone -> Glass (state updates)
1. Android OS media session update.
2. `MediaExtension` receives callback and computes normalized `MediaStateData`.
3. `GlassService.send(RPCMessage(MediaAPI.ID, MediaStateData))`.
4. Host service (`glass-ee`/`glass-xe`) routes to shared `MediaController`.
5. UI cards observe and re-render.

### B) Glass -> Phone (control commands)
1. User triggers card action (tap/menu/voice).
2. Host sends `RPCMessage(MediaAPI.ID, MediaCommandData)`.
3. Mobile `GlassService` routes inbound media command to `MediaExtension`.
4. `MediaExtension` invokes transport controls on active `MediaControllerCompat`.

## Android Platform Notes
- Primary integration: `MediaSessionManager` + active session callbacks + controller callbacks.
- Session discovery typically requires notification listener permission (already relevant in app due to notifications feature).
- Commands should no-op safely when no active session is available.
- Session churn is common (ads, short clips, app handoffs); extension should debounce/reselect target controller.

## UI/UX Summary
- Mobile app: show now-playing section under service options:
  - App + title + artist + playback state + position
  - Controls: previous, play/pause, next
- Glass EE: timeline card with compact metadata and interaction.
- Glass XE: live card view with concise text-first rendering.

## Error Handling and Fallbacks
- No active media: show "No media playing" state.
- Unsupported command by source app: disable/hide unavailable actions based on actions bitmask.
- Lost session token: re-scan active sessions and rebind.
- RPC disconnect: keep local state but pause outbound sync until connection resumes.

## Security and Privacy
- Do not transmit raw notification text bodies beyond media metadata required for playback UI.
- Avoid persisting sensitive track history unless explicitly enabled later.
- Keep feature off if required Android permission is missing; expose clear UI hint.

## Performance
- State updates should be throttled for position ticks (e.g., 1 Hz max while playing).
- Send immediate updates on play/pause/track change.
- Avoid heavy payloads in RPC messages (text + primitive fields only).

## Acceptance Criteria (Feature-level)
- While `GlassService` is running, active media from major apps can be seen and controlled from AnotherGlass.
- Mobile UI reflects current media state in near real-time.
- Both Glass editions display media status using shared state receiver code in `glass-shared`.
- Play/pause/next/previous command roundtrip works phone <-> Glass.
- Feature degrades gracefully when no session/permission is available.

