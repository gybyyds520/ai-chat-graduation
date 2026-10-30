Write-Host "=== Starting file copy ==="

# Create directories
New-Item -Path "dist/electron/AIChat-win32-x64/resources/app/dist" -ItemType Directory -Force | Out-Null
New-Item -Path "dist/electron/AIChat-win32-x64/resources/app/electron" -ItemType Directory -Force | Out-Null

# Copy frontend files (excluding electron directory)
Write-Host "Copying frontend files..."
Get-ChildItem -Path "dist" -Exclude "electron" | ForEach-Object {
    if ($_.PSIsContainer) {
        Copy-Item -Path $_.FullName -Destination "dist/electron/AIChat-win32-x64/resources/app/dist/" -Recurse -Force
    } else {
        Copy-Item -Path $_.FullName -Destination "dist/electron/AIChat-win32-x64/resources/app/dist/" -Force
    }
}

# Copy electron files
Write-Host "Copying electron files..."
Copy-Item -Path "electron/main.js" -Destination "dist/electron/AIChat-win32-x64/resources/app/electron/" -Force
Copy-Item -Path "electron/preload.js" -Destination "dist/electron/AIChat-win32-x64/resources/app/electron/" -Force
Copy-Item -Path "electron/trayConfig.js" -Destination "dist/electron/AIChat-win32-x64/resources/app/electron/" -Force
Copy-Item -Path "electron/enhancedTray.js" -Destination "dist/electron/AIChat-win32-x64/resources/app/electron/" -Force

# Copy package.json
Write-Host "Copying package.json..."
Copy-Item -Path "package.json" -Destination "dist/electron/AIChat-win32-x64/resources/app/" -Force

# Verify
Write-Host "=== Verification ==="
Write-Host "index.html exists: $(Test-Path "dist/electron/AIChat-win32-x64/resources/app/dist/index.html")"
Write-Host "main.js exists: $(Test-Path "dist/electron/AIChat-win32-x64/resources/app/electron/main.js")"

Write-Host "=== Done ==="