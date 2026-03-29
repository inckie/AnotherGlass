import json
import random
import socket
import threading
import time
from enum import StrEnum
from typing import TypedDict, Optional, Union

import netifaces
import qrcode

'''
IMPORTANT:
Make sure to change communication protocol to JSON Lines by setting SerializerProvider.currentSerializer to JSON
'''

# Types (almost) matching definitions in com.damn.anotherglass.shared package

class Location(TypedDict):
    latitude: float
    longitude: float
    altitude: float
    speed: float
    bearing: float
    accuracy: float

class NotificationAction(StrEnum):
    Posted = 'Posted'
    Removed = 'Removed'

class NotificationDeliveryMode(StrEnum):
    Silent = 'Silent'
    Sound = 'Sound'

class NotificationData(TypedDict):
    action: NotificationAction
    id: int
    packageName: str
    postedTime: int
    isOngoing: bool
    title: str
    text: str
    tickerText: str
    icon: Optional[bytes]
    deliveryMode: NotificationDeliveryMode


class BatteryStatusData(TypedDict):
    level: int
    isCharging: bool


class PlaybackStateValue(StrEnum):
    NoneState = 'None'
    Playing = 'Playing'
    Paused = 'Paused'
    Stopped = 'Stopped'
    Buffering = 'Buffering'


class MediaStateData(TypedDict):
    playbackState: PlaybackStateValue
    title: Optional[str]
    artist: Optional[str]
    album: Optional[str]
    sourceApp: Optional[str]
    sourcePackage: Optional[str]
    positionMs: int
    durationMs: int
    actionsMask: int
    lastUpdatedMs: int


class Song(TypedDict):
    title: str
    artist: str
    album: str
    durationMs: int


class RPCMessage(TypedDict):
    service: Optional[str]
    type: Optional[str]
    payload: Optional[Union[Location, NotificationData, BatteryStatusData, MediaStateData, str]]


LOCATIONS: list[Location] = [
    {"latitude": 34.0522, "longitude": -118.2437, "altitude": 71.0, "speed": 0.0, "bearing": 0.0, "accuracy": 5.0},
    {"latitude": 40.7128, "longitude": -74.0060, "altitude": 10.0, "speed": 0.0, "bearing": 0.0, "accuracy": 5.0},
    {"latitude": 48.8566, "longitude": 2.3522, "altitude": 35.0, "speed": 0.0, "bearing": 0.0, "accuracy": 5.0},
]

BASE_NOTIFICATION: NotificationData = {
    "action": NotificationAction.Posted,
    "id": 0,
    "packageName": "com.damn.anotherglass.python",
    "postedTime": 0,
    "isOngoing": False,
    "title": "Test Notification",
    "text": "This is a test notification from the Python client.",
    "tickerText": "Test Notification",
    "icon": None,
    "deliveryMode": NotificationDeliveryMode.Sound
}

def get_local_ip() -> Optional[str]:
    """Gets the local IP address of the machine."""
    try:
        # Try to get the IP address of the default gateway interface
        gws = netifaces.gateways()
        default_gateway = gws['default'][netifaces.AF_INET][1]
        iface = netifaces.ifaddresses(default_gateway)
        return iface[netifaces.AF_INET][0]['addr']
    except Exception:
        # Fallback to getting all interfaces and finding a suitable IP
        for interface in netifaces.interfaces():
            iface = netifaces.ifaddresses(interface)
            if netifaces.AF_INET in iface:
                # Check for non-loopback addresses
                for link in iface[netifaces.AF_INET]:
                    if link['addr'] != '127.0.0.1':
                        return link['addr']
    return None

def create_rpc_message(
        service: str,
        payload: Union[Location, NotificationData, MediaStateData]
) -> RPCMessage:
    payload_type = {
        "MockGPS": "com.damn.anotherglass.shared.gps.Location",
        "Notifications": "com.damn.anotherglass.shared.notifications.NotificationData",
        "Media": "com.damn.anotherglass.shared.media.MediaStateData"
    }
    return {
        "service": service,
        "type": payload_type.get(service),
        "payload": payload
    }


class MediaDebugController:
    ACTION_PAUSE = 2
    ACTION_PLAY = 4
    ACTION_SKIP_TO_PREVIOUS = 16
    ACTION_SKIP_TO_NEXT = 32
    ACTION_PLAY_PAUSE = 512

    PLAYLIST: list[Song] = [
        {"title": "Morning Drive", "artist": "The Satellites", "album": "City Lights", "durationMs": 196000},
        {"title": "Blue Horizon", "artist": "Neon Waves", "album": "Skyline", "durationMs": 213000},
        {"title": "Night Walk", "artist": "Aster", "album": "Urban Stories", "durationMs": 187000},
        {"title": "Northern Wind", "artist": "Polaris", "album": "Latitude", "durationMs": 241000},
        {"title": "Slow Rain", "artist": "Low Tide", "album": "After Hours", "durationMs": 205000},
    ]

    def __init__(self) -> None:
        self.media_running = False
        self.playback_state = PlaybackStateValue.NoneState
        self.current_track_index: Optional[int] = None

    @property
    def current_track(self) -> Optional[Song]:
        if self.current_track_index is None:
            return None
        return self.PLAYLIST[self.current_track_index]

    @property
    def available_buttons(self) -> dict[str, bool]:
        return {
            "start_app": not self.media_running,
            "stop_app": self.media_running,
            "play": self.media_running and self.playback_state != PlaybackStateValue.Playing,
            "pause": self.media_running and self.playback_state == PlaybackStateValue.Playing,
            "prev": self.media_running and self.current_track is not None,
            "next": self.media_running and self.current_track is not None,
        }

    def start_app(self) -> None:
        if self.media_running:
            return
        self.media_running = True
        self.current_track_index = 0
        self.playback_state = PlaybackStateValue.Stopped

    def stop_app(self) -> None:
        if not self.media_running:
            return
        self.media_running = False
        self.current_track_index = None
        self.playback_state = PlaybackStateValue.NoneState

    def play(self) -> None:
        if not self.media_running:
            return
        if self.current_track_index is None:
            self.current_track_index = 0
        self.playback_state = PlaybackStateValue.Playing

    def pause(self) -> None:
        if not self.media_running:
            return
        self.playback_state = PlaybackStateValue.Paused

    def next_track(self) -> None:
        if not self.media_running:
            return
        if self.current_track_index is None:
            self.current_track_index = 0
            return
        self.current_track_index = (self.current_track_index + 1) % len(self.PLAYLIST)

    def prev_track(self) -> None:
        if not self.media_running:
            return
        if self.current_track_index is None:
            self.current_track_index = 0
            return
        self.current_track_index = (self.current_track_index - 1) % len(self.PLAYLIST)

    def _actions_mask(self) -> int:
        if not self.media_running:
            return 0
        mask = self.ACTION_PLAY_PAUSE
        if self.playback_state == PlaybackStateValue.Playing:
            mask |= self.ACTION_PAUSE
        else:
            mask |= self.ACTION_PLAY
        if self.current_track is not None:
            mask |= self.ACTION_SKIP_TO_NEXT | self.ACTION_SKIP_TO_PREVIOUS
        return mask

    def to_media_state(self) -> MediaStateData:
        track = self.current_track
        return {
            "playbackState": self.playback_state,
            "title": track["title"] if track else None,
            "artist": track["artist"] if track else None,
            "album": track["album"] if track else None,
            "sourceApp": "Python Debug Player" if self.media_running else None,
            "sourcePackage": "com.damn.anotherglass.python.media" if self.media_running else None,
            "positionMs": 0,
            "durationMs": track["durationMs"] if track else 0,
            "actionsMask": self._actions_mask(),
            "lastUpdatedMs": int(time.time() * 1000),
        }


class Core:
    def __init__(self):
        self.notification_id = 0
        self.conn: Optional[socket.socket] = None
        self.media = MediaDebugController()

    def send_message(self, message: RPCMessage) -> bool:
        if not self.conn:
            print("Not connected.")
            return False
        try:
            self.conn.sendall(json.dumps(message).encode('utf-8'))
            self.conn.sendall(b'\n')
            print(f"Sent message: {message.get('service')}")
            return True
        except socket.error as e:
            print(f"Error sending message: {e}")
            return False

    def send_gps(self):
        location = random.choice(LOCATIONS)
        message = create_rpc_message("MockGPS", location)
        self.send_message(message)

    def add_notification(self):
        self.notification_id += 1
        notification: NotificationData = BASE_NOTIFICATION.copy()
        notification['id'] = self.notification_id
        notification['action'] = NotificationAction.Posted
        notification['postedTime'] = int(time.time() * 1000)
        notification['title'] = notification['title'] + ' ' + str(self.notification_id)
        message = create_rpc_message("Notifications", notification)
        self.send_message(message)

    def remove_notification(self):
        if self.notification_id > 0:
            notification: NotificationData = BASE_NOTIFICATION.copy()
            notification['id'] = self.notification_id
            notification['action'] = NotificationAction.Removed
            message = create_rpc_message("Notifications", notification)
            if self.send_message(message):
                self.notification_id -= 1
        else:
            print("No notifications to remove.")

    def send_media_state(self):
        message = create_rpc_message("Media", self.media.to_media_state())
        self.send_message(message)

    def media_start_app(self):
        self.media.start_app()
        self.send_media_state()

    def media_stop_app(self):
        self.media.stop_app()
        self.send_media_state()

    def media_play(self):
        self.media.play()
        self.send_media_state()

    def media_pause(self):
        self.media.pause()
        self.send_media_state()

    def media_prev(self):
        self.media.prev_track()
        self.send_media_state()

    def media_next(self):
        self.media.next_track()
        self.send_media_state()

    def exit(self):
        if self.conn:
            message: RPCMessage = {"service": None, "type": None, "payload": None}
            self.send_message(message)


def main_ui():

    import tkinter as tk
    from tkinter import ttk
    from PIL import ImageTk

    class App:
        def __init__(self, root):
            super().__init__()
            self.qr_image = None
            self.root = root
            self.root.title("AnotherGlass Python Client")
            self.root.resizable(False, False)
            self.status_var = tk.StringVar()
            self.device_name_var = tk.StringVar(value="Name: N/A")
            self.battery_status_var = tk.StringVar(value="Battery: N/A")
            self.media_track_var = tk.StringVar(value="Track: N/A")
            self.media_state_var = tk.StringVar(value="State: None")

            self.ip_address = get_local_ip()
            if not self.ip_address:
                self.root.destroy()
                return

            self.core = Core()

            self.setup_ui()
            self.start_server()

        def setup_ui(self):
            frame = ttk.Frame(self.root, padding="10")
            frame.grid(row=0, column=0, sticky=(tk.W, tk.E, tk.N, tk.S))

            # QR Code
            qr = qrcode.QRCode(
                version=1,
                error_correction=qrcode.constants.ERROR_CORRECT_L,
                box_size=10,
                border=4,
            )
            qr.add_data(self.ip_address)
            qr.make(fit=True)
            img = qr.make_image(fill_color="black", back_color="white")
            self.qr_image = ImageTk.PhotoImage(img)

            qr_label = ttk.Label(frame, image=self.qr_image)
            qr_label.grid(row=0, column=0, columnspan=2, pady=10)

            ip_label = ttk.Label(frame, text=f"IP Address: {self.ip_address}")
            ip_label.grid(row=1, column=0, columnspan=2)

            # Device Info
            device_frame = ttk.LabelFrame(frame, text="Device", padding="10")
            device_frame.grid(row=2, column=0, columnspan=2, sticky=(tk.W, tk.E), pady=10)

            device_name_label = ttk.Label(device_frame, textvariable=self.device_name_var)
            device_name_label.grid(row=0, column=0, sticky=tk.W)

            battery_status_label = ttk.Label(device_frame, textvariable=self.battery_status_var)
            battery_status_label.grid(row=1, column=0, sticky=tk.W)

            media_frame = ttk.LabelFrame(frame, text="Media", padding="10")
            media_frame.grid(row=3, column=0, columnspan=2, sticky=(tk.W, tk.E), pady=10)

            media_track_label = ttk.Label(media_frame, textvariable=self.media_track_var)
            media_track_label.grid(row=0, column=0, columnspan=3, sticky=tk.W)

            media_state_label = ttk.Label(media_frame, textvariable=self.media_state_var)
            media_state_label.grid(row=1, column=0, columnspan=3, sticky=tk.W)

            self.media_buttons = {
                "start_app": ttk.Button(media_frame, text="Start App", command=self.on_media_start_app),
                "stop_app": ttk.Button(media_frame, text="Stop App", command=self.on_media_stop_app),
                "play": ttk.Button(media_frame, text="Play", command=self.on_media_play),
                "pause": ttk.Button(media_frame, text="Pause", command=self.on_media_pause),
                "prev": ttk.Button(media_frame, text="Prev", command=self.on_media_prev),
                "next": ttk.Button(media_frame, text="Next", command=self.on_media_next),
            }

            self.media_buttons["start_app"].grid(row=2, column=0, padx=5, pady=4)
            self.media_buttons["stop_app"].grid(row=2, column=1, padx=5, pady=4)
            self.media_buttons["play"].grid(row=2, column=2, padx=5, pady=4)
            self.media_buttons["pause"].grid(row=3, column=0, padx=5, pady=4)
            self.media_buttons["prev"].grid(row=3, column=1, padx=5, pady=4)
            self.media_buttons["next"].grid(row=3, column=2, padx=5, pady=4)

            self.refresh_media_ui()

            status_label = ttk.Label(frame, textvariable=self.status_var)
            status_label.grid(row=6, column=0, columnspan=2, pady=10)

            # Buttons

            add_notification_button = ttk.Button(frame, text="Add Notification", command=self.core.add_notification)
            add_notification_button.grid(row=4, column=0, padx=5, pady=10)

            remove_notification_button = ttk.Button(frame, text="Remove Notification", command=self.core.remove_notification)
            remove_notification_button.grid(row=4, column=1, padx=5, pady=10)

            send_gps_button = ttk.Button(frame, text="Send Random GPS", command=self.core.send_gps)
            send_gps_button.grid(row=5, column=0, padx=5, pady=10)

            exit_button = ttk.Button(frame, text="Exit", command=self.exit_app)
            exit_button.grid(row=5, column=1, padx=5, pady=10)

        def refresh_media_ui(self):
            track = self.core.media.current_track
            if track:
                self.media_track_var.set(f"Track: {track['title']} - {track['artist']}")
            else:
                self.media_track_var.set("Track: N/A")
            self.media_state_var.set(f"State: {self.core.media.playback_state.value}")

            available = self.core.media.available_buttons
            for key, button in self.media_buttons.items():
                if available.get(key, False):
                    button.state(["!disabled"])
                else:
                    button.state(["disabled"])

        def on_media_start_app(self):
            self.core.media_start_app()
            self.refresh_media_ui()

        def on_media_stop_app(self):
            self.core.media_stop_app()
            self.refresh_media_ui()

        def on_media_play(self):
            self.core.media_play()
            self.refresh_media_ui()

        def on_media_pause(self):
            self.core.media_pause()
            self.refresh_media_ui()

        def on_media_prev(self):
            self.core.media_prev()
            self.refresh_media_ui()

        def on_media_next(self):
            self.core.media_next()
            self.refresh_media_ui()

        def start_server(self):
            server_thread = threading.Thread(target=self.run_server)
            server_thread.daemon = True
            server_thread.start()

        def run_server(self):
            with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s:
                s.bind((self.ip_address, 9090))
                s.listen()
                while True:
                    self.status_var.set("Listening...")
                    print("Server listening on port 9090...")
                    self.core.conn, addr = s.accept()
                    self.status_var.set(f"Connected by {addr}")
                    print(f"Connected by {addr}")

                    buffer = b''
                    try:
                        while True:
                            data = self.core.conn.recv(1024)
                            if not data:
                                break
                            buffer += data
                            while b'\n' in buffer:
                                line, buffer = buffer.split(b'\n', 1)
                                if line:
                                    try:
                                        message: RPCMessage = json.loads(line.decode('utf-8'))
                                        self.handle_message(message)
                                    except json.JSONDecodeError:
                                        print(f"Error decoding JSON: {line.decode('utf-8')}")
                    except ConnectionResetError:
                        pass  # client disconnected

                    self.core.conn = None
                    self.status_var.set("Disconnected")
                    self.clear_device_info()

        def handle_message(self, message: RPCMessage):
            service = message.get('service')
            payload = message.get('payload')
            msg_type = message.get('type')

            if service == 'device':
                if msg_type == 'com.damn.anotherglass.shared.device.BatteryStatusData' and isinstance(payload, dict):
                    self.update_battery_status(payload)
                elif msg_type == 'name' and isinstance(payload, str):
                    self.update_device_name(payload)
            elif service == 'Media':
                if msg_type == 'com.damn.anotherglass.shared.media.MediaCommandData' and isinstance(payload, dict):
                    self.handle_media_command(payload)

        def handle_media_command(self, payload: dict):
            command = payload.get('command')
            print(f"Received media command: {command}")
            if command == 'TogglePlayPause':
                if self.core.media.playback_state == PlaybackStateValue.Playing:
                    self.core.media_pause()
                else:
                    self.core.media_play()
            elif command == 'Play':
                self.core.media_play()
            elif command == 'Pause':
                self.core.media_pause()
            elif command == 'Next':
                self.core.media_next()
            elif command == 'Previous':
                self.core.media_prev()
            self.root.after(0, self.refresh_media_ui)

        def update_device_name(self, name: str):
            self.device_name_var.set(f"Name: {name}")

        def update_battery_status(self, data: BatteryStatusData):
            level = data['level']
            is_charging = data['isCharging']
            status_str = f"{level}%"
            if is_charging:
                status_str += " ⚡"
            self.battery_status_var.set(f"Battery: {status_str}")

        def clear_device_info(self):
            self.device_name_var.set("Name: N/A")
            self.battery_status_var.set("Battery: N/A")

        def exit_app(self):
            self.core.exit()
            self.root.destroy()

    root = tk.Tk()
    App(root)
    root.mainloop()


def main_console():
    core = Core()
    ip_address = get_local_ip()
    if not ip_address:
        print("Could not determine local IP address. Please connect to a network.")
        return

    # Generate and display QR code
    qr = qrcode.QRCode(
        version=1,
        error_correction=qrcode.constants.ERROR_CORRECT_L,
        box_size=10,
        border=4,
    )
    qr.add_data(ip_address)
    qr.make(fit=True)
    print("Scan the QR code with the glass-ee client to connect.")
    qr.print_tty()
    print(f"IP Address: {ip_address}")

    with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s:
        s.bind((ip_address, 9090))
        s.listen()
        print("Server listening on port 9090...")
        conn, addr = s.accept()
        with conn:
            core.conn = conn
            print(f"Connected by {addr}")
            while True:
                media = core.media
                track = media.current_track
                track_name = f"{track['title']} - {track['artist']}" if track else "N/A"
                print(f"\nMedia state: {media.playback_state.value}; Track: {track_name}")
                print(f"Available media actions: {', '.join([k for k, v in media.available_buttons.items() if v])}")

                print("\nMenu:")
                print("1. Send random GPS location")
                print("2. Add debug notification")
                print("3. Remove debug notification")
                print("4. Media: Start App")
                print("5. Media: Stop App")
                print("6. Media: Play")
                print("7. Media: Pause")
                print("8. Media: Prev")
                print("9. Media: Next")
                print("0. Exit")
                choice = input("Enter your choice: ")

                if choice == '1':
                    core.send_gps()
                elif choice == '2':
                    core.add_notification()
                elif choice == '3':
                    core.remove_notification()
                elif choice == '4':
                    core.media_start_app()
                elif choice == '5':
                    core.media_stop_app()
                elif choice == '6':
                    core.media_play()
                elif choice == '7':
                    core.media_pause()
                elif choice == '8':
                    core.media_prev()
                elif choice == '9':
                    core.media_next()
                elif choice == '0':
                    core.exit()
                    break
                else:
                    print("Invalid choice. Please try again.")

if __name__ == "__main__":
    import sys
    if "--ui" in sys.argv:
        main_ui()
    else:
        main_console()
