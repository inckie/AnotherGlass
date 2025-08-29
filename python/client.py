import json
import random
import socket
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

def send_message(
        conn: socket.socket,
        message: RPCMessage
) -> bool:
    try:
        conn.sendall(json.dumps(message).encode('utf-8'))
        conn.sendall(b'\n')
        print(f"Sent message: {message.get('service')}")
    except socket.error as e:
        print(f"Error sending message: {e}")
        return False
    return True

def main() -> None:
    notification_id = 0
    ip_address = get_local_ip()
    if not ip_address:
        print("Could not determine local IP address. Please connect to a network.")
        return

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
            print(f"Connected by {addr}")
            while True:
                print("\nMenu:")
                print("1. Send random GPS location")
                print("2. Add debug notification")
                print("3. Remove debug notification")
                print("4. Exit")
                choice = input("Enter your choice: ")

                if choice == '1':
                    location = random.choice(LOCATIONS)
                    message = create_rpc_message("MockGPS", location)
                    if not send_message(conn, message):
                        break
                elif choice == '2':
                    notification_id += 1
                    notification = BASE_NOTIFICATION.copy()
                    notification['id'] = notification_id
                    notification['action'] = NotificationAction.Posted
                    notification['title'] = notification['title'] + ' ' + str(notification_id)
                    notification['postedTime'] = int(time.time() * 1000)
                    message = create_rpc_message("Notifications", notification)
                    if not send_message(conn, message):
                        break
                elif choice == '3':
                    if notification_id > 0:
                        notification = BASE_NOTIFICATION.copy()
                        notification['id'] = notification_id
                        notification['action'] = NotificationAction.Removed
                        message = create_rpc_message("Notifications", notification)
                        if not send_message(conn, message):
                            break
                        notification_id -= 1
                    else:
                        print("No notifications to remove.")
                elif choice == '4':
                    message: RPCMessage = {"service": None, "type": None, "payload": None}
                    send_message(conn, message)
                    break
                else:
                    print("Invalid choice. Please try again.")

if __name__ == "__main__":
    main()
