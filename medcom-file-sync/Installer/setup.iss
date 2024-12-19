#define AppId "{{1a0eacc0-8770-4299-b6b6-d0cb7c54f14b}"
#define AppSourceDir "..\S3FileUploader\bin\Debug"
#define AppName "MedcomFileSync"
#define AppVersion "1.1.0"
#define AppPublisher "Digital Identity"
#define AppURL "https://digital-identity.dk/"

[Setup]
AppId={#AppId}
AppName={#AppName}
AppVersion={#AppVersion}
AppPublisher={#AppPublisher}
AppPublisherURL={#AppURL}
AppSupportURL={#AppURL}
AppUpdatesURL={#AppURL}
DefaultDirName={pf}\{#AppPublisher}\{#AppName}
DefaultGroupName={#AppName}
DisableProgramGroupPage=yes
OutputBaseFilename={#AppName}
Compression=lzma
SolidCompression=yes
SourceDir= {#SourcePath}\{#AppSourceDir}
OutputDir={#SourcePath}

[Languages]
Name: "english"; MessagesFile: "compiler:Default.isl"

[Files]
Source: "*.exe"; DestDir: "{app}"; Flags: ignoreversion
Source: "*.dll"; DestDir: "{app}"; Flags: ignoreversion
Source: "*.dll"; DestDir: "{app}"; Flags: ignoreversion
Source: "*.pdb"; DestDir: "{app}"; Flags: ignoreversion
Source: "*.xml"; DestDir: "{app}"; Flags: ignoreversion
Source: "*.config"; DestDir: "{app}"; Flags: ignoreversion
Source: "*.txt"; DestDir: "{app}"; Flags: ignoreversion
Source: "*.json"; DestDir: "{app}"; Flags: ignoreversion

[Run]
Filename: "{app}\{#AppName}.exe"; Parameters: "install --delayed" 

[UninstallRun]
Filename: "{app}\{#AppName}.exe"; Parameters: "uninstall"
