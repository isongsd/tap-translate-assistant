function Set-AndroidBuildJavaHome {
    $currentJavaHome = $env:JAVA_HOME
    if (-not [string]::IsNullOrWhiteSpace($currentJavaHome)) {
        $currentJava = Join-Path $currentJavaHome "bin\java.exe"
        if (Test-Path $currentJava) {
            return
        }
    }

    $candidates = @(
        "C:\Program Files\Microsoft\jdk-17.0.18.8-hotspot",
        "C:\Program Files\Android\Android Studio\jbr"
    )

    $javaCommand = Get-Command java -ErrorAction SilentlyContinue
    if ($javaCommand) {
        $javaHomeFromPath = Split-Path -Parent (Split-Path -Parent $javaCommand.Source)
        $candidates += $javaHomeFromPath
    }

    foreach ($candidate in ($candidates | Where-Object { -not [string]::IsNullOrWhiteSpace($_) } | Select-Object -Unique)) {
        $java = Join-Path $candidate "bin\java.exe"
        if (Test-Path $java) {
            $env:JAVA_HOME = $candidate
            return
        }
    }

    throw "JDK not found. Install JDK 17 or fix JAVA_HOME."
}
