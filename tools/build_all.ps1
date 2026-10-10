param(
    [string]$ConfluxCheckout,
    [string]$NeoJavaHome = $env:JAVA_HOME,
    [string]$ForgeJavaHome = $env:JAVA_HOME,
    [switch]$RuntimeChecks
)

$ErrorActionPreference = 'Stop'
$taskRoot = Split-Path -Parent $PSScriptRoot
function Invoke-TargetBuild([string]$directory, [string]$java, [string]$core, [string[]]$tasks) {
    $previousJava = $env:JAVA_HOME
    Push-Location -LiteralPath $directory
    try {
        if ($java) { $env:JAVA_HOME = $java }
        $arguments = @($tasks) + '--console=plain'
        if ($core) { $arguments += "-PconfluxCheckout=$core" }
        # Windows PowerShell treats javac's stderr warnings as ErrorRecords.
        # Judge native builds by their exit code, not by the presence of warnings.
        $previousErrorAction = $ErrorActionPreference
        try {
            $ErrorActionPreference = 'Continue'
            & .\gradlew.bat @arguments 2>&1 | ForEach-Object { $_.ToString() }
            $buildExit = $LASTEXITCODE
        } finally { $ErrorActionPreference = $previousErrorAction }
        if ($buildExit -ne 0) { throw "Build failed in $directory (exit $buildExit)" }
    } finally {
        $env:JAVA_HOME = $previousJava
        Pop-Location
    }
}

$neoCore = if ($ConfluxCheckout) { (Resolve-Path -LiteralPath $ConfluxCheckout).Path } else { $null }
$forgeCore = if ($neoCore) { Join-Path $neoCore 'forge' } else { $null }
$neoTasks = @(':test', ':jar', ':sourcesJar', ':compileGameTestJava', ':ars-conflux:jar')
$forgeTasks = @(':test', ':jarJar', ':sourcesJar', ':compileForgeTestJava', ':ars-conflux:jarJar')
if ($RuntimeChecks) {
    $checks = @(':runGameTestServer', '-PgameTestNamespaces=irons_nouveau_shared,irons_nouveau_context')
    $neoTasks += $checks
    $forgeTasks += $checks
}
Invoke-TargetBuild $taskRoot $NeoJavaHome $neoCore $neoTasks
Invoke-TargetBuild (Join-Path $taskRoot 'forge') $ForgeJavaHome $forgeCore $forgeTasks
& python (Join-Path $PSScriptRoot 'check_shared_layout.py') --generated
if ($LASTEXITCODE -ne 0) { throw 'Shared layout verification failed' }
