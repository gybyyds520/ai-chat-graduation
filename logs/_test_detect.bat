@echo off
powershell -NoProfile -Command "$api=[bool](Get-NetTCPConnection -State Listen -LocalPort 8080 -ErrorAction SilentlyContinue); $web=[bool](Get-NetTCPConnection -State Listen -LocalPort 5187 -ErrorAction SilentlyContinue); if($api -and $web){ exit 0 } else { exit 1 }"
echo DETECT_CODE=%errorlevel%
