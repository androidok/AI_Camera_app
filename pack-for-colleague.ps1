# 打包项目给同事用
$src = "C:\Users\hp\Desktop\ai-camera\AI_Camera_app-main"
$dst = "C:\Users\hp\Desktop\ai-camera\ai-camera-for-colleague.zip"

# 排除的文件夹/文件
$exclude = @(".gradle", "app\build", ".idea\.gradle", ".kotlin", "*.log", "hs_err_*.log")

# 创建临时目录复制
$temp = "C:\Users\hp\Desktop\ai-camera\temp-pack"
if (Test-Path $temp) { Remove-Item $temp -Recurse -Force }
New-Item -ItemType Directory -Path $temp | Out-Null

Copy-Item -Path "$src\*" -Destination $temp -Recurse -Exclude $exclude

# 压缩
Compress-Archive -Path "$temp\*" -DestinationPath $dst -Force

# 清理
Remove-Item $temp -Recurse -Force

Write-Host "打包完成: $dst"
Write-Host "文件大小: $((Get-Item $dst).Length / 1MB) MB"