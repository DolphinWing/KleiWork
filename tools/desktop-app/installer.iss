; Inno Setup 6 script for OniTranslator (Compose Desktop createDistributable app image).
; Required preprocessor defines passed by Gradle task :packageInnoSetup:
;   /DAppVersion=4.2.2
;   /DSourceDir=<path to build\compose\binaries\main\app\OniTranslator>
;   /DOutputDir=<path to build\installer>
;   /DIconFile=<path to src\main\resources\nisbet_ponder.ico>
; Optional:
;   /DHasZhTwLang (defined when compiler:Languages\ChineseTraditional.isl exists)

#ifndef AppVersion
  #error AppVersion must be defined via /DAppVersion=...
#endif
#ifndef SourceDir
  #error SourceDir must be defined via /DSourceDir=...
#endif
#ifndef OutputDir
  #error OutputDir must be defined via /DOutputDir=...
#endif
#ifndef IconFile
  #error IconFile must be defined via /DIconFile=...
#endif
#ifndef AppPublisher
  #define AppPublisher "DolphinWing"
#endif
#ifndef AppDescription
  #define AppDescription "ONI PO Translate Helper"
#endif
#ifndef AppCopyright
  #define AppCopyright "Copyright (c) 2026 DolphinWing"
#endif

[Setup]
; NOTE: AppId must remain unchanged across future Inno Setup releases so upgrades overwrite cleanly.
; Note that WiX (packageExe) and Inno Setup use different installer engines and cannot upgrade each other.
AppId={{f33ea1be-e738-43e0-9918-9360b0620fc0}
AppName=OniTranslator
AppVersion={#AppVersion}
AppVerName=OniTranslator {#AppVersion}
AppPublisher={#AppPublisher}
AppCopyright={#AppCopyright}
VersionInfoVersion={#AppVersion}
VersionInfoProductVersion={#AppVersion}
VersionInfoProductName=OniTranslator
VersionInfoCompany={#AppPublisher}
VersionInfoDescription={#AppDescription} Setup
VersionInfoCopyright={#AppCopyright}
DefaultDirName={autopf}\OniTranslator
DefaultGroupName=OniTranslator
DisableProgramGroupPage=yes
; Per-user non-UAC install (%LOCALAPPDATA%\Programs\OniTranslator)
PrivilegesRequired=lowest
OutputDir={#OutputDir}
OutputBaseFilename=OniTranslator-Setup-{#AppVersion}
SetupIconFile={#IconFile}
UninstallDisplayIcon={app}\OniTranslator.exe
UninstallDisplayName=OniTranslator
Compression=lzma2/ultra64
SolidCompression=yes
LZMAUseSeparateProcess=yes
WizardStyle=modern
CloseApplications=yes
CloseApplicationsFilter=*.exe
RestartApplications=no
#if VER >= 0x06030000
ArchitecturesAllowed=x64compatible
ArchitecturesInstallIn64BitMode=x64compatible
#else
ArchitecturesAllowed=x64
ArchitecturesInstallIn64BitMode=x64
#endif

[Languages]
#ifdef HasZhTwLang
Name: "chinesetraditional"; MessagesFile: "compiler:Languages\ChineseTraditional.isl"
#endif
Name: "english"; MessagesFile: "compiler:Default.isl"

[InstallDelete]
; Remove previous hashed JARs and bundled runtime before extracting the new release.
; User configs live separately in %LOCALAPPDATA%\oni-translator\ and are never touched.
Type: filesandordirs; Name: "{app}\app"
Type: filesandordirs; Name: "{app}\runtime"

[Files]
Source: "{#SourceDir}\*"; DestDir: "{app}"; Flags: ignoreversion recursesubdirs createallsubdirs

[Icons]
Name: "{autoprograms}\OniTranslator"; Filename: "{app}\OniTranslator.exe"; WorkingDir: "{app}"

[Run]
Filename: "{app}\OniTranslator.exe"; Description: "{cm:LaunchProgram,OniTranslator}"; Flags: nowait postinstall skipifsilent unchecked
