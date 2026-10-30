$url = "https://npmmirror.com/mirrors/electron/28.3.3/electron-v28.3.3-win32-x64.zip"
$output = "electron.zip"
$extractPath = "node_modules/electron/dist"

Write-Host "Downloading Electron..."
Invoke-WebRequest -Uri $url -OutFile $output -UseBasicParsing

Write-Host "Extracting..."
Expand-Archive -Path $output -DestinationPath $extractPath -Force

Write-Host "Cleaning up..."
Remove-Item $output

Write-Host "Done!"