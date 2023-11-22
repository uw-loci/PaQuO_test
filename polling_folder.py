import os
import time

def get_files(folder_path):
    """
    Returns a set of file names in the folder.

    Args:
    folder_path (str): Path to the folder to be monitored.

    Returns:
    set: A set of file names present in the folder.
    """
    return set(os.listdir(folder_path))

def print_file_changes(previous_files, current_files):
    """
    Prints the changes in files between two sets of file names.

    Args:
    previous_files (set): Set of file names previously in the folder.
    current_files (set): Set of file names currently in the folder.
    """
    added = current_files - previous_files
    removed = previous_files - current_files

    for file in added:
        size = os.path.getsize(os.path.join(folder_path, file))
        print(f"Added: {file}, Size: {size} bytes")

    for file in removed:
        print(f"Removed: {file}")

folder_path = "D:/Python/test"
previous_files = get_files(folder_path)

# Polling method is straightforward but less efficient for high-frequency file changes.
# Strength: Easy to implement with no external dependencies.
# Weakness: High resource usage due to continuous polling.
# Weakness: Not immediate in response, with a delay up to the polling interval (1 second here).
while True:
    current_files = get_files(folder_path)
    if current_files != previous_files:
        print_file_changes(previous_files, current_files)
        previous_files = current_files
    time.sleep(1)  # Interval set to 1 second
