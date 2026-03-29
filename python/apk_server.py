import mimetypes
import socket
import threading
import tkinter as tk
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path
from tkinter import messagebox, ttk
from typing import Optional, cast
from urllib.parse import quote, urlparse

import qrcode
from PIL import Image
from PIL import ImageTk


def get_local_ip() -> str:
    """Return the best-effort LAN IP for sharing a local URL."""
    probe = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    try:
        probe.connect(("8.8.8.8", 80))
        return probe.getsockname()[0]
    except OSError:
        try:
            return socket.gethostbyname(socket.gethostname())
        except OSError:
            return "127.0.0.1"
    finally:
        probe.close()


class SingleFileServer:
    def __init__(self) -> None:
        self._httpd: Optional[ThreadingHTTPServer] = None
        self._thread: Optional[threading.Thread] = None
        self.file_path: Optional[Path] = None
        self.public_path: Optional[str] = None

    @property
    def is_running(self) -> bool:
        return self._httpd is not None

    def start(self, file_path: Path, port: int) -> None:
        self.stop()
        self.file_path = file_path
        self.public_path = "/" + quote(file_path.name)

        file_size = file_path.stat().st_size
        content_type = mimetypes.guess_type(file_path.name)[0] or "application/octet-stream"
        expected_path = self.public_path

        class Handler(BaseHTTPRequestHandler):
            def do_GET(self) -> None:  # noqa: N802
                requested_path = urlparse(self.path).path
                if requested_path != expected_path:
                    self.send_error(404, "File not found")
                    return

                self.send_response(200)
                self.send_header("Content-Type", content_type)
                self.send_header("Content-Length", str(file_size))
                self.send_header("Content-Disposition", f'attachment; filename="{file_path.name}"')
                self.end_headers()

                with file_path.open("rb") as fh:
                    while True:
                        chunk = fh.read(1024 * 64)
                        if not chunk:
                            break
                        self.wfile.write(chunk)

            def log_message(self, fmt: str, *args) -> None:
                # Keep the UI console clean while serving.
                return

        self._httpd = ThreadingHTTPServer(("0.0.0.0", port), Handler)  # type: ignore[arg-type]
        self._httpd.daemon_threads = True
        self._thread = threading.Thread(target=self._httpd.serve_forever, daemon=True)
        self._thread.start()

    def stop(self) -> None:
        if self._httpd is None:
            return

        self._httpd.shutdown()
        self._httpd.server_close()
        if self._thread is not None:
            self._thread.join(timeout=1)

        self._httpd = None
        self._thread = None


class ApkServerApp:
    def __init__(self, root: tk.Tk) -> None:
        self.root = root
        self.root.title("ApkServer")
        self.root.resizable(False, False)

        self.script_dir = Path(__file__).resolve().parent
        self.server = SingleFileServer()
        self.local_ip = get_local_ip()
        self.current_url: Optional[str] = None
        self.qr_image: Optional[ImageTk.PhotoImage] = None

        self.status_var = tk.StringVar(value="Ready")
        self.url_var = tk.StringVar(value="URL: -")
        self.port_var = tk.StringVar(value="8080")

        self._build_ui()
        self._load_apk_list()

        self.root.protocol("WM_DELETE_WINDOW", self._on_close)

    def _build_ui(self) -> None:
        frame = ttk.Frame(self.root, padding=10)
        frame.grid(row=0, column=0, sticky="nsew")

        ttk.Label(frame, text=f"Directory: {self.script_dir}").grid(row=0, column=0, columnspan=3, sticky=tk.W)

        ttk.Label(frame, text="APK files").grid(row=1, column=0, sticky=tk.W, pady=(8, 2))
        self.apk_listbox = tk.Listbox(frame, height=10, width=55, exportselection=False)
        self.apk_listbox.grid(row=2, column=0, columnspan=3, sticky="we")
        self.apk_listbox.bind("<<ListboxSelect>>", self._on_selection_changed)

        controls = ttk.Frame(frame)
        controls.grid(row=3, column=0, columnspan=3, sticky=tk.W, pady=(8, 0))

        ttk.Button(controls, text="Refresh", command=self._load_apk_list).grid(row=0, column=0, padx=(0, 6))
        ttk.Label(controls, text="Port:").grid(row=0, column=1, padx=(6, 4))
        ttk.Entry(controls, textvariable=self.port_var, width=7).grid(row=0, column=2)
        ttk.Button(controls, text="Start", command=self.start_selected).grid(row=0, column=3, padx=(8, 4))
        ttk.Button(controls, text="Stop", command=self.stop_server).grid(row=0, column=4)

        ttk.Label(frame, textvariable=self.url_var, foreground="blue").grid(row=4, column=0, columnspan=3, sticky=tk.W, pady=(10, 4))

        self.qr_label = ttk.Label(frame)
        self.qr_label.grid(row=5, column=0, columnspan=3, pady=6)

        ttk.Label(frame, textvariable=self.status_var).grid(row=6, column=0, columnspan=3, sticky=tk.W, pady=(4, 0))

    def _load_apk_list(self) -> None:
        self.apk_listbox.delete(0, tk.END)
        apks = sorted(
            [p for p in self.script_dir.iterdir() if p.is_file() and p.suffix.lower() == ".apk"],
            key=lambda p: p.name.lower(),
        )

        for apk in apks:
            self.apk_listbox.insert(tk.END, apk.name)

        if apks:
            self.apk_listbox.selection_set(0)
            self._on_selection_changed()
            self.status_var.set(f"Found {len(apks)} APK file(s).")
        else:
            self.current_url = None
            self.url_var.set("URL: -")
            self.qr_label.configure(image="")
            self.status_var.set("No APK files found in this directory.")

    def _selected_apk(self) -> Optional[Path]:
        selection = self.apk_listbox.curselection()
        if not selection:
            return None
        file_name = self.apk_listbox.get(selection[0])
        return self.script_dir / file_name

    def _on_selection_changed(self, _event: Optional[object] = None) -> None:
        selected = self._selected_apk()
        if selected is None:
            self.current_url = None
            self.url_var.set("URL: -")
            self.qr_label.configure(image="")
            return

        if self.server.public_path and self.server.is_running:
            self.current_url = f"http://{self.local_ip}:{self._read_port()}{'/' + quote(selected.name)}"
        else:
            self.current_url = f"http://{self.local_ip}:{self._read_port()}/{quote(selected.name)}"

        self.url_var.set(f"URL: {self.current_url}")
        self._update_qr(self.current_url)

        if self.server.is_running:
            self.start_selected()

    def _update_qr(self, text: str) -> None:
        qr = qrcode.QRCode(
            version=1,
            error_correction=qrcode.constants.ERROR_CORRECT_L,
            box_size=8,
            border=4,
        )
        qr.add_data(text)
        qr.make(fit=True)

        image_wrapper = qr.make_image(fill_color="black", back_color="white")
        pil_image = image_wrapper.get_image() if hasattr(image_wrapper, "get_image") else image_wrapper
        self.qr_image = ImageTk.PhotoImage(cast(Image.Image, pil_image))
        self.qr_label.configure(image=self.qr_image)

    def _read_port(self) -> int:
        try:
            port = int(self.port_var.get().strip())
        except ValueError:
            return 8080
        if port < 1 or port > 65535:
            return 8080
        return port

    def start_selected(self) -> None:
        selected = self._selected_apk()
        if selected is None:
            messagebox.showwarning("No file", "Select an APK file first.")
            return

        port = self._read_port()
        if str(port) != self.port_var.get().strip():
            self.port_var.set(str(port))

        try:
            self.server.start(selected, port)
        except OSError as exc:
            messagebox.showerror("Server error", f"Could not start server on port {port}: {exc}")
            self.status_var.set(f"Failed to start server on port {port}.")
            return

        self.current_url = f"http://{self.local_ip}:{port}/{quote(selected.name)}"
        self.url_var.set(f"URL: {self.current_url}")
        self._update_qr(self.current_url)
        self.status_var.set(f"Serving {selected.name} on port {port}.")

    def stop_server(self) -> None:
        self.server.stop()
        self.status_var.set("Server stopped.")

    def _on_close(self) -> None:
        self.server.stop()
        self.root.destroy()


def main() -> None:
    root = tk.Tk()
    ApkServerApp(root)
    root.mainloop()


if __name__ == "__main__":
    main()

