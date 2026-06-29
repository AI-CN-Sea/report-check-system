$ports = @(8080, 5173)

foreach ($port in $ports) {
  $lines = netstat -ano | Select-String "LISTENING" | Select-String "^[ ]*TCP[ ]+([^ ]+:$port)[ ]+"
  foreach ($line in $lines) {
    $parts = ($line.ToString() -split "\s+") | Where-Object { $_ }
    if ($parts.Count -gt 0 -and $parts[-1] -match "^\d+$") {
      $processId = [int]$parts[-1]
      try {
        Stop-Process -Id $processId -Force -ErrorAction Stop
        Write-Host "Stopped process $processId on port $port"
      } catch {
        Write-Host "Skip process $processId on port ${port}: $($_.Exception.Message)"
      }
    }
  }
}

Write-Host "Development services stopped."
