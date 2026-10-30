# PowerShell script to analyze Vue template div structure
$filePath = "c:\Users\郭耀彬\Desktop\gybct\ai-chat-graduation\frontend\src\views\AdminView.vue"
$content = Get-Content -Path $filePath -Raw
$lines = $content -split "`n"

# Find template boundaries
$templateStart = -1
$templateEnd = -1

for ($i = 0; $i -lt $lines.Count; $i++) {
    if ($lines[$i] -match '<template>' -and $templateStart -eq -1) {
        $templateStart = $i
    }
    if ($lines[$i] -match '</template>' -and $templateStart -ne -1) {
        $templateEnd = $i
        break
    }
}

Write-Host "Template found: lines $($templateStart + 1) to $($templateEnd + 1)"

# Track divs
$stack = @()
$depth = 0

for ($i = $templateStart; $i -le $templateEnd; $i++) {
    $line = $lines[$i]
    $lineNum = $i + 1
    
    # Skip template/script/style tags and comments
    if ($line -match '<template>|</template>|<script|</script|<style|</style|<!--' -and $line -notmatch '<div') {
        continue
    }
    
    # Find opening divs (multi-line support)
    $openings = [regex]::Matches($line, '<div(?:\s[^>]*)?>(?!\s*</)')
    foreach ($match in $openings) {
        $depth++
        $indent = "  " * $depth
        $divText = $line.Substring($match.Index, [Math]::Min(70, $line.Length - $match.Index))
        $stack += ,@($lineNum, $depth, $divText.Trim())
        Write-Host "$indent OPEN  [$lineNum]: $($divText.Trim().Substring(0, [Math]::Min(60, $divText.Trim().Length)))"
    }
    
    # Find closing divs
    $closings = [regex]::Matches($line, '</div>')
    foreach ($match in $closings) {
        if ($stack.Count -gt 0) {
            $item = $stack[$stack.Count - 1]
            $stack = $stack[0..($stack.Count - 2)]
            $depth--
            $indent = "  " * ($depth + 1)
            Write-Host "$indent CLOSE [$lineNum]: </div> (was at line $($item[0]))"
        } else {
            Write-Host "  ERROR [$lineNum]: Extra closing </div>"
        }
    }
}

Write-Host ""
Write-Host "=" * 80
if ($stack.Count -gt 0) {
    Write-Host "UNCLOSED DIVS ($($stack.Count) items):"
    foreach ($item in $stack) {
        Write-Host "  Line $($item[0]): depth $($item[1]) - $($item[2].Substring(0, [Math]::Min(50, $item[2].Length)))"
    }
} else {
    Write-Host "All divs are properly closed!"
}
