Write-Host "=== Full Electron Package Script ==="

# Clean up old builds
Write-Host "`n1. Cleaning up..."
if (Test-Path "dist/electron") {
    Write-Host "Deleting old electron directory..."
    Remove-Item -Path "dist/electron" -Recurse -Force -ErrorAction SilentlyContinue
    Start-Sleep -Seconds 1
}

# Create directories
Write-Host "`n2. Creating directories..."
New-Item -Path "dist/electron/AIChat-win32-x64" -ItemType Directory -Force | Out-Null
New-Item -Path "dist/electron/AIChat-win32-x64/resources/app" -ItemType Directory -Force | Out-Null
New-Item -Path "dist/electron/AIChat-win32-x64/resources/app/dist" -ItemType Directory -Force | Out-Null
New-Item -Path "dist/electron/AIChat-win32-x64/resources/app/electron" -ItemType Directory -Force | Out-Null

# Copy Electron core files
Write-Host "`n3. Copying Electron core files..."
Copy-Item -Path "node_modules/electron/dist/electron.exe" -Destination "dist/electron/AIChat-win32-x64/" -Force
Copy-Item -Path "node_modules/electron/dist/icudtl.dat" -Destination "dist/electron/AIChat-win32-x64/" -Force
Copy-Item -Path "node_modules/electron/dist/snapshot_blob.bin" -Destination "dist/electron/AIChat-win32-x64/" -Force
Copy-Item -Path "node_modules/electron/dist/v8_context_snapshot.bin" -Destination "dist/electron/AIChat-win32-x64/" -Force
Copy-Item -Path "node_modules/electron/dist/ffmpeg.dll" -Destination "dist/electron/AIChat-win32-x64/" -Force
Copy-Item -Path "node_modules/electron/dist/libEGL.dll" -Destination "dist/electron/AIChat-win32-x64/" -Force
Copy-Item -Path "node_modules/electron/dist/libGLESv2.dll" -Destination "dist/electron/AIChat-win32-x64/" -Force
Copy-Item -Path "node_modules/electron/dist/resources.pak" -Destination "dist/electron/AIChat-win32-x64/" -Force
Copy-Item -Path "node_modules/electron/dist/chrome_100_percent.pak" -Destination "dist/electron/AIChat-win32-x64/" -Force
Copy-Item -Path "node_modules/electron/dist/chrome_200_percent.pak" -Destination "dist/electron/AIChat-win32-x64/" -Force
Copy-Item -Path "node_modules/electron/dist/locales" -Destination "dist/electron/AIChat-win32-x64/" -Recurse -Force
Copy-Item -Path "node_modules/electron/dist/resources" -Destination "dist/electron/AIChat-win32-x64/" -Recurse -Force

# Copy frontend files
Write-Host "`n4. Copying frontend files..."
Copy-Item -Path "dist/index.html" -Destination "dist/electron/AIChat-win32-x64/resources/app/dist/" -Force
Copy-Item -Path "dist/assets" -Destination "dist/electron/AIChat-win32-x64/resources/app/dist/" -Recurse -Force

# Copy Electron main files
Write-Host "`n5. Copying Electron main files..."
Copy-Item -Path "electron/main.js" -Destination "dist/electron/AIChat-win32-x64/resources/app/electron/" -Force
Copy-Item -Path "electron/preload.js" -Destination "dist/electron/AIChat-win32-x64/resources/app/electron/" -Force
Copy-Item -Path "electron/trayConfig.js" -Destination "dist/electron/AIChat-win32-x64/resources/app/electron/" -Force
Copy-Item -Path "electron/enhancedTray.js" -Destination "dist/electron/AIChat-win32-x64/resources/app/electron/" -Force

# Copy package.json (without type:module)
Write-Host "`n6. Copying package.json..."
$pkgContent = Get-Content "package.json" -Raw | ConvertFrom-Json
$pkgContent.PSObject.Properties.Remove("type")
$pkgContent | ConvertTo-Json -Depth 10 | Set-Content "dist/electron/AIChat-win32-x64/resources/app/package.json" -Encoding UTF8

# Copy vite.svg
Write-Host "`n7. Copying static assets..."
Copy-Item -Path "public/vite.svg" -Destination "dist/electron/AIChat-win32-x64/resources/app/dist/" -Force

# Verification
Write-Host "`n=== Verification ==="
Write-Host "electron.exe: $(Test-Path "dist/electron/AIChat-win32-x64/electron.exe")"
Write-Host "icudtl.dat: $(Test-Path "dist/electron/AIChat-win32-x64/icudtl.dat")"
Write-Host "index.html: $(Test-Path "dist/electron/AIChat-win32-x64/resources/app/dist/index.html")"
Write-Host "main.js: $(Test-Path "dist/electron/AIChat-win32-x64/resources/app/electron/main.js")"
Write-Host "package.json: $(Test-Path "dist/electron/AIChat-win32-x64/resources/app/package.json")"

Write-Host "`n=== Package Complete ==="