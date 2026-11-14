; Compile with scripts/installer.ps1 after building the jpackage app-image.
#ifndef AppVersion
  #define AppVersion "0.1.1"
#endif
#ifndef AppImage
  #define AppImage SourcePath + "..\target\distribution\Missanye"
#endif
#if !FileExists(AppImage + "\Missanye.exe")
  #error "Missanye.exe absent : construire d'abord l'image Windows avec jpackage."
#endif

[Setup]
; Keep this identifier across releases so upgrades use the same application.
AppId={{E6849DEB-2845-461C-AB8B-386A8F635DD9}
AppName=Missanye
AppVersion={#AppVersion}
AppPublisher=Missanye
DefaultDirName={commonpf}\Missanye
DefaultGroupName=Missanye
DisableProgramGroupPage=yes
PrivilegesRequired=admin
DisableDirPage=no
UsePreviousAppDir=yes
ArchitecturesAllowed=x64compatible
ArchitecturesInstallIn64BitMode=x64compatible
MinVersion=10.0
OutputDir=..\target\installer
OutputBaseFilename=Missanye-Setup
Compression=lzma2
SolidCompression=yes
WizardStyle=modern
SetupIconFile=..\branding\missanye.ico
UninstallDisplayIcon={app}\Missanye.exe
CloseApplications=yes
CloseApplicationsFilter=Missanye.exe
RestartApplications=no
SetupLogging=yes

[Languages]
Name: "french"; MessagesFile: "compiler:Languages\French.isl"
Name: "english"; MessagesFile: "compiler:Default.isl"

[Tasks]
Name: "desktopicon"; Description: "{cm:CreateDesktopIcon}"; GroupDescription: "{cm:AdditionalIcons}"; Flags: unchecked

[Files]
; Include the complete application and its bundled Java/JavaFX runtime.
Source: "{#AppImage}\*"; DestDir: "{app}"; Flags: ignoreversion recursesubdirs createallsubdirs

[Icons]
Name: "{autoprograms}\Missanye"; Filename: "{app}\Missanye.exe"; WorkingDir: "{app}"
Name: "{autodesktop}\Missanye"; Filename: "{app}\Missanye.exe"; WorkingDir: "{app}"; Tasks: desktopicon

[Run]
Filename: "{app}\Missanye.exe"; Description: "{cm:LaunchProgram,Missanye}"; Flags: nowait postinstall skipifsilent

Filename: "{app}\Missanye.exe"; Flags: nowait skipifnotsilent; Check: IsIntegratedUpdate

; An integrated upgrade relaunches the app after files are replaced.
[Code]
function IsIntegratedUpdate: Boolean;
begin
  Result := ExpandConstant('{param:MISSANYEUPDATE|0}') = '1';
end;

; Do not delete ~/.missanye-desktop: uninstalling must preserve the user's vault.
