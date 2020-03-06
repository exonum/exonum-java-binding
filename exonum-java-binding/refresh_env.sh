#!/usr/bin/env bash

function refreshenv
{
  powershell -ExecutionPolicy Bypass -NonInteractive - <<\EOF
Import-Module "$env:ChocolateyInstall\helpers\chocolateyProfile.psm1"
Update-SessionEnvironment
# Round brackets in variable names cause problems with bash
Get-ChildItem env:* | %{
  if (!($_.Name.Contains('('))) {
    $value = $_.Value
    if ($_.Name -eq 'PATH') {
      $value = $value -replace ';',':'
    }
    Write-Output ("export " + $_.Name + "='" + $value + "'")
  }
} | Out-File -Encoding ascii $env:TEMP\refreshenv.sh
EOF

  echo $(cat "$TEMP/refreshenv.sh")
  source "$TEMP/refreshenv.sh"

  echo "Environment variables refeshed"
}
