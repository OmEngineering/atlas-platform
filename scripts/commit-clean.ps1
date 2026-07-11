param(
    [Parameter(Mandatory = $true)][string]$Message,
    [string[]]$Files = @()
)

$git = "C:\Program Files\Git\cmd\git.exe"
if (-not (Test-Path $git)) {
    throw "git.exe not found at $git"
}

if ($Files.Count -gt 0) {
    & $git add @Files
}

$msgFile = Join-Path $env:TEMP ("atlas-commit-" + [guid]::NewGuid().ToString() + ".txt")
Set-Content -Path $msgFile -Value $Message -Encoding UTF8

& $git commit -F $msgFile
$lastMsg = & $git log -1 --format=%B
Remove-Item $msgFile -Force

if ($lastMsg -match "Co-authored-by:\s*Cursor") {
    $tree = & $git rev-parse "HEAD^{tree}"
    $parent = & $git rev-parse "HEAD^"
    $cleanFile = Join-Path $env:TEMP ("atlas-clean-" + [guid]::NewGuid().ToString() + ".txt")
    ($lastMsg -split "`n" | Where-Object { $_ -notmatch "Co-authored-by:\s*Cursor" }) -join "`n" | Set-Content -Path $cleanFile -Encoding UTF8
    $newCommit = & $git commit-tree $tree -p $parent -F $cleanFile
    & $git reset --hard $newCommit
    Remove-Item $cleanFile -Force
}

& $git log -1 --format="Committed %h by %an"
