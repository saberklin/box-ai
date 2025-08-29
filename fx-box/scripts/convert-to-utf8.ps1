$ErrorActionPreference = 'Stop'

# 扫描 src 下常见文本类型，检测非严格 UTF-8 的文件，尝试按 GBK 还原并写回为 UTF-8
$root = Join-Path $PSScriptRoot '..' | Resolve-Path
$src = Join-Path $root 'src'
$utf8Strict = New-Object System.Text.UTF8Encoding($false, $true)
$gbk = [System.Text.Encoding]::GetEncoding(936)
$exts = @('.java','.css','.fxml','.properties','.txt','.md','.xml')

$files = Get-ChildItem -Path $src -Recurse -File | Where-Object { $exts -contains $_.Extension.ToLower() }
$converted = @()

foreach($f in $files){
  $bytes = [System.IO.File]::ReadAllBytes($f.FullName)
  try {
    # 若严格 UTF-8 成功，则跳过
    $null = $utf8Strict.GetString($bytes)
  } catch {
    # 不是有效 UTF-8：按 GBK 还原成字符串，再写回为 UTF-8（不带 BOM）
    $text = $gbk.GetString($bytes)
    [System.IO.File]::WriteAllText($f.FullName, $text, (New-Object System.Text.UTF8Encoding($false)))
    $converted += $f.FullName
  }
}

Write-Output ("Converted: " + $converted.Count)
$converted | ForEach-Object { Write-Output $_ }


