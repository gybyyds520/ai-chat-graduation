/**
 * AI Chat Build and Deploy Script
 * Builds Electron app and creates all shortcuts
 */

const { exec, spawn } = require('child_process')
const path = require('path')
const fs = require('fs')

// Paths
const projectRoot = path.join(__dirname, '..')
const buildDir = path.join(projectRoot, 'build')
const distDir = path.join(projectRoot, 'dist')
const electronDistDir = path.join(distDir, 'electron')
const exePath = path.join(electronDistDir, 'win-unpacked', 'AI Chat.exe')

// ANSI colors for console
const colors = {
  reset: '\x1b[0m',
  bright: '\x1b[1m',
  dim: '\x1b[2m',
  cyan: '\x1b[36m',
  yellow: '\x1b[33m',
  green: '\x1b[32m',
  red: '\x1b[31m',
  white: '\x1b[37m',
  gray: '\x1b[90m'
}

function log(message, color = 'white') {
  console.log(`${colors[color]}${message}${colors.reset}`)
}

function runCommand(command, cwd, hideWindow = false) {
  return new Promise((resolve, reject) => {
    log(`Executing: ${command}`, 'gray')
    
    const options = { 
      cwd,
      shell: true,
      windowsHide: hideWindow
    }
    
    exec(command, options, (error, stdout, stderr) => {
      if (error) {
        reject(new Error(`${error.message}\n${stderr}`))
      } else {
        resolve(stdout)
      }
    })
  })
}

function runNpm(script, cwd) {
  return new Promise((resolve, reject) => {
    log(`Running npm script: ${script}`, 'yellow')
    
    const child = spawn('npm', ['run', script], {
      cwd,
      shell: true,
      stdio: 'inherit',
      windowsHide: true
    })
    
    child.on('close', (code) => {
      if (code === 0) {
        resolve()
      } else {
        reject(new Error(`npm script '${script}' failed with exit code ${code}`))
      }
    })
    
    child.on('error', (error) => {
      reject(error)
    })
  })
}

async function main() {
  console.log('')
  log('========================================', 'cyan')
  log('   AI Chat Build & Deploy', 'cyan')
  log('========================================', 'cyan')
  console.log('')
  
  try {
    // Step 1: Generate icon
    log('[Step 1/4] Generating application icon...', 'yellow')
    if (fs.existsSync(path.join(buildDir, 'generate-icon-simple.ps1'))) {
      await runCommand(
        'powershell -ExecutionPolicy Bypass -File "build/generate-icon-simple.ps1"',
        projectRoot,
        true
      )
    }
    console.log('')
    
    // Step 2: Build frontend
    log('[Step 2/4] Building frontend...', 'yellow')
    await runNpm('build', projectRoot)
    console.log('')
    
    // Step 3: Build Electron
    log('[Step 3/4] Building Electron application...', 'yellow')
    await runNpm('electron:build', projectRoot)
    console.log('')
    
    // Step 4: Create shortcuts
    if (fs.existsSync(exePath)) {
      log('[Step 4/4] Creating shortcuts...', 'yellow')
      
      const shortcutScript = path.join(__dirname, 'create-shortcut.js')
      if (fs.existsSync(shortcutScript)) {
        await runCommand(`node "${shortcutScript}" -all`, projectRoot, true)
      }
    } else {
      log('[WARNING] Executable not found, skipping shortcuts', 'yellow')
    }
    
    // Summary
    console.log('')
    log('========================================', 'green')
    log('   Build Completed Successfully!', 'green')
    log('========================================', 'green')
    console.log('')
    
    if (fs.existsSync(exePath)) {
      log('Executable location:', 'white')
      log(`  ${exePath}`, 'cyan')
      console.log('')
    }
    
    log('You can now:', 'white')
    log('  1. Double-click the AI Chat shortcut on Desktop', 'gray')
    log('  2. Find AI Chat in Start Menu', 'gray')
    log('  3. Launch from Quick Launch bar', 'gray')
    console.log('')
    
  } catch (error) {
    console.log('')
    log('========================================', 'red')
    log('   Build Failed!', 'red')
    log('========================================', 'red')
    console.log('')
    log(`Error: ${error.message}`, 'red')
    console.log('')
    process.exit(1)
  }
}

// Run main function
main()
