# extract_glossary.ps1
# Goal: Extract terminology from ONI .po translation file.
# Output: oni-assets/glossary.tsv (Tab-Separated Values)

$RootPath = Resolve-Path "$PSScriptRoot\.."
$InputFile = Join-Path $RootPath "workshop-2906930548\strings.po"
$OutputFile = Join-Path $RootPath "oni-assets\glossary.tsv"

if (-not (Test-Path $InputFile)) {
    Write-Host "Error: Input file not found: $InputFile"
    exit 1
}

Write-Host "Reading: $InputFile ..."

# Load all lines
$Lines = Get-Content $InputFile -Encoding UTF8

$Results = New-Object System.Collections.Generic.List[PSObject]
$SeenNames = @{}

$currentCtxt = $null
$currentId = ""
$currentStr = ""
$state = "NONE" # NONE, CTXT, ID, STR

# Categories that should remain single-level
$SingleLevelExceptions = @("BLUEPRINTS", "ELEMENTS", "WORLD_TRAITS", "WORLDS")

foreach ($line in $Lines) {
    if ($line -match '^msgctxt "(?<val>.*\.NAME)"') {
        $currentCtxt = $Matches["val"]
        $currentId = ""
        $currentStr = ""
        $state = "CTXT"
    }
    elseif ($line -match '^msgid "(?<val>.*)"') {
        if ($state -eq "CTXT" -or $state -eq "ID") {
            $currentId += $Matches["val"]
            $state = "ID"
        }
    }
    elseif ($line -match '^msgstr "(?<val>.*)"') {
        if ($state -eq "ID" -or $state -eq "STR") {
            $currentStr += $Matches["val"]
            $state = "STR"
        }
    }
    elseif ($line -match '^"(?<val>.*)"') {
        if ($state -eq "ID") {
            $currentId += $Matches["val"]
        }
        elseif ($state -eq "STR") {
            $currentStr += $Matches["val"]
        }
    }
    elseif ([string]::IsNullOrWhiteSpace($line)) {
        if ($state -eq "STR" -and $currentCtxt -ne $null) {
            
            # Clean HTML tags
            $CleanEng = $currentId -replace '<[^>]+>', ''
            $CleanCht = $currentStr -replace '<[^>]+>', ''
            
            # Unescape quotes \" -> "
            $CleanEng = $CleanEng -replace '\\"', '"'
            $CleanCht = $CleanCht -replace '\\"', '"'
            
            if (-not [string]::IsNullOrWhiteSpace($CleanCht) -and ($CleanEng -ne $CleanCht)) {
                if (-not $SeenNames.ContainsKey($CleanEng)) {
                    $SeenNames[$CleanEng] = $true
                    
                    # Refined category logic
                    $Parts = $currentCtxt -split '\.'
                    if ($Parts.Count -ge 3) {
                        $MainCat = $Parts[1]
                        if ($SingleLevelExceptions -contains $MainCat) {
                            $Category = $MainCat
                        } else {
                            $Category = "$($Parts[1]).$($Parts[2])"
                        }
                    } else {
                        $Category = $Parts[1]
                    }

                    $Results.Add([PSCustomObject]@{
                        Category = $Category
                        English  = $CleanEng
                        Chinese  = $CleanCht
                        Path     = $currentCtxt
                    })
                }
            }
            # Reset
            $currentCtxt = $null
            $state = "NONE"
        }
    }
}

Write-Host "Successfully extracted $($Results.Count) terms."

# Export to TSV
$Results | Export-Csv -Path $OutputFile -Delimiter "`t" -NoTypeInformation -Encoding UTF8

Write-Host "Glossary saved to: $OutputFile"
