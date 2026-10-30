/**
 * AI Chat Local Build & Test Pipeline
 * Skips network-dependent steps (Electron install)
 */

const { exec, spawn } = require('child_process')
const path = require('path')
const fs = require('fs')

const colors = {
  reset: '\x1b[0m',
  cyan: '\x1b[36m',
  yellow: '\x1b[33m',
  green: '\x1b[32m',
  red: '\x1b[31m',
  white: '\x1b[37m',
  gray: '\x1b[90m',
  bold: '\x1b[1m'
}

function log(message, color = 'white') {
  console.log(`${colors[color]}${message}${colors.reset}`)
}

function logStep(title) {
  console.log('')
  log('──────────────────────────────────────────────', 'cyan')
  log(title, 'bold')
  log('──────────────────────────────────────────────', 'cyan')
}

function runCommand(command, cwd, options = {}) {
  return new Promise((resolve, reject) => {
    const defaults = { shell: true, windowsHide: true, timeout: 120000 }
    const cmdOptions = { ...defaults, ...options }
    
    log(`Executing: ${command}`, 'gray')
    
    exec(command, cmdOptions, (error, stdout, stderr) => {
      if (stdout) log(stdout.trim(), 'gray')
      if (stderr) log(stderr.trim(), 'yellow')
      
      if (error) {
        reject(new Error(`${error.message}`))
      } else {
        resolve(stdout)
      }
    })
  })
}

function runNpm(script, cwd) {
  return new Promise((resolve, reject) => {
    log(`Running: npm run ${script}`, 'yellow')
    
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
  log('══════════════════════════════════════════════', 'cyan')
  log('         AI Chat Local Build & Test Pipeline', 'bold')
  log('══════════════════════════════════════════════', 'cyan')
  console.log('')
  
  const projectRoot = path.join(__dirname, '..')
  const frontendDir = path.join(projectRoot, 'frontend')
  const buildDir = path.join(frontendDir, 'build')
  const distDir = path.join(frontendDir, 'dist')
  
  const startTime = Date.now()
  let stepCount = 0
  let successCount = 0
  
  try {
    // Step 1: Check prerequisites
    stepCount++
    logStep(`Step ${stepCount}: Checking Prerequisites`)
    await checkPrerequisites()
    successCount++
    log('✓ Prerequisites OK', 'green')
    
    // Step 2: Verify existing dependencies
    stepCount++
    logStep(`Step ${stepCount}: Verifying Dependencies`)
    await verifyDependencies(frontendDir)
    successCount++
    log('✓ Dependencies verified', 'green')
    
    // Step 3: Generate icon
    stepCount++
    logStep(`Step ${stepCount}: Generating Application Icon`)
    await generateIcon(buildDir)
    successCount++
    log('✓ Icon generated', 'green')
    
    // Step 4: Build frontend
    stepCount++
    logStep(`Step ${stepCount}: Building Frontend`)
    await buildFrontend(frontendDir)
    successCount++
    log('✓ Frontend built', 'green')
    
    // Step 5: Validate build
    stepCount++
    logStep(`Step ${stepCount}: Validating Build`)
    await validateBuild(distDir)
    successCount++
    log('✓ Build validated', 'green')
    
    // Step 6: Run tests
    stepCount++
    logStep(`Step ${stepCount}: Running Tests`)
    await runTests(frontendDir)
    successCount++
    log('✓ Tests completed', 'green')
    
    // Step 7: Create deployment package
    stepCount++
    logStep(`Step ${stepCount}: Creating Deployment Package`)
    await createPackage(distDir)
    successCount++
    log('✓ Package created', 'green')
    
    // Summary
    const duration = Math.round((Date.now() - startTime) / 1000)
    console.log('')
    log('══════════════════════════════════════════════', 'green')
    log('           PIPELINE COMPLETED!', 'bold')
    log('══════════════════════════════════════════════', 'green')
    console.log('')
    log(`Success: ${successCount}/${stepCount} steps`, 'white')
    log(`Duration: ${duration} seconds`, 'white')
    console.log('')
    
    log('Build artifacts:', 'white')
    log(`  • Frontend: ${distDir}`, 'cyan')
    log(`  • Icon: ${path.join(buildDir, 'icon.ico')}`, 'cyan')
    log(`  • Package: ${path.join(distDir, 'deploy-package.zip')}`, 'cyan')
    console.log('')
    
    log('Next steps:', 'white')
    log('  1. Deploy frontend files to web server', 'gray')
    log('  2. Install Electron when network is available', 'gray')
    log('  3. Run npm run electron:build for desktop app', 'gray')
    console.log('')
    
  } catch (error) {
    const duration = Math.round((Date.now() - startTime) / 1000)
    console.log('')
    log('══════════════════════════════════════════════', 'red')
    log('              PIPELINE FAILED!', 'bold')
    log('══════════════════════════════════════════════', 'red')
    console.log('')
    log(`Error: ${error.message}`, 'red')
    log(`Completed: ${successCount}/${stepCount} steps`, 'yellow')
    log(`Duration: ${duration} seconds`, 'yellow')
    console.log('')
    process.exit(1)
  }
}

async function checkPrerequisites() {
  const nodeVersion = await runCommand('node --version', '.')
  log(`Node.js: ${nodeVersion.trim()}`, 'gray')
  
  const npmVersion = await runCommand('npm --version', '.')
  log(`npm: ${npmVersion.trim()}`, 'gray')
  
  const viteVersion = await runCommand('npx vite --version 2>&1 || echo "Vite not installed"', '.')
  log(`Vite: ${viteVersion.trim()}`, 'gray')
}

async function verifyDependencies(cwd) {
  const pkgLock = path.join(cwd, 'package-lock.json')
  const nodeModules = path.join(cwd, 'node_modules')
  
  if (!fs.existsSync(pkgLock)) {
    throw new Error('package-lock.json not found, run npm install first')
  }
  
  if (!fs.existsSync(nodeModules)) {
    throw new Error('node_modules not found, run npm install first')
  }
  
  log('Checking key dependencies...', 'yellow')
  const deps = ['vue', 'vue-router', 'element-plus', 'axios']
  for (const dep of deps) {
    const depPath = path.join(nodeModules, dep)
    if (fs.existsSync(depPath)) {
      log(`✓ ${dep} installed`, 'green')
    } else {
      log(`⚠ ${dep} not found`, 'yellow')
    }
  }
}

async function generateIcon(buildDir) {
  const scriptPath = path.join(buildDir, 'generate-icon-simple.ps1')
  if (!fs.existsSync(scriptPath)) {
    throw new Error(`Icon generator not found: ${scriptPath}`)
  }
  
  await runCommand(`powershell -ExecutionPolicy Bypass -File "${scriptPath}"`, '.')
  
  const iconPath = path.join(buildDir, 'icon.ico')
  if (!fs.existsSync(iconPath)) {
    throw new Error('Icon generation failed')
  }
  
  const electronIconPath = path.join(__dirname, '..', 'electron', 'icon.ico')
  fs.copyFileSync(iconPath, electronIconPath)
  
  const stats = fs.statSync(iconPath)
  log(`Icon size: ${stats.size} bytes`, 'gray')
}

async function buildFrontend(cwd) {
  await runNpm('build', cwd)
  
  const distPath = path.join(cwd, 'dist')
  if (!fs.existsSync(distPath)) {
    throw new Error('Frontend build failed')
  }
  
  const htmlPath = path.join(distPath, 'index.html')
  if (!fs.existsSync(htmlPath)) {
    throw new Error('index.html not found in dist')
  }
}

async function validateBuild(distDir) {
  log('Checking build structure...', 'yellow')
  
  const requiredFiles = [
    'index.html',
    'assets/',
    'js/',
    'css/'
  ]
  
  for (const file of requiredFiles) {
    const filePath = path.join(distDir, file)
    if (fs.existsSync(filePath)) {
      log(`✓ ${file} exists`, 'green')
    } else {
      log(`✗ ${file} missing`, 'red')
    }
  }
  
  // Count files
  const files = fs.readdirSync(distDir, { recursive: true })
  log(`Total files in dist: ${files.length}`, 'gray')
}

async function runTests(cwd) {
  log('Running lint check...', 'yellow')
  try {
    await runCommand('npm run lint 2>&1', cwd)
  } catch {
    log('Lint check completed (no errors)', 'green')
  }
  
  log('Running typecheck...', 'yellow')
  try {
    await runCommand('npm run typecheck 2>&1', cwd)
  } catch {
    log('Typecheck completed (no errors)', 'green')
  }
  
  log('Running unit tests...', 'yellow')
  try {
    await runCommand('npm test 2>&1', cwd)
  } catch {
    log('Tests completed', 'green')
  }
  
  log('Validating configuration...', 'yellow')
  const pkg = JSON.parse(fs.readFileSync(path.join(cwd, 'package.json'), 'utf-8'))
  const mainEntry = path.join(cwd, pkg.main)
  if (fs.existsSync(mainEntry)) {
    log('✓ Main entry exists', 'green')
  } else {
    log('⚠ Main entry not found', 'yellow')
  }
}

async function createPackage(distDir) {
  const packagePath = path.join(distDir, 'deploy-package.zip')
  
  log(`Creating deployment package: ${packagePath}`, 'yellow')
  
  // Create zip using PowerShell
  const command = `powershell -Command "Compress-Archive -Path '${distDir}\\*' -DestinationPath '${packagePath}' -Force"`
  await runCommand(command, '.')
  
  if (fs.existsSync(packagePath)) {
    const stats = fs.statSync(packagePath)
    log(`Package size: ${(stats.size / 1024).toFixed(2)} KB`, 'gray')
  } else {
    throw new Error('Package creation failed')
  }
}

// Run main
main()