$p = "C:\Users\郭耀彬\Desktop\gybct\ai-chat-graduation\frontend\src\views\FriendView.vue"
$c = Get-Content $p -Raw -Encoding utf8
if ($c -notmatch "\.online-dot\s*\{") {
  $css = ".friend-avatar { position: relative; }`n.friend-avatar .online-dot { position: absolute; bottom: 0; right: 0; width: 10px; height: 10px; background: #22c55e; border-radius: 50%; border: 2px solid var(--card-bg, #fff); }`n.friend-status-text { font-size: 11px; color: var(--text-muted); }`n"
  $c = $c -replace "</style>", ($css + "</style>")
  [System.IO.File]::WriteAllText($p, $c, (New-Object System.Text.UTF8Encoding($false)))
  Write-Host "CSS INSERTED"
} else {
  Write-Host "ALREADY PRESENT"
}
