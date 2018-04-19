# Setting Ctrl+D to exit
Set-PSReadlineKeyHandler -Key Ctrl+d -Function DeleteCharOrExit

# Some aliasses
set-alias subl "C:\Program Files\Sublime Text 3\sublime_text.exe"
set-alias grep select-string
set-alias ssh New-PSSecureRemoteSession
set-alias sh New-PSRemoteSession
set-alias l ls
set-alias b "C:\Users\baronesc\bin\bash.ps1"
set-alias vim "C:\Program Files (x86)\Vim\vim80\vim.exe"
set-alias open Start-Process
Set-PSReadlineOption -BellStyle None
Import-Module PSReadLine
Set-PSReadLineKeyHandler -Key Tab -Function Complete
