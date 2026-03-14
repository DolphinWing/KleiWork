param(
    [switch]$Fix
)

function Parse-PropertiesFile {
    param(
        [string]$FilePath
    )

    $properties = [System.Collections.Specialized.OrderedDictionary]::new()
    try {
        Get-Content -Path $FilePath -Encoding UTF8 | ForEach-Object {
            $line = $_.Trim()
            if (-not [string]::IsNullOrEmpty($line) -and -not $line.StartsWith('#') -and -not $line.StartsWith('!')) {
                if ($line.Contains('=')) {
                    $key, $value = $line.Split('=', 2)
                } elseif ($line.Contains(':')) {
                    $key, $value = $line.Split(':', 2)
                } else {
                    continue
                }
                $properties[$key.Trim()] = $value.Trim()
            }
        }
    } catch [System.IO.FileNotFoundException] {
        Write-Warning "File not found at $FilePath"
    }
    return $properties
}

$resourceDir = 'src/main/resources'
$baseFileName = 'strings.properties'
$baseFilePath = Join-Path (Get-Location) $resourceDir $baseFileName

if (-not (Test-Path $baseFilePath)) {
    Write-Error "Base file '$baseFilePath' not found."
    exit 1
}

Write-Host "Loading base properties from: $baseFilePath"
$baseProperties = Parse-PropertiesFile -FilePath $baseFilePath
Write-Host "Found $($baseProperties.Count) keys in $baseFileName"

$languageFiles = Get-ChildItem -Path (Join-Path (Get-Location) $resourceDir) -Filter 'strings_*.properties' -File

if (-not $languageFiles) {
    Write-Host "No other language-specific .properties files found to compare."
    exit 0
}

foreach ($langFile in $languageFiles) {
    $langFilePath = $langFile.FullName
    $langFileName = $langFile.Name
    Write-Host "`n--- Comparing $langFileName ---"
    $langProperties = Parse-PropertiesFile -FilePath $langFilePath

    $baseKeys = $baseProperties.Keys | Sort-Object
    $langKeys = $langProperties.Keys | Sort-Object

    $missingInLang = @()
    foreach ($key in $baseKeys) {
        if (-not $langProperties.ContainsKey($key)) {
            $missingInLang += $key
        }
    }

    $extraInLang = @()
    foreach ($key in $langKeys) {
        if (-not $baseProperties.ContainsKey($key)) {
            $extraInLang += $key
        }
    }

    if ($missingInLang.Count -gt 0) {
        Write-Host "Keys missing in $langFileName:"
        if ($Fix) {
            Add-Content -Path $langFilePath -Value "`n# Added by localization key checker script"
            foreach ($key in $missingInLang) {
                $value = $baseProperties[$key]
                $lineToAdd = "`n$key=$value"
                Add-Content -Path $langFilePath -Value $lineToAdd
                Write-Host "  + Added '$key=$value'"
            }
            Write-Host "  Automatically added $($missingInLang.Count) missing keys to $langFileName."
        } else {
            foreach ($key in $missingInLang) {
                Write-Host "  - $key"
            }
            Write-Host "  To fix, run the script with '-Fix' argument: pwsh -File check_localization_keys.ps1 -Fix"
        }
    } else {
        Write-Host "No missing keys in $langFileName."
    }

    if ($extraInLang.Count -gt 0) {
        Write-Host "Extra keys found in $langFileName:"
        foreach ($key in $extraInLang) {
            Write-Host "  - $key (Consider removing if not needed)"
        }
    } else {
        Write-Host "No extra keys in $langFileName."
    }
}

Write-Host "`n--- Comparison complete ---"
