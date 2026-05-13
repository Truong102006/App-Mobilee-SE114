param(
    [Parameter(Mandatory = $true)]
    [string]$Email,

    [Parameter(Mandatory = $true)]
    [string]$Password,

    [string]$ApiKey,

    [string]$GoogleServicesPath = "..\\app\\google-services.json",

    [switch]$RawTokenOnly
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

function Resolve-ApiKey {
    param(
        [string]$ProvidedApiKey,
        [string]$JsonPath
    )

    if ($ProvidedApiKey) {
        return $ProvidedApiKey
    }

    $resolved = Resolve-Path -LiteralPath $JsonPath -ErrorAction Stop
    $raw = Get-Content -LiteralPath $resolved -Raw -Encoding UTF8
    $json = $raw | ConvertFrom-Json
    $key = $json.client[0].api_key[0].current_key
    if (-not $key) {
        throw "Cannot find api_key.current_key in google-services.json."
    }
    return [string]$key
}

$actualApiKey = Resolve-ApiKey -ProvidedApiKey $ApiKey -JsonPath $GoogleServicesPath
$uri = "https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=$actualApiKey"
$body = @{
    email = $Email
    password = $Password
    returnSecureToken = $true
} | ConvertTo-Json

try {
    $response = Invoke-RestMethod -Method Post -Uri $uri -ContentType "application/json" -Body $body
} catch {
    throw "Failed to get ID token. Check email/password/apiKey. Details: $($_.Exception.Message)"
}

if (-not $response.idToken) {
    throw "Firebase did not return idToken."
}

if ($RawTokenOnly) {
    Write-Output $response.idToken
    return
}

Write-Host ""
Write-Host "UID: $($response.localId)"
Write-Host "ID_TOKEN:"
Write-Host $response.idToken
Write-Host ""
