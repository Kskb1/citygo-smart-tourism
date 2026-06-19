$ErrorActionPreference = "Stop"

$Root = Split-Path -Parent $MyInvocation.MyCommand.Path
$FrontendDir = Join-Path $Root "frontend"
$EnvFile = Join-Path $Root ".env.local"

if (Test-Path $EnvFile) {
    Get-Content -Encoding UTF8 $EnvFile | ForEach-Object {
        $Line = $_.Trim()
        if ($Line.Length -gt 0 -and -not $Line.StartsWith("#")) {
            $Parts = $Line -split "=", 2
            if ($Parts.Count -eq 2 -and $Parts[0].Trim() -eq "VITE_AMAP_JS_API_KEY") {
                $Value = $Parts[1].Trim()
                if (($Value.StartsWith('"') -and $Value.EndsWith('"')) -or ($Value.StartsWith("'") -and $Value.EndsWith("'"))) {
                    $Value = $Value.Substring(1, $Value.Length - 2)
                }
                [Environment]::SetEnvironmentVariable("VITE_AMAP_JS_API_KEY", $Value, "Process")
                if ([string]::IsNullOrWhiteSpace($Value)) {
                    Write-Host "[CityGo] VITE_AMAP_JS_API_KEY: 未配置，将使用路线示意图。"
                } else {
                    Write-Host "[CityGo] VITE_AMAP_JS_API_KEY: 已配置"
                }
            }
        }
    }
}

Set-Location $FrontendDir

if (-not (Test-Path (Join-Path $FrontendDir "node_modules"))) {
    Write-Host "[CityGo] 未找到 node_modules，正在执行 npm install..."
    npm install
}

Write-Host "[CityGo] 正在启动 Vite 前端..."
npm run dev
