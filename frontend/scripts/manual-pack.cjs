/**
 * Manual Electron Packager Script
 * Bypasses electron-builder's code signing issues
 */

const fs = require('fs')
const path = require('path')
const { execSync } = require('child_process')

const projectRoot = path.join(__dirname, '..')
const electronDist = path.join(projectRoot, 'dist', 'electron')
const appDir = path.join(electronDist, 'AI Chat-win32-x64')
const resourcesDir = path.join(appDir, 'resources')
const appResourcesDir = path.join(resourcesDir, 'app')

function log(message, color = '\x1b[37m') {
  console.log(`${color}${message}\x1b[0m`)
}

function logStep(title) {
  console.log('')
  log('──────────────────────────────────────────────', '\x1b[36m')
  log(title, '\x1b[1m')
  log('──────────────────────────────────────────────', '\x1b[36m')
}

try {
  logStep('1. Creating directory structure')
  
  // Create directories
  if (!fs.existsSync(electronDist)) fs.mkdirSync(electronDist, { recursive: true })
  if (!fs.existsSync(appDir)) fs.mkdirSync(appDir, { recursive: true })
  if (!fs.existsSync(resourcesDir)) fs.mkdirSync(resourcesDir, { recursive: true })
  if (!fs.existsSync(appResourcesDir)) fs.mkdirSync(appResourcesDir, { recursive: true })
  
  log('✓ Directory structure created', '\x1b[32m')

  logStep('2. Copying Electron binaries')
  
  // Find Electron path
  const electronPath = path.join(projectRoot, 'node_modules', 'electron')
  const electronBinary = path.join(electronPath, 'dist', 'electron.exe')
  
  if (!fs.existsSync(electronBinary)) {
    throw new Error(`Electron binary not found: ${electronBinary}`)
  }
  
  // Copy electron.exe as AI Chat.exe
  fs.copyFileSync(electronBinary, path.join(appDir, 'AI Chat.exe'))
  
  // Copy other Electron files
  const electronFiles = ['ffmpeg.dll', 'libEGL.dll', 'libGLESv2.dll', 'locales', 'resources', 'version']
  for (const file of electronFiles) {
    const src = path.join(electronPath, 'dist', file)
    const dest = path.join(appDir, file)
    if (fs.existsSync(src)) {
      if (fs.lstatSync(src).isDirectory()) {
        execSync(`xcopy /E /I /H /Y "${src}" "${dest}"`, { stdio: 'ignore' })
      } else {
        fs.copyFileSync(src, dest)
      }
    }
  }
  
  log('✓ Electron binaries copied', '\x1b[32m')

  logStep('3. Copying application resources')
  
  // Copy package.json
  fs.copyFileSync(path.join(projectRoot, 'package.json'), path.join(appResourcesDir, 'package.json'))
  
  // Copy electron main file
  fs.copyFileSync(path.join(projectRoot, 'electron', 'main.js'), path.join(appResourcesDir, 'main.js'))
  
  // Copy preload
  fs.copyFileSync(path.join(projectRoot, 'electron', 'preload.js'), path.join(appResourcesDir, 'preload.js'))
  
  // Copy tray config
  fs.copyFileSync(path.join(projectRoot, 'electron', 'trayConfig.js'), path.join(appResourcesDir, 'trayConfig.js'))
  
  // Copy enhanced tray
  fs.copyFileSync(path.join(projectRoot, 'electron', 'enhancedTray.js'), path.join(appResourcesDir, 'enhancedTray.js'))
  
  // Copy frontend dist
  const frontendDist = path.join(projectRoot, 'dist')
  const appDist = path.join(appResourcesDir, 'dist')
  if (!fs.existsSync(appDist)) fs.mkdirSync(appDist, { recursive: true })
  execSync(`powershell -Command "Copy-Item -Path '${frontendDist}\\*' -Destination '${appDist}' -Recurse -Force"`, { stdio: 'ignore' })
  
  log('✓ Application resources copied', '\x1b[32m')

  logStep('4. Setting up icon')
  
  const iconPath = path.join(projectRoot, 'build', 'icon.ico')
  if (fs.existsSync(iconPath)) {
    fs.copyFileSync(iconPath, path.join(appDir, 'AI Chat.ico'))
    log('✓ Icon copied', '\x1b[32m')
  } else {
    log('⚠ Icon not found, skipping', '\x1b[33m')
  }

  logStep('5. Verifying package')
  
  const expectedFiles = [
    path.join(appDir, 'AI Chat.exe'),
    path.join(appResourcesDir, 'package.json'),
    path.join(appResourcesDir, 'main.js'),
    path.join(appResourcesDir, 'dist', 'index.html')
  ]
  
  let allExists = true
  for (const file of expectedFiles) {
    if (fs.existsSync(file)) {
      log(`✓ ${path.relative(projectRoot, file)}`, '\x1b[32m')
    } else {
      log(`✗ ${path.relative(projectRoot, file)}`, '\x1b[31m')
      allExists = false
    }
  }
  
  if (!allExists) {
    throw new Error('Package verification failed')
  }

  console.log('')
  log('══════════════════════════════════════════════', '\x1b[32m')
  log('           PACKAGE CREATED SUCCESSFULLY!', '\x1b[1m')
  log('══════════════════════════════════════════════', '\x1b[32m')
  console.log('')
  log(`Output directory: ${appDir}`, '\x1b[36m')
  console.log('')
  log('To run the application:', '\x1b[37m')
  log(`  > "${path.join(appDir, 'AI Chat.exe')}"`, '\x1b[33m')
  console.log('')
  
} catch (error) {
  console.log('')
  log('══════════════════════════════════════════════', '\x1b[31m')
  log('              PACKAGE CREATION FAILED!', '\x1b[1m')
  log('══════════════════════════════════════════════', '\x1b[31m')
  console.log('')
  log(`Error: ${error.message}`, '\x1b[31m')
  console.log('')
  process.exit(1)
}