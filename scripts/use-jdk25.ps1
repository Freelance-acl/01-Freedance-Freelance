# Dot-source before Maven so JAVA_HOME matches Java 25 (POM uses release 25):
#   . .\scripts\use-jdk25.ps1
#   .\mvnw.cmd package -DskipTests

$candidates = @(
    (Join-Path $env:ProgramFiles "Java\jdk-25"),
    (Join-Path $env:ProgramFiles "Java\jdk-25.0.2"),
    (Join-Path $env:ProgramFiles "Eclipse Adoptium\jdk-25.0.2.10-hotspot")
)
foreach ($d in $candidates) {
    $javac = Join-Path $d "bin\javac.exe"
    if (Test-Path $javac) {
        $env:JAVA_HOME = $d
        $bin = Join-Path $d "bin"
        $path = [string]$env:Path
        if ($path -notlike "$bin;*" -and $path -ne $bin) {
            $env:Path = "$bin;$path"
        }
        Write-Host "JAVA_HOME=$d"
        return
    }
}
Write-Warning "JDK 25 not found under Program Files. Install Temurin/Oracle JDK 25 or set JAVA_HOME to that JDK, then retry."
