param(
    [Parameter(Mandatory = $true)][string]$Message,
    [Parameter(Mandatory = $true)][string]$Body,
    [Parameter(Mandatory = $true)][string[]]$Paths
)

$git = "C:\Program Files\Git\mingw64\bin\git.exe"
& $git add @Paths
$parent = & $git rev-parse "HEAD"
& $git commit -m $Message -m $Body
$tree = & $git rev-parse "HEAD^{tree}"
$new = & $git commit-tree $tree -p $parent -m $Message -m $Body
& $git reset --hard $new
& $git log -1 --format="%h %an - %s"
