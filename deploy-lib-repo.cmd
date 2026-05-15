@echo off
setlocal EnableDelayedExpansion

echo ================================================
echo  将内部依赖部署到 lib-repo/（供 GitHub 分发）
echo ================================================
echo.
echo 本脚本由教师在修改内部依赖后运行，将最新 JAR 写入
echo lib-repo/ 目录，再 git commit 推送到 GitHub。
echo 学生克隆后 mvn package 可直接从 lib-repo/ 解析依赖，
echo 无需配置阿里云制品库。
echo.

:: 脚本所在目录即项目根目录
set ROOT=%~dp0
if "%ROOT:~-1%"=="\" set ROOT=%ROOT:~0,-1%
set LIB_REPO=%ROOT%\lib-repo

:: 将反斜杠转为正斜杠（Maven file:// URL 要求）
set "LIB_REPO_FWD=%LIB_REPO:\=/%"

echo 项目根目录: %ROOT%
echo lib-repo  : %LIB_REPO%
echo.

:: ─────────────────────────────────────────────────────────────────────────────
:: Step 1：编译各源模块（生成 target/*.jar）
:: ─────────────────────────────────────────────────────────────────────────────
echo [Step 1/2] 编译源模块...
echo.

echo --- coffee-common ---
cd /d "%ROOT%\coffee-common"
call mvn clean package -DskipTests -q
if errorlevel 1 ( echo [ERROR] coffee-common 编译失败 & exit /b 1 )

echo --- coffee-userorder/api ---
cd /d "%ROOT%\coffee-userorder\api"
call mvn clean package -DskipTests -q
if errorlevel 1 ( echo [ERROR] coffee-userorder-api 编译失败 & exit /b 1 )

echo --- coffee-expresstrack/api ---
cd /d "%ROOT%\coffee-expresstrack\api"
call mvn clean package -DskipTests -q
if errorlevel 1 ( echo [ERROR] coffee-expresstrack-api 编译失败 & exit /b 1 )

echo.
echo [Step 1/2] 编译完成。
echo.

:: ─────────────────────────────────────────────────────────────────────────────
:: Step 2：将 JAR/POM 部署到 lib-repo/
:: 注意：父 POM（packaging=pom）也需要发布，否则消费方解析依赖时找不到父模块
:: ─────────────────────────────────────────────────────────────────────────────
echo [Step 2/2] 发布到 lib-repo/...
echo.

:: 2-1. coffee-common（JAR + POM）
echo --- coffee-common ---
cd /d "%ROOT%\coffee-common"
call mvn deploy:deploy-file ^
  -Durl="file:///%LIB_REPO_FWD%" ^
  -DrepositoryId=project-lib-repo ^
  -Dfile=target/coffee-common-1.0-SNAPSHOT.jar ^
  -DpomFile=pom.xml ^
  -q
if errorlevel 1 ( echo [ERROR] coffee-common 发布失败 & exit /b 1 )

:: 2-2. coffee-userorder 父 POM（packaging=pom，只发布 POM 文件）
echo --- coffee-userorder（父 POM）---
cd /d "%ROOT%\coffee-userorder"
call mvn deploy:deploy-file ^
  -Durl="file:///%LIB_REPO_FWD%" ^
  -DrepositoryId=project-lib-repo ^
  -Dpackaging=pom ^
  -Dfile=pom.xml ^
  -DgroupId=com.coffee.yun ^
  -DartifactId=coffee-userorder ^
  -Dversion=1.0-SNAPSHOT ^
  -q
if errorlevel 1 ( echo [ERROR] coffee-userorder 父 POM 发布失败 & exit /b 1 )

:: 2-3. coffee-userorder-api（JAR + POM）
echo --- coffee-userorder-api ---
cd /d "%ROOT%\coffee-userorder\api"
call mvn deploy:deploy-file ^
  -Durl="file:///%LIB_REPO_FWD%" ^
  -DrepositoryId=project-lib-repo ^
  -Dfile=target/coffee-userorder-api-1.0-SNAPSHOT.jar ^
  -DpomFile=pom.xml ^
  -q
if errorlevel 1 ( echo [ERROR] coffee-userorder-api 发布失败 & exit /b 1 )

:: 2-4. coffee-expresstrack 父 POM
echo --- coffee-expresstrack（父 POM）---
cd /d "%ROOT%\coffee-expresstrack"
call mvn deploy:deploy-file ^
  -Durl="file:///%LIB_REPO_FWD%" ^
  -DrepositoryId=project-lib-repo ^
  -Dpackaging=pom ^
  -Dfile=pom.xml ^
  -DgroupId=com.coffee.yun ^
  -DartifactId=coffee-expresstrack ^
  -Dversion=1.0-SNAPSHOT ^
  -q
if errorlevel 1 ( echo [ERROR] coffee-expresstrack 父 POM 发布失败 & exit /b 1 )

:: 2-5. coffee-expresstrack-api（JAR + POM）
echo --- coffee-expresstrack-api ---
cd /d "%ROOT%\coffee-expresstrack\api"
call mvn deploy:deploy-file ^
  -Durl="file:///%LIB_REPO_FWD%" ^
  -DrepositoryId=project-lib-repo ^
  -Dfile=target/coffee-expresstrack-api-1.0-SNAPSHOT.jar ^
  -DpomFile=pom.xml ^
  -q
if errorlevel 1 ( echo [ERROR] coffee-expresstrack-api 发布失败 & exit /b 1 )

echo.
echo ================================================
echo  全部完成！lib-repo/ 已就绪。
echo ================================================
echo.
echo 下一步：提交 lib-repo/ 到 GitHub
echo.
echo   git add lib-repo/
echo   git commit -m "chore: 更新内部依赖 lib-repo"
echo   git push
echo.
