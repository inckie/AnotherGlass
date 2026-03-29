# Media Control Feature Overview

## Status
**Complete** — all phases implemented and integrated. _(2026-03-28)_

## Goal
Enable AnotherGlass to mirror the currently playing media session from the Android phone to Glass and allow playback control from Glass back to the phone, while `GlassService` is running.

## Scope
1. Mobile-side `MediaExtension` that listens to OS media sessions and forwards state + routes commands.
2. Shared media protocol DTOs and API constants in `shared`.
3. Shared `MediaController` in `glass-shared` used by both Glass apps.
4. Mobile app UI showing now-playing state and local controls.
5. Glass Enterprise Edition (EE) timeline media card with gesture controls.
6. Glass Explorer Edition (XE) media live card + `MediaPlaybackActivity` control screen.
7. Python debug client bidirectional support (receives and handles media commands from Glass).

## Non-goals (this iteration)
- Album art transfer over RPC.
- In-app media browsing / queue management.
- Per-app allow-list filtering.
- Settings toggle to enable/disable the feature independently.

---

## Architecture

### Data Flow

#### Phone → Glass (state updates)
1. Android OS media session fires a callback.
2. `MediaExtension` normalises it into `MediaStateData` and emits via `GlassService`.
3. `RPCMessage(MediaAPI.ID, MediaStateData)` is sent over the transport.
4. Host service on Glass (`HostService`) routes it to `MediaController.instance`.
5. UI observes `MediaController.getState()` and re-renders.

#### Glass → Phone (control commands)
1. User triggers a card action (tap / swipe).
2. UI calls `MediaController.instance.sendCommand(MediaCommandData)`.
3. `MediaController` forwards via `IRPCSender` set by the host service.
4. `RPCMessage(MediaAPI.ID, MediaCommandData)` is sent over the transport.
5. `GlassService` on the phone routes it to `MediaExtension.onCommand(...)`.
6. `MediaExtension` calls `TransportControls` on the active media session.

---

## Key Files

### Shared protocol (`shared`)
| File | Purpose |
|------|---------|
| `shared/media/MediaAPI.kt` | Service ID constant: `"Media"` |
| `shared/media/MediaStateData.kt` | Playback state DTO: title, artist, album, sourceApp, sourcePackage, positionMs, durationMs, actionsMask, lastUpdatedMs, playbackState |
| `shared/media/MediaCommandData.kt` | Command DTO: `command` enum + `seekToMs`. Commands: `Play`, `Pause`, `TogglePlayPause`, `Next`, `Previous`, `SeekTo` |

### Mobile (`mobile`)
| File | Purpose |
|------|---------|
| `extensions/media/MediaExtension.kt` | Discovers active sessions via `MediaSessionManager`; selects best controller (prefers `STATE_PLAYING`); throttles position-only updates to 1 s; executes inbound `MediaCommandData` via `TransportControls` |
| `core/GlassService.kt` | Owns `MediaExtension` lifecycle; exposes `mediaState: StateFlow<MediaStateData?>`; routes inbound `MediaAPI` messages to extension |
| `ui/mainscreen/MediaStatusCard.kt` | Compose card: hidden when extension inactive; shows app/title/artist/position; play/pause/prev/next buttons gated by `actionsMask` |
| `ui/mainscreen/MainScreen.kt` | Conditionally renders `MediaStatusCard` |

### Shared Glass layer (`glass-shared`)
| File | Purpose |
|------|---------|
| `glass/shared/media/MediaController.kt` | Singleton state holder; `getState(): StateFlow<MediaStateData?>`; `setService`/`clearService`/`sendCommand` lifecycle; clears state on reconnect |

### Glass Enterprise Edition (`glass-ee`)
| File | Purpose |
|------|---------|
| `core/HostService.kt` | Routes `MediaAPI` messages to `MediaController`; registers/clears `IRPCSender` |
| `ui/MainActivityEx.kt` | Adds/removes `MediaCard` module on the timeline based on state presence |
| `ui/cards/MediaCard.kt` | Observes state; displays app/title/artist; gesture controls: single tap → `TogglePlayPause`, timed double tap → `Next`, two-finger tap → `Pause` |
| `res/layout/layout_card_media.xml` | Card layout |

### Glass Explorer Edition (`glass-xe`)
| File | Purpose |
|------|---------|
| `host/HostService.java` | Routes `MediaAPI` messages to `MediaController`; registers/clears `IRPCSender` |
| `host/media/MediaCardController.java` | Publishes/unpublishes media live card; renders `RemoteViews` using `CardBuilder.Layout.AUTHOR`; opens `MediaPlaybackActivity` on tap |
| `host/media/MediaPlaybackActivity.kt` | 3-card `CardScrollView`: Prev (`CAPTION`), Center (`AUTHOR`), Next (`CAPTION`). Center: single tap → `TogglePlayPause`, timed double tap → `Next`. All commands via `MediaController.sendCommand(...)` |

### Python debug client (`python`)
| File | Purpose |
|------|---------|
| `client.py` | `MediaDebugController` simulates a 5-track playlist with full state machine; UI mode handles inbound `MediaCommandData` messages from Glass (`TogglePlayPause`, `Play`, `Pause`, `Next`, `Previous`) and updates the UI; console mode provides a manual menu |

---

## Card Layout Decisions

### XE Live Card & Center playback card — `CardBuilder.Layout.AUTHOR`
Fields mapped:
- `setHeading(title)` — track title (or source app name if no title)
- `setSubheading(artist ?? sourceApp)` — artist, falling back to source app
- `setFootnote(sourceApp)` — source app name (live card) / interaction hint (playback center card)
- `setTimestamp(playbackState.name)` — e.g. "Playing", "Paused"

### XE Prev / Next cards — `CardBuilder.Layout.CAPTION`
Short action label + footnote hint; avoids text cropping seen with `LAYOUT_MENU`.

### EE Media Card — custom XML layout (`layout_card_media.xml`)
Full-bleed card with title, artist, state, and gesture hint footer.

---

## Platform Notes
- `MediaSessionManager` requires notification-listener permission to enumerate sessions — reuses the existing `NotificationService` component name.
- Session churn (ads, app handoffs) is handled by re-selecting the best controller on every `OnActiveSessionsChangedListener` callback and `onSessionDestroyed`.
- Position-tick updates are throttled to 1 Hz via a state fingerprint; play/pause/track-change events are sent immediately.
- `actionsMask` bits from the media session are forwarded as-is; UI disables controls for unsupported actions.
- Commands no-op safely when no active session exists.

## Expansion Ideas
- **Album art** — transfer as a Base64 or compressed byte array in `MediaStateData`; render as `setIcon` on `AUTHOR` cards.
- **Seek bar** — add a position/duration progress view; use `MediaCommandData.Command.SeekTo` + `seekToMs`.
- **Per-app filter** — settings toggle or allow-list stored in `SharedPreferences`; `MediaExtension` skips disallowed packages.
- **Settings toggle** — add `Settings.MEDIA_ENABLED` flag; start/stop `MediaExtension` dynamically.
- **Voice commands** — EE voice grammar entry to trigger play/pause/next without tapping the card.
- **EE queue card** — swipe-right from the media card to show upcoming tracks if the session exposes a queue.
