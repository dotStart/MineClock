; Product Definitions
!define PRODUCT_NAME "MineClock"
!define PRODUCT_VERSION "1.1"
!define PRODUCT_PUBLISHER "dotStart"
!define PRODUCT_WEB_SITE "https://github.com/LordAkkarin/MineClock"
!define PRODUCT_DIR_REGKEY "Software\Microsoft\Windows\CurrentVersion\App Paths\MineClock.exe"
!define PRODUCT_UNINST_KEY "Software\Microsoft\Windows\CurrentVersion\Uninstall\${PRODUCT_NAME}"
!define PRODUCT_UNINST_ROOT_KEY "HKLM"

; Java Variables
!DEFINE JAVA_VERSION "1.8"

SetCompressor lzma

!include "FileFunc.nsh"
!insertmacro GetFileVersion

!include "WordFunc.nsh"
!insertmacro VersionCompare

; MUI 1.67 compatible ------
!include "MUI.nsh"

; MUI Settings
!define MUI_ABORTWARNING
!define MUI_ICON "${NSISDIR}\Contrib\Graphics\Icons\modern-install.ico"
!define MUI_UNICON "${NSISDIR}\Contrib\Graphics\Icons\modern-uninstall.ico"

; Welcome page
!insertmacro MUI_PAGE_WELCOME
; License page
!insertmacro MUI_PAGE_LICENSE "LICENSE"
; Directory page
!insertmacro MUI_PAGE_DIRECTORY
; Instfiles page
!insertmacro MUI_PAGE_INSTFILES
; Finish page
!define MUI_FINISHPAGE_RUN "$INSTDIR\MineClock.exe"
!insertmacro MUI_PAGE_FINISH

; Uninstaller pages
!insertmacro MUI_UNPAGE_INSTFILES

; Language files
!insertmacro MUI_LANGUAGE "English"

; Reserve files
!insertmacro MUI_RESERVEFILE_INSTALLOPTIONS

; MUI end ------

Name "${PRODUCT_NAME} ${PRODUCT_VERSION}"
OutFile "MineClockSetup.exe"
InstallDir "$PROGRAMFILES\MineClock"
InstallDirRegKey HKLM "${PRODUCT_DIR_REGKEY}" ""
ShowInstDetails show
ShowUnInstDetails show

Section -Prerequisites
  Call CheckJRE
  Call CheckJDK
SectionEnd

Section "MainSection" SEC01
  SetOutPath "$INSTDIR"
  SetOverwrite ifnewer
  File "ui\target\MineClock.exe"
  CreateDirectory "$SMPROGRAMS\MineClock"
  CreateShortCut "$SMPROGRAMS\MineClock\MineClock.lnk" "$INSTDIR\MineClock.exe"
  CreateShortCut "$DESKTOP\MineClock.lnk" "$INSTDIR\MineClock.exe"
  File "ui\target\MineClock.jar"
  SetOutPath "$INSTDIR\lib"
  SetOverwrite try
  File "ui\target\lib\rocks.spud.minecraft.mineclock.attach.jar"
SectionEnd

Section -AdditionalIcons
  SetOutPath $INSTDIR
  WriteIniStr "$INSTDIR\${PRODUCT_NAME}.url" "InternetShortcut" "URL" "${PRODUCT_WEB_SITE}"
  CreateShortCut "$SMPROGRAMS\MineClock\Website.lnk" "$INSTDIR\${PRODUCT_NAME}.url"
  CreateShortCut "$SMPROGRAMS\MineClock\Uninstall.lnk" "$INSTDIR\uninst.exe"
SectionEnd

Section -Post
  WriteUninstaller "$INSTDIR\uninst.exe"
  WriteRegStr HKLM "${PRODUCT_DIR_REGKEY}" "" "$INSTDIR\MineClock.exe"
  WriteRegStr ${PRODUCT_UNINST_ROOT_KEY} "${PRODUCT_UNINST_KEY}" "DisplayName" "$(^Name)"
  WriteRegStr ${PRODUCT_UNINST_ROOT_KEY} "${PRODUCT_UNINST_KEY}" "UninstallString" "$INSTDIR\uninst.exe"
  WriteRegStr ${PRODUCT_UNINST_ROOT_KEY} "${PRODUCT_UNINST_KEY}" "DisplayIcon" "$INSTDIR\MineClock.exe"
  WriteRegStr ${PRODUCT_UNINST_ROOT_KEY} "${PRODUCT_UNINST_KEY}" "DisplayVersion" "${PRODUCT_VERSION}"
  WriteRegStr ${PRODUCT_UNINST_ROOT_KEY} "${PRODUCT_UNINST_KEY}" "URLInfoAbout" "${PRODUCT_WEB_SITE}"
  WriteRegStr ${PRODUCT_UNINST_ROOT_KEY} "${PRODUCT_UNINST_KEY}" "Publisher" "${PRODUCT_PUBLISHER}"
SectionEnd

; JDK
Function CheckJDK
  PUSH $R0
  PUSH $R1
  PUSH $2
  
  DetailPrint "Searching for JDK installation ..."

  ; 1) Check whether JAVA_HOME points at a JDK
  ClearErrors
  ReadEnvStr $R0 "JAVA_HOME"
  StrCpy $R0 "$R0\bin\javac.exe"
  IfErrors CheckRegistry
  IfFileExists $R0 0 CheckRegistry
  Call CheckJDKVersion
  IfErrors CheckRegistry JdkFound
    
  ; 2) Check whether the registry points at a JDK Installation
  CheckRegistry:
    ClearErrors
    ReadRegStr $R1 HKLM "SOFTWARE\JavaSoft\Java Development Kit" "CurrentVersion"
    ReadRegStr $R0 HKLM "SOFTWARE\JavaSoft\Java Development Kit\$R1" "JavaHome"
    StrCpy $R0 "$R0\bin\javac.exe"
    IfErrors WarnUser
    IfFileExists $R0 0 WarnUser
    Call CheckJDKVersion
    IfErrors WarnUser JdkFound
    
  WarnUser:
    MessageBox MB_ICONQUESTION|MB_YESNO "This application may optionally make use of the Java Development Kit. Do you wish to download it?" IDNO JdkFound
    ExecShell "open" "http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html"

  JdkFound:
    Pop $2
    Pop $R1
    Exch $R0
FunctionEnd

Function CheckJDKVersion
    Push $R1
    
    DetailPrint "Java Compiler located at $R0"
    
    ${GetFileVersion} $R0 $R1
    DetailPrint "Java Development Kit reports version $R1"
    ${VersionCompare} ${JAVA_VERSION} $R1 $R1
    
    ClearErrors
    StrCmp $R1 "1" 0 CheckDone
    SetErrors
    
    CheckDone:
      DetailPrint "Java Development Kit seems to be compatible"
      Pop $R1
FunctionEnd

; JRE
Function CheckJRE
  PUSH $R0
  PUSH $R1

  DetailPrint "Searching for Java installation ..."

  ; 1) Check whether JAVA_HOME points at a JRE
  ClearErrors
  ReadEnvStr $R0 "JAVA_HOME"
  StrCpy $R0 "$R0\bin\javaw.exe"
  IfErrors CheckRegistry
  IfFileExists $R0 0 CheckRegistry
  Call CheckJREVersion
  IfErrors CheckRegistry JreFound

  ; 2) Check whether the registry points at a JDK Installation
  CheckRegistry:
    ClearErrors
    ReadRegStr $R1 HKLM "SOFTWARE\JavaSoft\Java Runtime Environment" "CurrentVersion"
    ReadRegStr $R0 HKLM "SOFTWARE\JavaSoft\Java Runtime Environment\$R1" "JavaHome"
    StrCpy $R0 "$R0\bin\javaw.exe"
    IfErrors WarnUser
    IfFileExists $R0 0 WarnUser
    Call CheckJREVersion
    IfErrors WarnUser JreFound

  WarnUser:
    MessageBox MB_ICONSTOP|MB_OK "Could not locate Java Runtime Envionment 8.0 or newer"
    Abort

  JreFound:
    Pop $R1
    Exch $R0
FunctionEnd

Function CheckJREVersion
    Push $R1
    
    DetailPrint "Java located at $R0"

    ${GetFileVersion} $R0 $R1
    DetailPrint "Java reports version $R1"
    ${VersionCompare} ${JAVA_VERSION} $R1 $R1

    ClearErrors
    StrCmp $R1 "1" 0 CheckDone
    SetErrors

    CheckDone:
      DetailPrint "Java seems to be compatible"
      Pop $R1
FunctionEnd

; Uninstaller
Function un.onUninstSuccess
  HideWindow
  MessageBox MB_ICONINFORMATION|MB_OK "MineClock was successfully removed from your computer."
FunctionEnd

Function un.onInit
  MessageBox MB_ICONQUESTION|MB_YESNO|MB_DEFBUTTON2 "Are you sure you want to completely remove MineClock and all of its components?" IDYES +2
  Abort
FunctionEnd

Section Uninstall
  Delete "$INSTDIR\${PRODUCT_NAME}.url"
  Delete "$INSTDIR\uninst.exe"
  Delete "$INSTDIR\lib\rocks.spud.minecraft.mineclock.attach.jar"
  Delete "$INSTDIR\MineClock.jar"
  Delete "$INSTDIR\MineClock.exe"

  Delete "$SMPROGRAMS\MineClock\Uninstall.lnk"
  Delete "$SMPROGRAMS\MineClock\Website.lnk"
  Delete "$DESKTOP\MineClock.lnk"
  Delete "$SMPROGRAMS\MineClock\MineClock.lnk"

  RMDir "$SMPROGRAMS\MineClock"
  RMDir "$INSTDIR\lib"
  RMDir "$INSTDIR"

  DeleteRegKey ${PRODUCT_UNINST_ROOT_KEY} "${PRODUCT_UNINST_KEY}"
  DeleteRegKey HKLM "${PRODUCT_DIR_REGKEY}"
  SetAutoClose true
SectionEnd