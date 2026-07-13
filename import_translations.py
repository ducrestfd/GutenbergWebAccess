import csv
import os
import xml.sax.saxutils as saxutils

# --- CONFIGURATION ---
# Target directory for resources
RES_DIR = 'app/src/main/res'

# Mapping of CSV column index to the target Android locale directory
# Assuming CSV structure: Key, English, French, German, Spanish
LANG_MAP = {
    1: "values",      # Default (English)
    2: "values-fr",  # French
    3: "values-de",  # German
    4: "values-es",  # Spanish
    # 1: "values-en" # Uncomment if you also want a specific values-en folder
}

# The name of your exported CSV file
CSV_FILE = 'translations.csv'

def escape_android_string(s):
    """Escapes special characters for Android strings.xml."""
    if not s:
        return ""
    # Standard XML escaping (&, <, >)
    s = saxutils.escape(s)
    # Android specific escaping
    s = s.replace("'", "\\'")
    s = s.replace('"', '\\"')
    s = s.replace("?", "\\?")
    s = s.replace("@", "\\@")
    return s

def process_translations():
    if not os.path.exists(CSV_FILE):
        print(f"Error: '{CSV_FILE}' not found in the current directory.")
        print("Please export your spreadsheet as a CSV named 'translations.csv' with columns: Key, English, French, German, Spanish.")
        return

    # Initialize a dictionary to hold lists of strings for each locale
    translations = {loc: [] for loc in LANG_MAP.values()}

    try:
        with open(CSV_FILE, mode='r', encoding='utf-8') as f:
            reader = csv.reader(f)
            # Skip the header row
            try:
                header = next(reader)
            except StopIteration:
                print("Error: CSV file is empty.")
                return

            for row_idx, row in enumerate(reader, start=2):
                if not row:
                    continue

                # Basic validation: ensure we have at least the key and English
                if len(row) < 2:
                    print(f"Warning: Skipping row {row_idx} due to insufficient columns.")
                    continue

                key = row[0].strip()
                if not key:
                    continue

                for col_idx, loc in LANG_MAP.items():
                    if col_idx < len(row):
                        val = row[col_idx].strip()
                        if val:
                            translations[loc].append((key, escape_android_string(val)))

        # Write to strings.xml files
        for loc, items in translations.items():
            dir_path = os.path.join(RES_DIR, loc)
            # Ensure directory exists
            if not os.path.exists(dir_path):
                os.makedirs(dir_path)

            file_path = os.path.join(dir_path, 'strings.xml')

            with open(file_path, 'w', encoding='utf-8') as f:
                f.write('<?xml version="1.0" encoding="utf-8"?>\n')
                f.write('<resources>\n')
                # If you want to preserve the app_name or other existing strings,
                # you might want to merge instead of overwrite.
                # This script overwrites for a clean sync.
                for key, val in items:
                    f.write(f'    <string name="{key}">{val}</string>\n')
                f.write('</resources>\n')

            print(f"Successfully updated: {file_path} ({len(items)} strings)")

    except Exception as e:
        print(f"An error occurred: {e}")

if __name__ == "__main__":
    process_translations()
