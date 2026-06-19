$ErrorActionPreference = "Stop"

$Root = Split-Path -Parent $MyInvocation.MyCommand.Path
$EnvFile = Join-Path $Root ".env.local"

$AllowedKeys = @(
    "AMAP_API_KEY",
    "AMADEUS_CLIENT_ID",
    "AMADEUS_CLIENT_SECRET",
    "AMADEUS_ENV",
    "TRAIN_PROVIDER",
    "TRAIN_API_KEY",
    "MYSQL_URL",
    "MYSQL_USER",
    "MYSQL_PASSWORD",
    "SPRING_PROFILES_ACTIVE",
    "SPRING_DATASOURCE_URL",
    "SPRING_DATASOURCE_USERNAME",
    "SPRING_DATASOURCE_PASSWORD",
    "JWT_SECRET",
    "JWT_TTL_HOURS",
    "CITYGO_ADMIN_USERNAME",
    "CITYGO_ADMIN_PASSWORD",
    "CITYGO_ADMIN_EMAIL",
    "CITYGO_DEMO_USERS_ENABLED"
)

function Set-CityGoEnvValue {
    param (
        [string] $Key,
        [string] $Value
    )

    if ($AllowedKeys -contains $Key) {
        [Environment]::SetEnvironmentVariable($Key, $Value, "Process")
    }
}

function Show-ConfiguredStatus {
    param (
        [string] $Key
    )

    $Value = [Environment]::GetEnvironmentVariable($Key, "Process")

    if ([string]::IsNullOrWhiteSpace($Value)) {
        Write-Host ("[CityGo] {0}: not configured" -f $Key)
    } else {
        Write-Host ("[CityGo] {0}: configured" -f $Key)
    }
}

Write-Host ("[CityGo] Project root: {0}" -f $Root)

if (Test-Path $EnvFile) {
    Get-Content -Encoding UTF8 $EnvFile | ForEach-Object {
        $Line = $_.Trim()

        if ($Line.Length -gt 0 -and -not $Line.StartsWith("#")) {
            $Parts = $Line -split "=", 2

            if ($Parts.Count -eq 2) {
                $Key = $Parts[0].Trim()
                $Value = $Parts[1].Trim()

                if (
                    ($Value.StartsWith('"') -and $Value.EndsWith('"')) -or
                    ($Value.StartsWith("'") -and $Value.EndsWith("'"))
                ) {
                    $Value = $Value.Substring(1, $Value.Length - 2)
                }

                Set-CityGoEnvValue -Key $Key -Value $Value
            }
        }
    }

    Write-Host "[CityGo] .env.local loaded"
} else {
    Write-Host "[CityGo] .env.local not found. Backend will start without API keys."
}

$AmadeusEnv = [Environment]::GetEnvironmentVariable("AMADEUS_ENV", "Process")
if ([string]::IsNullOrWhiteSpace($AmadeusEnv)) {
    $AmadeusEnv = "test"
    [Environment]::SetEnvironmentVariable("AMADEUS_ENV", $AmadeusEnv, "Process")
}

$TrainProvider = [Environment]::GetEnvironmentVariable("TRAIN_PROVIDER", "Process")
if ([string]::IsNullOrWhiteSpace($TrainProvider)) {
    $TrainProvider = "official_redirect"
    [Environment]::SetEnvironmentVariable("TRAIN_PROVIDER", $TrainProvider, "Process")
}

Show-ConfiguredStatus "AMAP_API_KEY"
Show-ConfiguredStatus "AMADEUS_CLIENT_ID"
Show-ConfiguredStatus "AMADEUS_CLIENT_SECRET"
Show-ConfiguredStatus "TRAIN_API_KEY"
Show-ConfiguredStatus "MYSQL_URL"
Show-ConfiguredStatus "SPRING_DATASOURCE_URL"
Show-ConfiguredStatus "JWT_SECRET"
Show-ConfiguredStatus "CITYGO_ADMIN_PASSWORD"

Write-Host ("[CityGo] AMADEUS_ENV: {0}" -f $AmadeusEnv)
Write-Host ("[CityGo] TRAIN_PROVIDER: {0}" -f $TrainProvider)
Write-Host "[CityGo] Starting Spring Boot backend..."

Set-Location (Join-Path $Root "backend")

.\mvnw.cmd spring-boot:run
