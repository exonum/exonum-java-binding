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
    if ($_.Name -eq 'Path') {
      $value = $value -replace ';',':'
    }
    Write-Output ("export " + $_.Name + "='" + $value + "'")
  }
} | Out-File -Encoding ascii .\refreshenv.sh
EOF

  echo $(cat "./refreshenv.sh")
  source "./refreshenv.sh"

  echo "Environment variables refeshed"
}
