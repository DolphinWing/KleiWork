#!/bin/bash

# This script checks for mismatched tags in .po files, similar to its PowerShell counterpart.
# It's designed to be run in a bash environment, including WSL on Windows.

# --- Configuration ---
SCRIPT_DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )
DEFAULT_FILE_PATH="$SCRIPT_DIR/../workshop-2906930548/strings.po"
FILE_PATH="${1:-$DEFAULT_FILE_PATH}"
echo "FILE_PATH=$FILE_PATH"

# --- WSL Path Conversion ---
# If running in WSL, convert Windows-style paths (e.g., C:\Users) to WSL paths (e.g., /mnt/c/Users).
if [[ -n "$WSL_DISTRO_NAME" && "$FILE_PATH" =~ ^([A-Za-z]):(.*) ]]; then
    drive=$(echo "${BASH_REMATCH[1]}" | tr '[:upper:]' '[:lower:]')
    path=${BASH_REMATCH[2]}
    path=${path//\\/\/}
    FILE_PATH="/mnt/$drive$path"
fi

# Check if the file exists
if [ ! -f "$FILE_PATH" ]; then
    echo "Error: File not found at '$FILE_PATH'" >&2
    exit 1
fi

# --- Colors for terminal output ---
YELLOW='\033[1;33m' # For warnings
NC='\033[0m'       # No Color

# --- Global state variables ---
mismatches_found=false
entry_count=0
error_count=0
current_line=0
entry_start_line=0

# --- Functions ---
# Function to test a single .po entry for mismatched tag counts between msgid and msgstr.
# This version uses a single awk process per entry for high performance.
#
# @param {string} entry_lines The multi-line string containing the full entry.
# @param {number} entry_start_line The starting line number of the entry in the file.
# @returns {number} The exit code from awk (0 for mismatch, 1 for none).
test_entry() {
    local entry_lines="$1"
    local entry_start_line="$2"

    # AWK script to perform the heavy lifting of parsing, counting, and printing in the correct order.
    local awk_script='
    BEGIN {
        tags_to_check[0] = "link"; tags_to_check[1] = "style"; tags_to_check[2] = "smallcaps";
        entry_has_mismatch = 0;
        mismatch_details = "";
        line_num = 0;
    }
    {
        # Buffer every line of the entry
        buffered_lines[line_num++] = $0;

        # Parse msgid/msgstr content
        if ($0 ~ /^msgid/) {
            context = "msgid";
            gsub(/^msgid\s*"/, ""); gsub(/"\s*$/, "");
            msgid_content = msgid_content $0;
        } else if ($0 ~ /^msgstr/) {
            context = "msgstr";
            gsub(/^msgstr\s*"/, ""); gsub(/"\s*$/, "");
            msgstr_content = msgstr_content $0;
        } else if ($0 ~ /^\s*"/) {
            gsub(/^\s*"/, ""); gsub(/"\s*$/, "");
            if (context == "msgid") { msgid_content = msgid_content $0; }
            else if (context == "msgstr") { msgstr_content = msgstr_content $0; }
        }
    }
    END {
        # Perform mismatch check
        for (i in tags_to_check) {
            tag = tags_to_check[i];
            
            if (tag == "link" || tag == "style") { open_tag_pattern = "<" tag "="; }
            else { open_tag_pattern = "<" tag ">"; }
            close_tag_pattern = "</" tag ">";

            temp_str = msgid_content; msgid_open_count = gsub(open_tag_pattern, "&", temp_str);
            temp_str = msgstr_content; msgstr_open_count = gsub(open_tag_pattern, "&", temp_str);
            temp_str = msgid_content; msgid_close_count = gsub(close_tag_pattern, "&", temp_str);
            temp_str = msgstr_content; msgstr_close_count = gsub(close_tag_pattern, "&", temp_str);

            if (msgid_open_count != msgstr_open_count || \
                msgid_close_count != msgstr_close_count || \
                msgid_open_count != msgid_close_count) {
                
                entry_has_mismatch = 1;
                mismatch_details = mismatch_details sprintf("  - Tag '\''%s'\'': msgid open=%d, close=%d, msgstr open=%d, close=%d\n", tag, msgid_open_count, msgid_close_count, msgstr_open_count, msgstr_close_count);
            }
        }
        
        # If a mismatch was found, print all information in the desired order
        if (entry_has_mismatch) {
            # 1. Print warnings
            printf "%sMismatched tag count found at line %d:%s\n", YELLOW, entry_start_line, NC > "/dev/stderr";
            printf "%s%s%s", YELLOW, mismatch_details, NC > "/dev/stderr";
            
            # 2. Print context lines (comments, etc.)
            for (i = 0; i < line_num; i++) {
                if (buffered_lines[i] !~ /^msgid/ && buffered_lines[i] !~ /^msgstr/ && buffered_lines[i] !~ /^\s*"/) {
                    print buffered_lines[i] > "/dev/stderr";
                }
            }
            
            # 3. Clean and print compact msgid/msgstr content
            gsub(/[\n\r\t]/, "", msgid_content);
            gsub(/[\n\r\t]/, "", msgstr_content);
            print "msgid " msgid_content > "/dev/stderr";
            print "msgstr " msgstr_content > "/dev/stderr";
            print "" > "/dev/stderr";

            exit 0;
        } else {
            exit 1;
        }
    }'

    # Execute the awk script and pass its exit code up.
    echo "$entry_lines" | awk -v entry_start_line="$entry_start_line" -v YELLOW="$YELLOW" -v NC="$NC" "$awk_script"
}

# --- Main processing loop ---
echo "Starting analysis of '$FILE_PATH'. This may take a moment..." >&2

# Read the file line by line, accumulating lines for each entry.
# Entries are separated by blank lines.
entry_lines=""
# `while read || [[ -n $line ]]` ensures the last line is processed if it has no trailing newline.
while IFS= read -r line || [[ -n "$line" ]]; do
    ((current_line++))
    
    # An empty line signifies the end of an entry.
    if [[ -z "$line" ]]; then
        if [[ -n "$entry_lines" ]]; then
            ((entry_count++))
            # Print progress to stderr every 100 entries.
            if (( entry_count % 100 == 0 )); then
                echo -ne "--> Processed $entry_count entries...\r" >&2
            fi

            # Call test_entry and check its return code.
            if test_entry "${entry_lines%$'\n'}" "$entry_start_line"; then
                mismatches_found=true
                ((error_count++))
            fi
        fi
        entry_lines=""
        entry_start_line=0
    else
        if [[ "$entry_start_line" -eq 0 ]]; then
            entry_start_line=$current_line
        fi
        # Append line with a newline to preserve line breaks for the functions.
        entry_lines+="$line"$'
'
    fi
done < "$FILE_PATH"

# Check the last entry in case the file doesn't end with a blank line.
if [[ -n "$entry_lines" ]]; then
    ((entry_count++))
    # The last newline added in the loop needs to be removed for the last entry.
    if test_entry "${entry_lines%$'\n'}" "$entry_start_line"; then
        mismatches_found=true
        ((error_count++))
    fi
fi

# Print a final newline to stderr to clear the progress indicator line.
echo "" >&2

# --- Final Output ---
echo "Total entries found: $entry_count"
if [[ "$mismatches_found" == false ]]; then
    echo "... No mismatched tag counts found between msgid and msgstr."
else
    echo -e "${YELLOW}... Found $error_count entries have mismatched tags.${NC}"
fi
echo ""
