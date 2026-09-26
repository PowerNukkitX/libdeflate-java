@echo off

set "VcvarsArch=amd64"
set "PNX_ARCH=amd64"

if /I "%PROCESSOR_ARCHITECTURE%"=="ARM64" (
  set "VcvarsArch=arm64"
  set "PNX_ARCH=aarch64"
)

for /f "usebackq tokens=*" %%i in (`vswhere -products * -latest -find **/Auxiliary/Build/vcvarsall.bat`) do (
  set Vcvarsall=%%i
)

echo "%Vcvarsall%"

if exist "%Vcvarsall%" (
  call "%Vcvarsall%" %VcvarsArch%
  nmake /F Makefile.nmake clean all
)
