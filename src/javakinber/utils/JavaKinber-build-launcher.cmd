@echo off
set DEST="C:\Program Files\JavaKinber"
C:
cd %DEST%
xcopy /Y D:\usr\src\JavaKinber\dist\JavaKinber.jar .
rmdir /S /Q lib
mkdir lib
xcopy /E /R D:\usr\src\JavaKinber\dist\lib\*.* lib\

"D:\Program Files\nsis\makensis.exe" /NOCD D:\usr\src\JavaKinber\src\JavaKinberLauncher.nsi

rmdir /S /Q S:\petrs\JavaKinber
xcopy /E /R %DEST%\*.* S:\petrs\JavaKinber\

pause
