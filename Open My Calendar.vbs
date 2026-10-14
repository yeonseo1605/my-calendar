Set shell = CreateObject("WScript.Shell")
Set browserShell = CreateObject("Shell.Application")
folder = CreateObject("Scripting.FileSystemObject").GetParentFolderName(WScript.ScriptFullName)
shell.Run "cmd /c cd /d """ & folder & """ && node server.js", 0, False
WScript.Sleep 1000
browserShell.ShellExecute "http://127.0.0.1:8765/?v=19"
