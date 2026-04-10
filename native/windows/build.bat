@echo off
REM Build the Windows WebView helper binary.
REM Requires: .NET 6+ SDK, WebView2 Runtime (ships with Windows 10/11)
REM
REM Usage: build.bat
REM Output: publish/WebViewHelper.exe (self-contained single-file)

echo Building webview-helper for Windows...

REM Create project if it doesn't exist
if not exist "WebViewHelper.csproj" (
    dotnet new console -n WebViewHelper --output . --force
    dotnet add package Microsoft.Web.WebView2
)

REM Copy our source
copy /Y WebViewHelper.cs Program.cs >nul

REM Publish as single-file self-contained
dotnet publish -c Release -r win-x64 --self-contained -p:PublishSingleFile=true -o publish

echo.
echo Built: publish/WebViewHelper.exe
echo.
echo To install, copy to your Minecraft directory:
echo   mkdir config\not-riding-alert\native
echo   copy publish\WebViewHelper.exe config\not-riding-alert\native\webview-helper.exe
