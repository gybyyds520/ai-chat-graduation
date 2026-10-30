/**
 * AI Chat Desktop Shortcut Creator (Node.js Version)
 * Creates Windows desktop shortcuts without console window
 */

const fs = require('fs')
const path = require('path')
const os = require('os')

// Get platform-specific paths
function getPaths() {
  const homedir = os.homedir()
  const desktopPath = path.join(homedir, 'Desktop')
  const startMenuPath = path.join(os.tmpdir(), '..', 'AppData', 'Roaming', 'Microsoft', 'Windows', 'Start Menu', 'Programs')
  const quickLaunchPath = path.join(os.tmpdir(), '..', 'AppData', 'Roaming', 'Microsoft', 'Internet Explorer', 'Quick Launch')
  
  return {
    desktop: desktopPath,
    startMenu: startMenuPath,
    quickLaunch: quickLaunchPath
  }
}

// Get exe path
function getExePath() {
  const projectRoot = path.join(__dirname, '..')
  const exePath = path.join(projectRoot, 'dist', 'electron', 'win-unpacked', 'AI Chat.exe')
  return exePath
}

// Get icon path
function getIconPath() {
  const projectRoot = path.join(__dirname, '..')
  const iconPath = path.join(projectRoot, 'build', 'icon.ico')
  return iconPath
}

// Create Windows shortcut using PowerShell
async function createShortcut(targetPath, shortcutLocation, description) {
  return new Promise((resolve, reject) => {
    const exec = require('child_process').exec
    
    // PowerShell command to create shortcut
    const psCommand = `
      $WshShell = New-Object -ComObject WScript.Shell
      $Shortcut = $WshShell.CreateShortcut("${shortcutLocation.replace(/\\/g, '\\\\')}")
      $Shortcut.TargetPath = "${targetPath.replace(/\\/g, '\\\\')}"
      $Shortcut.WorkingDirectory = "${path.dirname(targetPath).replace(/\\/g, '\\\\')}"
      $Shortcut.Description = "${description}"
      $Shortcut.IconLocation = "${getIconPath().replace(/\\/g, '\\\\')}"
      $Shortcut.Save()
    `
    
    // Execute PowerShell command
    const cmd = `powershell -ExecutionPolicy Bypass -Command "${psCommand.replace(/"/g, '\\"').replace(/\n/g, ' ')}"`
    
    exec(cmd, { windowsHide: true }, (error, stdout, stderr) => {
      if (error) {
        console.error(`[ERROR] Failed to create shortcut: ${error.message}`)
        resolve(false)
      } else {
        console.log(`[OK] Shortcut created: ${shortcutLocation}`)
        resolve(true)
      }
    })
  })
}

// Main function
async function main() {
  console.log('========================================')
  console.log('   AI Chat Shortcut Creator (Node.js)')
  console.log('========================================')
  console.log('')
  
  const exePath = getExePath()
  const iconPath = getIconPath()
  const paths = getPaths()
  const shortcutName = 'AI Chat.lnk'
  
  console.log(`Exe Path:     ${exePath}`)
  console.log(`Icon Path:    ${iconPath}`)
  console.log('')
  
  // Check if exe exists
  if (!fs.existsSync(exePath)) {
    console.log('[WARNING] Executable not found!')
    console.log('[INFO] Please build the Electron app first:')
    console.log('  npm run build')
    console.log('  npm run electron:build')
    console.log('')
    return
  }
  
  // Parse command line arguments
  const args = process.argv.slice(2)
  const createAll = args.includes('-all') || args.includes('--all') || args.includes('-a')
  const createDesktop = args.includes('-desktop') || createAll
  const createStartMenu = args.includes('-startmenu') || createAll
  const createQuickLaunch = args.includes('-quicklaunch') || createAll
  
  // If no specific option, create all
  const doAll = createDesktop || createStartMenu || createQuickLaunch || args.length === 0
  
  let created = 0
  
  // Create Desktop shortcut
  if (doAll || createDesktop) {
    const desktopShortcut = path.join(paths.desktop, shortcutName)
    if (await createShortcut(exePath, desktopShortcut, 'AI Chat Application')) {
      created++
    }
  }
  
  // Create Start Menu shortcut
  if (doAll || createStartMenu) {
    try {
      if (!fs.existsSync(paths.startMenu)) {
        fs.mkdirSync(paths.startMenu, { recursive: true })
      }
      const startMenuShortcut = path.join(paths.startMenu, shortcutName)
      if (await createShortcut(exePath, startMenuShortcut, 'AI Chat Application')) {
        created++
      }
    } catch (err) {
      console.log(`[WARNING] Could not create Start Menu shortcut: ${err.message}`)
    }
  }
  
  // Create Quick Launch shortcut
  if (doAll || createQuickLaunch) {
    try {
      if (!fs.existsSync(paths.quickLaunch)) {
        fs.mkdirSync(paths.quickLaunch, { recursive: true })
      }
      const quickLaunchShortcut = path.join(paths.quickLaunch, shortcutName)
      if (await createShortcut(exePath, quickLaunchShortcut, 'AI Chat Application')) {
        created++
      }
    } catch (err) {
      console.log(`[WARNING] Could not create Quick Launch shortcut: ${err.message}`)
    }
  }
  
  // Summary
  console.log('')
  console.log('========================================')
  if (created > 0) {
    console.log(`   SUCCESS! Created ${created} shortcut(s)`)
  } else {
    console.log('   NO shortcuts were created')
    console.log('   Usage: node create-shortcut.js [-all|-desktop|-startmenu|-quicklaunch]')
  }
  console.log('========================================')
  console.log('')
}

// Run main function
main().catch(console.error)
