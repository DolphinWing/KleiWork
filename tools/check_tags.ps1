$OutputEncoding = [System.Text.Encoding]::UTF8
$filePath = "D:\\work\\misc\\KleiWork\\workshop-2906930548\\strings.po"
$entryLines = New-Object System.Collections.Generic.List[string]
$entryStartLine = 0
$currentLine = 0
$mismatchesFound = $false # Initialize flag
$entryCount = 0
$errorCount = 0

function Write-HighlightedLine {
    param(
        [string]$line,
        [string]$regexPattern = "(<style=.*?>)|(</style>)|(<link=.*?>)|(</link>)|(<smallcaps>)|(</smallcaps>)",
        [string]$tagColor = "Cyan"
    )

    $matches = [regex]::Matches($line, $regexPattern)
    $lastIndex = 0

    # If there are no matches, just print the line and return
    if ($matches.Count -eq 0) {
        Write-Host -Object $line
        return
    }

    foreach ($match in $matches) {
        # Print text before the tag
        $nonMatchLength = $match.Index - $lastIndex
        if ($nonMatchLength -gt 0) {
            Write-Host -Object $line.Substring($lastIndex, $nonMatchLength) -NoNewline
        }

        # Print the tag in color
        Write-Host -Object $match.Value -ForegroundColor $tagColor -NoNewline

        $lastIndex = $match.Index + $match.Length
    }

    # Print any remaining text after the last tag
    if ($lastIndex -lt $line.Length) {
        Write-Host -Object $line.Substring($lastIndex)
    } else {
        # If the line ended with a tag, we need to add a newline
        Write-Host ""
    }
}

function Test-Entry {
    param(
        [System.Collections.Generic.List[string]]$entryLines,
        [int]$entryStartLine
    )

    $msgid = ''
    $msgstr = ''
    $isMsgId = $false
    $isMsgStr = $false

    foreach ($line in $entryLines) {
        if ($line -match '^msgid') {
            $isMsgId = $true
            $isMsgStr = $false
            $msgid += $line
        } elseif ($line -match '^msgstr') {
            $isMsgId = $false
            $isMsgStr = $true
            $msgstr += $line
        } elseif ($isMsgId -and $line -match '^\s*\"') {
            $msgid += $line
        } elseif ($isMsgStr -and $line -match '^\s*\"') {
            $msgstr += $line
        }
    }

    # Extract content from the multi-line strings
    $msgidContent = ($msgid -replace 'msgid\s+', '' -replace '`r|`n', '' -replace '^\s*\"\s*|\s*\"\s*$', '' -replace '\"\s*\"', '')
    $msgstrContent = ($msgstr -replace 'msgstr\s+', '' -replace '`r|`n', '' -replace '^\s*\"\s*|\s*\"\s*$', '' -replace '\"\s*\"', '')

    $tagsToCheck = @('link', 'style', 'smallcaps')
    $entryHasMismatch = $false

    foreach ($tag in $tagsToCheck) {
        # Handle tags with attributes like <link=...> and <style=...>
        $openTagPattern = if ($tag -eq 'link' -or $tag -eq 'style') { "<${tag}=" } else { "<${tag}>" }
        $closeTagPattern = "</${tag}>"

        $msgidOpenCount = ([regex]::Matches($msgidContent, $openTagPattern)).Count
        $msgstrOpenCount = ([regex]::Matches($msgstrContent, $openTagPattern)).Count
        $msgidCloseCount = ([regex]::Matches($msgidContent, $closeTagPattern)).Count
        $msgstrCloseCount = ([regex]::Matches($msgstrContent, $closeTagPattern)).Count

        if (($msgidOpenCount -ne $msgstrOpenCount) -or
            ($msgidCloseCount -ne $msgstrCloseCount) -or
            ($msgidOpenCount -ne $msgidCloseCount)) { # Check for balance within msgid
            
            $entryHasMismatch = $true # Mark that at least one mismatch was found for this entry
            
            # Print the specific error for this tag
            Write-Warning "Mismatched '${tag}' tag count found at line ${entryStartLine}:"
            Write-Warning "  msgid: open=$msgidOpenCount, close=$msgidCloseCount, msgstr: open=$msgstrOpenCount, close=$msgstrCloseCount"
        }
    }

    if ($entryHasMismatch) {
        # Print the full entry lines for context, with highlighting
        foreach ($entryLine in $entryLines) {
            Write-HighlightedLine -line $entryLine
        }
        Write-Host ""
        return $true
    }

    return $false
}

# Using switch -file is efficient for large files
switch -file $filePath {
    default {
        $currentLine++
        if ([string]::IsNullOrWhiteSpace($_)) {
            if ($entryLines.Count -gt 0) {
                $entryCount++
                if (Test-Entry -entryLines $entryLines -entryStartLine $entryStartLine) {
                    $mismatchesFound = $true
                    $errorCount++
                }
            }
            $entryLines.Clear()
            $entryStartLine = 0
        } else {
            if ($entryStartLine -eq 0) {
                $entryStartLine = $currentLine
            }
            $entryLines.Add($_)
        }
    }
}

# Check the last entry in case the file doesn't end with a blank line
if ($entryLines.Count -gt 0) {
    $entryCount++
    if (Test-Entry -entryLines $entryLines -entryStartLine $entryStartLine) {
        $mismatchesFound = $true
        $errorCount++
    }
}

# --- Final Output ---
Write-Host "Total entries found: $entryCount"
if (-not $mismatchesFound) {
    Write-Host "... No mismatched tag counts found between msgid and msgstr."
} else {
    Write-Warning "... Found $errorCount entries have mismatched tags."
}
Write-Output ""