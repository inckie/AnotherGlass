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


class RPCMessage(TypedDict):
    service: Optional[str]
    type: Optional[str]
    payload: Optional[Union[Location, NotificationData]]


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
        payload: Union[Location, NotificationData]
) -> RPCMessage:
    payload_type = {
        "MockGPS": "com.damn.anotherglass.shared.gps.Location",
        "Notifications": "com.damn.anotherglass.shared.notifications.NotificationData"
    }
    return {
        "service": service,
        "type": payload_type.get(service),
        "payload": payload
    }


class Core:
    def __init__(self):
        self.notification_id = 0
        self.conn: Optional[socket.socket] = None

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

            status_label = ttk.Label(frame, textvariable=self.status_var)
            status_label.grid(row=4, column=0, columnspan=2, pady=10)

            # Buttons

            add_notification_button = ttk.Button(frame, text="Add Notification", command=self.core.add_notification)
            add_notification_button.grid(row=2, column=0, padx=5, pady=10)

            remove_notification_button = ttk.Button(frame, text="Remove Notification", command=self.core.remove_notification)
            remove_notification_button.grid(row=2, column=1, padx=5, pady=10)

            send_gps_button = ttk.Button(frame, text="Send Random GPS", command=self.core.send_gps)
            send_gps_button.grid(row=3, column=0, padx=5, pady=10)

            exit_button = ttk.Button(frame, text="Exit", command=self.exit_app)
            exit_button.grid(row=3, column=1, padx=5, pady=10)

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
                    # a simple loop to block until client disconnects
                    try:
                        while True:
                            data = self.core.conn.recv(1024)
                            if not data:
                                break
                    except ConnectionResetError:
                        pass # client disconnected
                    self.core.conn = None
                    self.status_var.set("Disconnected")

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
                print("\nMenu:")
                print("1. Send random GPS location")
                print("2. Add debug notification")
                print("3. Remove debug notification")
                print("4. Exit")
                choice = input("Enter your choice: ")

                if choice == '1':
                    core.send_gps()
                elif choice == '2':
                    core.add_notification()
                elif choice == '3':
                    core.remove_notification()
                elif choice == '4':
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