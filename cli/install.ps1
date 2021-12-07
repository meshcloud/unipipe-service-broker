Clear-Host
Write-Host "`nStarting Installation of unipipe cli ..." -ForegroundColor Green

$ProgressPreference = "SilentlyContinue" # Don't remove this! Otherwise it will slow down the Download-Process under PS 5

# Check the latest release to get the exact download URL
try {
    $query = Invoke-RestMethod -Uri "https://api.github.com/repos/meshcloud/unipipe-service-broker/releases/latest"
    $downloadUrl = $query.assets | Where-Object { $_.browser_download_url -match "windows-msvc.exe" }
}
catch {
    throw "[ERROR] while downloading installation file. Check your Internet Connection: $(_.Exception.Message)!"
}

$installationPath = "${HOME}\unipipe-cli\$($query.name)"
Write-Host $($installationPath)

# Determine whether this is a fresh install or if unipipe has yet been installed before
if ( !(Test-Path -Path $installationPath) ) {
    $folder = New-Item -ItemType directory -Path "${HOME}\unipipe-cli\$($query.name)"
    if ($?) { Write-Host "[OK] Application-Folder '$installationPath' created!" -ForegroundColor Green }
}
elseif ( Test-Path -Path $($installationPath + "\unipipe.exe") ) {
    $replace = $(Write-Host "Replace existing Installation (y/n)? " -NoNewLine -ForegroundColor Green; Read-Host) 
    if ($replace -like "y") {
        Remove-Item -Path $installationPath -Recurse -Force
        $folder = New-Item -ItemType directory -Path "${HOME}\unipipe-cli\$($query.name)"
    }
    else { Exit } 
}

# Download unipipe cli and save it as an .exe
try {
    Write-Host "Downloading unipipe Version '$($query.Name)' ..." -ForegroundColor Green
    $download = Invoke-WebRequest -Uri $downloadUrl.browser_download_url -OutFile "$($folder.FullName)\unipipe.exe" -PassThru
    if ($?) { Write-Host "[OK] Download-Size '$([math]::Round($download.RawContentLength/1MB)),2 MB'`n" -ForegroundColor Green }
}
catch {
    throw "[ERROR] while downloading Installation File: $($($_.Exception).Message)!"
}

# Ask user whether to add unipipe to the environment variables automatically
$userenv = $(Write-Host "Adding unipipe to your Environment-Variables? (y/n)" -NoNewLine -ForegroundColor Green; Read-Host)
if ($userenv -like "y") {
    $Reg = "HKCU:Environment"
    $OldPath = (Get-ItemProperty -Path $Reg -Name PATH).Path
    $NewPath = $OldPath + ";" + $($folder.FullName).ToString()
    Set-ItemProperty -Path $Reg -Name PATH -Value $NewPath
    Write-Host "Reloading Powershell is required!`n" -ForegroundColor Yellow
}

Write-Host "[OK] unipipe CLI successfully installed: '$($folder.FullName)'`n" -ForegroundColor Green
# Done