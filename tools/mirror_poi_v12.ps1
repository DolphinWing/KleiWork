param(
    [Parameter(Mandatory=$false)]
    [string]$InputFile,
    [Parameter(Mandatory=$false)]
    [string]$OutputFile
)

if (-not $InputFile) {
    Write-Host "`nUsage: powershell.exe -File tools/mirror_poi_v12.ps1 -InputFile <path>" -ForegroundColor Yellow
    exit
}

if (-not $OutputFile) { $OutputFile = $InputFile -replace "\.yaml$", "_Mirrored.yaml" }

Write-Host "`n--- ONI Template Mirror Tool (V12) ---" -ForegroundColor Cyan
Write-Host "Input: $InputFile"
Write-Host "Output: $OutputFile`n"

# 建立寬度數據庫 (用於 X 座標補償)
$WidthDB = @{
    "ManualGenerator"=2; "ResearchCenter"=2; "Bed"=2; "WashBasin"=2; "Outhouse"=2; 
    "IceCooledFan"=2; "VendingMachine"=2; "Battery"=1; "BatteryMedium"=2; "BatterySmart"=2;
    "PowerTransformer"=3; "PowerTransformerSmall"=2; "Electrolyzer"=2; "LiquidPump"=2; "GasPump"=2;
    "Generator"=3; "MassageTable"=3;
}

$absPath = Resolve-Path $InputFile
$content = [System.IO.File]::ReadAllText($absPath)
$sections = $content -split "(?m)^- "
$header = $sections[0]
$mirroredSections = @()
$stats = @{ "Cells"=0; "Buildings"=0; "Wires"=0 }

# 1. 處理 Header
$oldMinX = 0; $oldSizeX = 0
if ($header -match "min:\s*X:\s*(-?\d+)") { $oldMinX = [int]$matches[1] }
if ($header -match "size:\s*X:\s*(\d+)") { $oldSizeX = [int]$matches[1] }
$newMinX = 2 - $oldMinX - $oldSizeX
$newHeader = $header -replace "(?m)(^\s*X:\s*)-?\d+(\s*#?\s*min:)", "`${1}$newMinX`${2}"

Write-Host "[Header] MinX: $oldMinX -> $newMinX (SizeX: $oldSizeX)" -ForegroundColor Gray

for ($i = 1; $i -lt $sections.Count; $i++) {
    $section = $sections[$i]
    $id = ""; if ($section -match "id:\s*(\w+)") { $id = $matches[1] }
    $rot = "R0"; if ($section -match "rotationOrientation:\s*(\w+)") { $rot = $matches[1] }
    
    # 決定寬度
    $width = 1
    if ($WidthDB.ContainsKey($id)) { $width = $WidthDB[$id] }
    if ($id -match "Door" -and $rot -eq "R90") { $width = 2 }
    if ($id -match "Bridge") {
        if ($rot -eq "R0" -or $rot -eq "R180") { $width = 3 } else { $width = 1 }
    }

    $currentX = 0
    if ($section -match "location_x:\s*(-?\d+)") { $currentX = [int]$matches[1] }
    
    # 公式: X_new = 2 - X_old - Width
    $newX = 2 - $currentX - $width
    
    if ($id) {
        Write-Host "  [Building] $id | Width: $width | X: $currentX -> $newX" -ForegroundColor DarkCyan
        $stats["Buildings"]++
    } else {
        $stats["Cells"]++
    }

    $lines = $section -split "`r?`n"
    $newLines = @()
    $xFound = $false
    
    foreach ($line in $lines) {
        if ($line -match "location_x:\s*(-?\d+)") {
            if ($newX -ne 0) {
                $newLines += $line -replace "location_x:\s*-?\d+", "location_x: $newX"
            }
            $xFound = $true
        }
        elseif ($line -match "connections:\s*(\d+)") {
            $conn = [int]$matches[1]
            # ONI Bitmask: 1:Left, 2:Right, 4:Up, 8:Down. Mirror 1 <-> 2
            $newConn = $conn -band 12
            if (($conn -band 1) -eq 1) { $newConn = $newConn -bor 2 }
            if (($conn -band 2) -eq 2) { $newConn = $newConn -bor 1 }
            $newLines += $line -replace "connections:\s*\d+", "connections: $newConn"
            
            if ($conn -ne $newConn) {
                Write-Host "    -> Wire Bitmask Flip: $conn -> $newConn (L<->R)" -ForegroundColor DarkGray
            }
            $stats["Wires"]++
        }
        else {
            if ($line.Length -gt 0 -or $line.Trim().Length -gt 0) { $newLines += $line }
        }
    }
    
    if (-not $xFound -and $newX -ne 0) {
        $inserted = $false
        for ($j=0; $j -lt $newLines.Count; $j++) {
            if ($newLines[$j] -match "location_y:") {
                $indent = "  "; if ($newLines[$j] -match "^(\s+)") { $indent = $matches[1] }
                $tempLines = @(); for ($k=0; $k -lt $j; $k++) { $tempLines += $newLines[$k] }
                $tempLines += "${indent}location_x: $newX"
                for ($k=$j; $k -lt $newLines.Count; $k++) { $tempLines += $newLines[$k] }
                $newLines = $tempLines; $inserted = $true; break
            }
        }
        if (-not $inserted) { $newLines += "  location_x: $newX" }
    }
    $mirroredSections += ($newLines -join "`r`n")
}

$finalOutput = $newHeader + "- " + ($mirroredSections -join "`r`n- ")
$utf8NoBom = New-Object System.Text.UTF8Encoding($false)
$finalAbsPath = [System.IO.Path]::Combine((Get-Location).Path, $outputFile)
[System.IO.File]::WriteAllText($finalAbsPath, $finalOutput, $utf8NoBom)

Write-Host "`n--- Summary ---" -ForegroundColor Green
Write-Host "Processed $($stats['Cells']) Cells."
Write-Host "Processed $($stats['Buildings']) Buildings."
Write-Host "Flipped $($stats['Wires']) Wire connections."
Write-Host "`nMirror Success: $OutputFile" -ForegroundColor Green
