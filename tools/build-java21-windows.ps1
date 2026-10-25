param(
 [string]$JavaHome = $env:JAVA_HOME,
 [string]$FxVersion = "23.0.2",
 [string]$MavenRepo = "$env:USERPROFILE\.m2\repository",
 [string]$CoreJar = "",
 [string]$CoreSha256 = "",
 [string]$Out = ""
)
$ErrorActionPreference = "Stop"
$root = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
if (-not $Out) { $Out = Join-Path $root "qa-java21-windows" }
if (-not $CoreJar -or -not $CoreSha256) { throw "CoreJar and CoreSha256 are required" }
$javac = Join-Path $JavaHome "bin\javac.exe"
$jar = Join-Path $JavaHome "bin\jar.exe"
$jlink = Join-Path $JavaHome "bin\jlink.exe"
if (-not (Test-Path $javac)) { throw "JDK21 Windows faltante" }
if (-not (Test-Path $CoreJar)) { throw "Core POS jar faltante: $CoreJar" }
if ($CoreJar -match "[\\/]target[\\/]") { throw "Refusing mutable target core" }
$actual = (Get-FileHash -Algorithm SHA256 $CoreJar).Hash.ToLowerInvariant()
if ($actual -ne $CoreSha256.ToLowerInvariant()) { throw "CORE_JAR_SHA256_MISMATCH expected=$CoreSha256 actual=$actual" }
$fx = @()
foreach ($m in @("base","graphics","controls","fxml")) {
 $f = Join-Path $MavenRepo ("org\openjfx\javafx-" + $m + "\" + $FxVersion + "\javafx-" + $m + "-" + $FxVersion + "-win.jar")
 if (-not (Test-Path $f)) { throw "JavaFX Windows faltante: $f" }
 $fx += $f
}
$lib = @(Get-ChildItem (Join-Path $root "lib") -Filter "*.jar" | ForEach-Object { $_.FullName })
$classes = Join-Path $Out "classes"
$dist = Join-Path $Out "dist"
New-Item -Force -ItemType Directory $classes, (Join-Path $dist "lib"), (Join-Path $dist "fx") | Out-Null
$cp = (($fx + $lib + @($CoreJar)) -join ";")
$src = @(Get-ChildItem (Join-Path $root "src"),(Join-Path $root "src-runtime") -Filter "*.java" -Recurse | ForEach-Object FullName)
$argsFile = Join-Path $Out "sources.txt"
$src | ForEach-Object { '"' + $_ + '"' } | Set-Content -Encoding ascii $argsFile
& $javac --release 21 -encoding UTF-8 -cp $cp -d $classes "@$argsFile"
if ($LASTEXITCODE -ne 0) { throw "Compilación Java21 fallo" }
Get-ChildItem (Join-Path $root "src") -File -Recurse | Where-Object { $_.Extension -ne ".java" } | ForEach-Object {
 $rel = $_.FullName.Substring((Join-Path $root "src").Length + 1)
 $target = Join-Path $classes $rel
 New-Item -Force -ItemType Directory (Split-Path $target) | Out-Null
 Copy-Item $_.FullName $target -Force
}
Copy-Item (Join-Path $root "src-runtime\META-INF") (Join-Path $classes "META-INF") -Recurse -Force
$manifest = Join-Path $Out "manifest.mf"
"Manifest-Version: 1.0" + [Environment]::NewLine + "Main-Class: com.mx.kylgis.kitchenscr.KitchenScr" + [Environment]::NewLine | Set-Content -Encoding ascii $manifest
$jarFile = Join-Path $dist "KylGis_POS_Monitor_de_Cocina_Java21.jar"
Push-Location $classes
try { & $jar --create --file $jarFile --manifest $manifest -C $classes . } finally { Pop-Location }
if ($LASTEXITCODE -ne 0) { throw "jar fallo" }
$lib | ForEach-Object { Copy-Item $_ (Join-Path $dist "lib") -Force }
$fx | ForEach-Object { Copy-Item $_ (Join-Path $dist "fx") -Force }
$rt = Join-Path $dist "runtime"
if (-not (Test-Path $rt)) {
 & $jlink --module-path ((Join-Path $JavaHome "jmods") + ";" + ($fx -join ";")) --add-modules javafx.controls,javafx.fxml,java.sql,java.naming,java.xml,java.desktop,java.management,java.instrument,jdk.unsupported --strip-debug --no-header-files --no-man-pages --output $rt
 if ($LASTEXITCODE -ne 0) { throw "jlink fallo" }
}
$bat = @'
@echo off
setlocal EnableDelayedExpansion
set "HERE=%~dp0"
set "CP=%HERE%KylGis_POS_Monitor_de_Cocina_Java21.jar"
for %%F in ("%HERE%lib\*.jar") do set "CP=!CP!;%%F"
set "FX="
for %%F in ("%HERE%fx\*.jar") do set "FX=!FX!;%%F"
"%HERE%runtime\bin\java.exe" --add-opens=java.base/java.lang=ALL-UNNAMED -Dprism.order=sw --module-path "%FX%" --add-modules javafx.controls,javafx.fxml -cp "%CP%" com.mx.kylgis.kitchenscr.KitchenScr %*
'@
Set-Content -Path (Join-Path $dist "Run.bat") -Value $bat -Encoding ascii
Get-ChildItem $dist -File -Recurse | Get-FileHash -Algorithm SHA256 | ForEach-Object { $_.Hash + "  " + $_.Path.Substring($dist.Length + 1) } | Set-Content (Join-Path $dist "SHA256SUMS.txt")
Write-Output "WINDOWS_PACKAGE_READY $dist"
