param(
    [string]$BaseUrl = "http://localhost:8080",

    [Parameter(Mandatory = $true)]
    [string]$IdToken,

    [string]$AppCheckToken,

    [switch]$RequireAppCheck,

    [string]$ReceiverId = "debug_receiver_uid",

    [switch]$SkipAi,

    [switch]$SkipCloudinary
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

function Invoke-Api {
    param(
        [Parameter(Mandatory = $true)][string]$Method,
        [Parameter(Mandatory = $true)][string]$Path,
        [object]$Body = $null,
        [int[]]$AllowedStatus = @(200),
        [switch]$UseAuth
    )

    $uri = "$BaseUrl$Path"
    $headers = @{}

    if ($UseAuth) {
        $headers["Authorization"] = "Bearer $IdToken"
        if ($RequireAppCheck) {
            if (-not $AppCheckToken) {
                throw "RequireAppCheck is on but AppCheckToken is empty."
            }
            $headers["X-Firebase-AppCheck"] = $AppCheckToken
        }
    }

    $requestBody = $null
    if ($null -ne $Body) {
        $requestBody = $Body | ConvertTo-Json -Depth 20
    }

    try {
        $invokeArgs = @{
            Method = $Method
            Uri = $uri
            Headers = $headers
        }

        # Windows PowerShell 5.x shows IE parsing warning without this flag.
        if ((Get-Command Invoke-WebRequest).Parameters.ContainsKey("UseBasicParsing")) {
            $invokeArgs["UseBasicParsing"] = $true
        }

        if ($null -ne $requestBody) {
            $invokeArgs["ContentType"] = "application/json"
            $invokeArgs["Body"] = $requestBody
        }

        $resp = Invoke-WebRequest @invokeArgs
        $status = [int]$resp.StatusCode
        $content = if ($resp.Content) { $resp.Content } else { "" }
    } catch {
        if (-not $_.Exception.Response) {
            throw
        }
        $status = [int]$_.Exception.Response.StatusCode.value__
        $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
        $content = $reader.ReadToEnd()
    }

    if ($AllowedStatus -notcontains $status) {
        throw "[$Method $Path] Expected $($AllowedStatus -join ',') but got $status. Body: $content"
    }

    if ([string]::IsNullOrWhiteSpace($content)) {
        return $null
    }

    try {
        return ($content | ConvertFrom-Json)
    } catch {
        return $content
    }
}

Write-Host ""
Write-Host "1) Unauthorized check (should be 401)..."
[void](Invoke-Api -Method GET -Path "/api/secure/ping" -AllowedStatus @(401))
Write-Host "   OK"

Write-Host "2) Authorized ping..."
$ping = Invoke-Api -Method GET -Path "/api/secure/ping" -UseAuth -AllowedStatus @(200)
Write-Host ("   OK - uid=" + $ping.uid)

Write-Host "3) Diary save/list/delete..."
$savePayload = @{
    title = "Smoke test"
    text = "Backend smoke test at $(Get-Date -Format o)"
    moodTag = "Neutral"
    imageUrls = @()
    audioUrl = $null
}
$saveResp = Invoke-Api -Method POST -Path "/api/secure/diaries/save" -UseAuth -Body $savePayload -AllowedStatus @(200)
$diaryId = $saveResp.diaryId
if (-not $diaryId) { throw "save diary did not return diaryId" }
Write-Host ("   Saved diaryId=" + $diaryId)

$listResp = Invoke-Api -Method GET -Path "/api/secure/diaries/me" -UseAuth -AllowedStatus @(200)
$foundDiary = $false
foreach ($d in $listResp.diaries) {
    if ($d.diaryId -eq $diaryId) { $foundDiary = $true; break }
}
if (-not $foundDiary) { throw "Saved diaryId not found in list." }
Write-Host "   Listed OK"

[void](Invoke-Api -Method DELETE -Path "/api/secure/diaries/$diaryId" -UseAuth -AllowedStatus @(200))
Write-Host "   Deleted OK"

Write-Host "4) Chat send/list/delete..."
$sendPayload = @{
    receiverId = $ReceiverId
    messageText = "Hello from backend smoke test"
    imageUrl = $null
}
$sendResp = Invoke-Api -Method POST -Path "/api/secure/chats/send" -UseAuth -Body $sendPayload -AllowedStatus @(200)
if (-not $sendResp.conversationId) { throw "send chat did not return conversationId" }
$conversationId = $sendResp.conversationId
Write-Host ("   Sent messageId=" + $sendResp.messageId)

$encodedReceiverId = [uri]::EscapeDataString($ReceiverId)
$conversationPath = "/api/secure/chats/conversation/{0}?limit=20" -f $encodedReceiverId
$convResp = Invoke-Api -Method GET -Path $conversationPath -UseAuth -AllowedStatus @(200)
if ($convResp.conversationId -ne $conversationId) { throw "conversationId mismatch." }
Write-Host "   Listed conversation OK"

$inboxResp = Invoke-Api -Method GET -Path "/api/secure/chats/inbox?limit=20" -UseAuth -AllowedStatus @(200)
$inboxFound = $false
foreach ($m in $inboxResp.messages) {
    if ($m.id -eq $sendResp.messageId) { $inboxFound = $true; break }
}
if (-not $inboxFound) { throw "Latest message not found in inbox endpoint." }
Write-Host "   Inbox OK"

$deleteConversationPath = "/api/secure/chats/conversation/{0}" -f $encodedReceiverId
[void](Invoke-Api -Method DELETE -Path $deleteConversationPath -UseAuth -AllowedStatus @(200))
Write-Host "   Deleted conversation OK"

if (-not $SkipCloudinary) {
    Write-Host "5) Cloudinary sign-upload..."
    $cloudinaryResp = Invoke-Api -Method POST -Path "/api/secure/cloudinary/sign-upload" -UseAuth -Body (@{ publicId = "smoke_test_file"; context = "source=smoke" }) -AllowedStatus @(200)
    if (-not $cloudinaryResp.signature) { throw "Missing cloudinary signature." }
    Write-Host "   OK"
} else {
    Write-Host "5) Cloudinary test skipped"
}

if (-not $SkipAi) {
    Write-Host "6) AI predict mood..."
    $aiResp = Invoke-Api -Method POST -Path "/api/secure/ai/predict-mood" -UseAuth -Body (@{ text = "Today I am very happy and productive." }) -AllowedStatus @(200)
    if (-not $aiResp.mood) { throw "Missing mood response." }
    Write-Host ("   OK - mood=" + $aiResp.mood)
} else {
    Write-Host "6) AI test skipped"
}

Write-Host ""
Write-Host "Smoke test completed successfully."
Write-Host ""
