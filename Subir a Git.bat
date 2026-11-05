@echo off
title Primera subida a GitHub

echo ====================================
echo INICIALIZANDO GIT
echo ====================================

git init

echo.
echo ====================================
echo AGREGANDO ARCHIVOS
echo ====================================

git add .

echo.
set /p MENSAJE=Mensaje del commit:

git commit -m "%MENSAJE%"

echo.
echo ====================================
echo CONFIGURANDO RAMA MAIN
echo ====================================

git branch -M main

echo.
echo ====================================
echo CONFIGURANDO REMOTO
echo ====================================

git remote remove origin 2>nul
git remote add origin git@github.com:jmanuelsanjass-tech/ATSButtonApp.git

echo.
echo ====================================
echo OBTENIENDO CAMBIOS REMOTOS
echo ====================================

git fetch origin

echo.
echo ====================================
echo ENVIANDO A GITHUB
echo ====================================

git push -u origin main --force

echo.
echo ====================================
echo PROCESO FINALIZADO
echo ====================================
pause