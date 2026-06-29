$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $MyInvocation.MyCommand.Path
$backend = Join-Path $root "backend"
$frontend = Join-Path $root "frontend"

$javaHome = if ($env:JAVA_HOME) { $env:JAVA_HOME } else { "E:\jdk" }
$mavenHome = if ($env:MAVEN_HOME) { $env:MAVEN_HOME } else { "E:\Apache\apache-maven-3.9.11" }
$nodeHome = if ($env:NODE_HOME) { $env:NODE_HOME } else { "E:\nodejs" }
$mvnCmd = Join-Path $mavenHome "bin\mvn.cmd"
$npmCmd = Join-Path $nodeHome "npm.cmd"

$env:JAVA_HOME = $javaHome
$env:MAVEN_HOME = $mavenHome
$env:Path = "$javaHome\bin;$mavenHome\bin;$nodeHome;" + $env:Path

Write-Host "Starting backend on http://localhost:8080 ..."
Start-Process -FilePath $mvnCmd `
  -ArgumentList "spring-boot:run" `
  -WorkingDirectory $backend `
  -RedirectStandardOutput (Join-Path $backend "backend.log") `
  -RedirectStandardError (Join-Path $backend "backend.err.log") `
  -WindowStyle Hidden

Write-Host "Starting frontend on http://localhost:5173 ..."
Start-Process -FilePath $npmCmd `
  -ArgumentList "run", "dev" `
  -WorkingDirectory $frontend `
  -RedirectStandardOutput (Join-Path $frontend "frontend.log") `
  -RedirectStandardError (Join-Path $frontend "frontend.err.log") `
  -WindowStyle Hidden

Write-Host "Done. Open http://localhost:5173"
