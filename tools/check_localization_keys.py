import os
import glob
import argparse
from collections import OrderedDict

def parse_properties_file(filepath):
    """Parses a .properties file and returns an OrderedDict of key-value pairs."""
    properties = OrderedDict()
    try:
        with open(filepath, 'r', encoding='utf-8') as f:
            for line in f:
                line = line.strip()
                if not line or line.startswith('#') or line.startswith('!'):
                    continue
                # Handle key-value pairs separated by '=' or ':'
                if '=' in line:
                    key, value = line.split('=', 1)
                elif ':' in line:
                    key, value = line.split(':', 1)
                else:
                    continue # Skip lines that don't look like key-value pairs
                
                properties[key.strip()] = value.strip()
    except FileNotFoundError:
        print(f"Warning: File not found at {filepath}")
    return properties

def main():
    parser = argparse.ArgumentParser(description="Check and optionally fix missing localization keys in .properties files.")
    parser.add_argument('--fix', action='store_true', help="Automatically add missing keys from the base 'strings.properties' to other language files.")
    args = parser.parse_args()

    resource_dir = 'src/main/resources'
    base_file_name = 'strings.properties'
    base_filepath = os.path.join(resource_dir, base_file_name)

    if not os.path.exists(base_filepath):
        print(f"Error: Base file '{base_filepath}' not found.")
        return

    print(f"Loading base properties from: {base_filepath}")
    base_properties = parse_properties_file(base_filepath)
    print(f"Found {len(base_properties)} keys in {base_file_name}")

    # Find all other strings_xx_YY.properties files
    language_files = glob.glob(os.path.join(resource_dir, 'strings_*.properties'))

    if not language_files:
        print("No other language-specific .properties files found to compare.")
        return

    for lang_filepath in language_files:
        lang_file_name = os.path.basename(lang_filepath)
        print(f"\n--- Comparing {lang_file_name} ---")
        lang_properties = parse_properties_file(lang_filepath)

        missing_in_lang = [key for key in base_properties if key not in lang_properties]
        extra_in_lang = [key for key in lang_properties if key not in base_properties]

        if missing_in_lang:
            print(f"Keys missing in {lang_file_name}:")
            if args.fix:
                with open(lang_filepath, 'a', encoding='utf-8') as f:
                    for key in missing_in_lang:
                        value = base_properties[key]
                        f.write(f"\n{key}={value}")
                        print(f"  + Added '{key}={value}'")
                print(f"  Automatically added {len(missing_in_lang)} missing keys to {lang_file_name}.")
            else:
                for key in missing_in_lang:
                    print(f"  - {key}")
                print(f"  To fix, run the script with '--fix' argument: python {os.path.basename(__file__)} --fix")
        else:
            print(f"No missing keys in {lang_file_name}.")

        if extra_in_lang:
            print(f"Extra keys found in {lang_file_name}:")
            for key in extra_in_lang:
                print(f"  - {key} (Consider removing if not needed)")
        else:
            print(f"No extra keys in {lang_file_name}.")

    print("\n--- Comparison complete ---")

if __name__ == '__main__':
    main()
