# 把本仓库推送到 GitHub（走 REST API，本机 github.com 的 git 协议不通）
#   $env:GH_TOKEN = "ghp_xxxxxxxx"
#   powershell -NoProfile -ExecutionPolicy Bypass -File tools\push_to_github.ps1

param(
    [string]$Owner = "ybk-992255",
    [string]$Repo = "Create-fueling-imagination",
    [string]$Branch = "main"
)

$ErrorActionPreference = "Stop"
$ProgressPreference = "SilentlyContinue"

$token = $env:GH_TOKEN
if ([string]::IsNullOrWhiteSpace($token)) {
    Write-Host "缺少令牌：先设置 `$env:GH_TOKEN"
    exit 1
}

$repoRoot = Split-Path -Parent $PSScriptRoot
$headers = @{
    "Authorization"        = "Bearer $token"
    "Accept"               = "application/vnd.github+json"
    "User-Agent"           = "fueling-imagination-pusher"
    "X-GitHub-Api-Version" = "2022-11-28"
}
$api = "https://api.github.com/repos/$Owner/$Repo"

$git = Join-Path (Split-Path -Parent $repoRoot) "_toolchain\git\cmd\git.exe"
if (-not (Test-Path $git)) { $git = "git" }
Push-Location $repoRoot
$files = @(& $git ls-files)
$version = (Select-String -Path (Join-Path $repoRoot "gradle.properties") -Pattern "^mod_version=(.+)$").Matches[0].Groups[1].Value
$commitMessage = "Update source to $version"
Pop-Location

Write-Host "文件数: $($files.Count)"
Write-Host "提交信息: $commitMessage"

$entries = New-Object System.Collections.ArrayList
$i = 0
foreach ($file in $files) {
    $i++
    if (($i % 50) -eq 0) { Write-Host "  已上传 $i / $($files.Count)" }
    $path = Join-Path $repoRoot $file
    if (-not (Test-Path $path)) { continue }
    $bytes = [System.IO.File]::ReadAllBytes($path)
    $payload = ConvertTo-Json -Compress -InputObject @{ content = [Convert]::ToBase64String($bytes); encoding = "base64" }
    $blob = Invoke-RestMethod -Uri "$api/git/blobs" -Method Post -Headers $headers -Body ([System.Text.Encoding]::UTF8.GetBytes($payload)) -ContentType "application/json"
    $null = $entries.Add(@{ path = ($file -replace "\\", "/"); mode = "100644"; type = "blob"; sha = $blob.sha })
}
Write-Host "blob 完成: $($entries.Count) 个"
if ($entries.Count -eq 0) { Write-Host "没有可提交的文件"; exit 1 }

$treePayload = ConvertTo-Json -Compress -Depth 6 -InputObject @{ tree = $entries.ToArray() }
$tree = Invoke-RestMethod -Uri "$api/git/trees" -Method Post -Headers $headers -Body ([System.Text.Encoding]::UTF8.GetBytes($treePayload)) -ContentType "application/json"
Write-Host "tree: $($tree.sha)"

$parentSha = $null
try {
    $head = Invoke-RestMethod -Uri "$api/git/ref/heads/$Branch" -Headers $headers
    $parentSha = $head.object.sha
} catch {
    Write-Host "远端分支不存在，将创建首个提交"
}

$commitBody = @{ message = $commitMessage; tree = $tree.sha }
if ($parentSha) { $commitBody["parents"] = @($parentSha) }
$commitPayload = ConvertTo-Json -Compress -Depth 4 -InputObject $commitBody
$commit = Invoke-RestMethod -Uri "$api/git/commits" -Method Post -Headers $headers -Body ([System.Text.Encoding]::UTF8.GetBytes($commitPayload)) -ContentType "application/json"
Write-Host "commit: $($commit.sha)"

if ($parentSha) {
    $refPayload = ConvertTo-Json -Compress -InputObject @{ sha = $commit.sha; force = $true }
    $null = Invoke-RestMethod -Uri "$api/git/refs/heads/$Branch" -Method Patch -Headers $headers -Body ([System.Text.Encoding]::UTF8.GetBytes($refPayload)) -ContentType "application/json"
} else {
    $refPayload = ConvertTo-Json -Compress -InputObject @{ ref = "refs/heads/$Branch"; sha = $commit.sha }
    $null = Invoke-RestMethod -Uri "$api/git/refs" -Method Post -Headers $headers -Body ([System.Text.Encoding]::UTF8.GetBytes($refPayload)) -ContentType "application/json"
}
Write-Host ""
Write-Host "推送完成: https://github.com/$Owner/$Repo"
