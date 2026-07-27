param(
    [string]$ProjectRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
)

Add-Type -AssemblyName System.Drawing

$textureDirectory = Join-Path $ProjectRoot "src/main/resources/assets/teenycraft/textures/item"
$gold = [System.Drawing.Color]::FromArgb(255, 244, 184, 42)

function Find-Nearest-AlphaDistance {
    param(
        [System.Drawing.Bitmap]$Bitmap,
        [int]$X,
        [int]$Y,
        [int]$Radius,
        [bool]$FindOpaque
    )

    $nearest = [double]::PositiveInfinity
    for ($offsetY = -$Radius; $offsetY -le $Radius; $offsetY++) {
        for ($offsetX = -$Radius; $offsetX -le $Radius; $offsetX++) {
            if ($offsetX -eq 0 -and $offsetY -eq 0) {
                continue
            }

            $sampleX = $X + $offsetX
            $sampleY = $Y + $offsetY
            if ($sampleX -lt 0 -or $sampleX -ge $Bitmap.Width -or $sampleY -lt 0 -or $sampleY -ge $Bitmap.Height) {
                if (-not $FindOpaque) {
                    $distance = [Math]::Sqrt($offsetX * $offsetX + $offsetY * $offsetY)
                    $nearest = [Math]::Min($nearest, $distance)
                }
                continue
            }

            $opaque = $Bitmap.GetPixel($sampleX, $sampleY).A -ge 24
            if ($opaque -eq $FindOpaque) {
                $distance = [Math]::Sqrt($offsetX * $offsetX + $offsetY * $offsetY)
                $nearest = [Math]::Min($nearest, $distance)
            }
        }
    }
    return $nearest
}

function New-GoldenTexture {
    param(
        [string]$SourceName,
        [string]$OutputName
    )

    $sourcePath = Join-Path $textureDirectory $SourceName
    $outputPath = Join-Path $textureDirectory $OutputName
    $source = [System.Drawing.Bitmap]::new($sourcePath)
    $output = [System.Drawing.Bitmap]::new($source.Width, $source.Height, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)

    try {
        for ($y = 0; $y -lt $source.Height; $y++) {
            for ($x = 0; $x -lt $source.Width; $x++) {
                $pixel = $source.GetPixel($x, $y)
                $result = $pixel

                if ($pixel.A -eq 0) {
                    $distance = Find-Nearest-AlphaDistance $source $x $y 5 $true
                    if ($distance -le 5.0) {
                        $strength = [Math]::Pow(1.0 - (($distance - 1.0) / 5.0), 1.6)
                        $alpha = [Math]::Min(105, [Math]::Max(8, [int][Math]::Round(105 * $strength)))
                        $result = [System.Drawing.Color]::FromArgb($alpha, $gold.R, $gold.G, $gold.B)
                    }
                }

                $output.SetPixel($x, $y, $result)
            }
        }

        $output.Save($outputPath, [System.Drawing.Imaging.ImageFormat]::Png)
        Write-Output "Generated $OutputName from $SourceName (balanced halo)"
    }
    finally {
        $output.Dispose()
        $source.Dispose()
    }
}

New-GoldenTexture "ability_amazonian_beatdown.png" "display_test_gold_halo_amazonian.png"
New-GoldenTexture "ability_birdarang.png" "display_test_gold_halo_birdarang.png"
