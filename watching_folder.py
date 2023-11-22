import os
import time
from watchdog.observers import Observer
from watchdog.events import FileSystemEventHandler

class MyHandler(FileSystemEventHandler):
    """
    Custom handler class for file system events.

    Methods:
    on_created(event): Called when a file is created in the monitored directory.
    on_deleted(event): Called when a file is deleted from the monitored directory.
    """

    def on_created(self, event):
        """Called when a file is created in the monitored directory."""
        if not event.is_directory:
            size = os.path.getsize(event.src_path)
            print(f"Added: {event.src_path}, Size: {size} bytes")

    def on_deleted(self, event):
        """Called when a file is deleted from the monitored directory."""
        if not event.is_directory:
            print(f"Removed: {event.src_path}")

folder_path = "D:/Python/test"
event_handler = MyHandler()
observer = Observer()
observer.schedule(event_handler, folder_path, recursive=False)
observer.start()

# Watchdog method provides efficient and real-time file change detection.
# Strength: Highly efficient and responsive to file system events.
# Weakness: Relies on external library (watchdog) and more complex to implement.
# Weakness: May have compatibility issues depending on OS and file system capabilities.
try:
    while True:
        time.sleep(1)
except KeyboardInterrupt:
    observer.stop()
observer.join()
