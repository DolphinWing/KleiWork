$OutputEncoding = [System.Text.Encoding]::UTF8
$filePath = Join-Path -Path $PSScriptRoot -ChildPath "..\workshop-2906930548\strings.po"
$entryLines = New-Object System.Collections.Generic.List[string]
$entryStartLine = 0
$currentLine = 0
$mismatchesFound = $false # Initialize flag

# Using switch -file is efficient for large files
switch -file $filePath {
    default {
        $currentLine++
        if ([string]::IsNullOrWhiteSpace($_)) {
            if ($entryLines.Count -gt 0) {
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
                    } elseif ($isMsgId -and $line -match '^\s*"') {
                        $msgid += $line
                    } elseif ($isMsgStr -and $line -match '^\s*"') {
                        $msgstr += $line
                    }
                }

                # Extract content from the multi-line strings
                $msgidContent = ($msgid -replace 'msgid\s+', '' -replace '`r|`n', '' -replace '^\s*"\s*|\s*"\s*$', '' -replace '"\s*"', '')
                $msgstrContent = ($msgstr -replace 'msgstr\s+', '' -replace '`r|`n', '' -replace '^\s*"\s*|\s*"\s*$', '' -replace '"\s*"', '')
                
                $combined = $msgidContent + $msgstrContent

                $openLinkCount = ([regex]::Matches($combined, '<link=')).Count
                $closeLinkCount = ([regex]::Matches($combined, '</link>')).Count
                $openStyleCount = ([regex]::Matches($combined, '<style=')).Count
                $closeStyleCount = ([regex]::Matches($combined, '</style>')).Count

                if (($openLinkCount -ne $closeLinkCount) -or ($openStyleCount -ne $closeStyleCount)) {
                    Write-Output "Mismatched entry found starting at line ${entryStartLine}:"
                    $entryLines | Write-Output
                    Write-Output ""
                    $mismatchesFound = $true # Set flag to true
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
        } elseif ($isMsgId -and $line -match '^\s*"') {
            $msgid += $line
        } elseif ($isMsgStr -and $line -match '^\s*"') {
            $msgstr += $line
        }
    }

    $msgidContent = ($msgid -replace 'msgid\s+', '' -replace '`r|`n', '' -replace '^\s*"\s*|\s*"\s*$', '' -replace '"\s*"', '')
    $msgstrContent = ($msgstr -replace 'msgstr\s+', '' -replace '`r|`n', '' -replace '^\s*"\s*|\s*"\s*$', '' -replace '"\s*"', '')
    
    $combined = $msgidContent + $msgstrContent

    $openLinkCount = ([regex]::Matches($combined, '<link=')).Count
    $closeLinkCount = ([regex]::Matches($combined, '</link>')).Count
    $openStyleCount = ([regex]::Matches($combined, '<style=')).Count
    $closeStyleCount = ([regex]::Matches($combined, '</style>')).Count

    if (($openLinkCount -ne $closeLinkCount) -or ($openStyleCount -ne $closeStyleCount)) {
        Write-Output "Mismatched entry found starting at line ${entryStartLine}:"
        $entryLines | Write-Output
        Write-Output ""
        $mismatchesFound = $true # Set flag to true
    }
}

# New logic to print message if no mismatches found
if (-not $mismatchesFound) {
    Write-Output "... No mismatched <link> or <style> tags found."
}