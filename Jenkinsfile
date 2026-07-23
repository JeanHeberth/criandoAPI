pipeline {
    agent any

    environment {
        API_HEALTH_URL = 'http://100.83.72.100:9999/criandoAPI/v1/actuator/health'

        TOMCAT_WINDOWS = 'C:\\apache-tomcat-11.0.11'
        TOMCAT_MAC     = '/opt/homebrew/opt/tomcat/libexec'

        WAR_NAME = 'criandoAPI.war'
    }

    options {
        timestamps()
        disableConcurrentBuilds()
        skipDefaultCheckout(true)
    }

    stages {

        stage('Checkout') {
            steps {
                echo 'Baixando código da API...'
                checkout scm
            }
        }

        stage('Validar Java') {
            steps {
                script {
                    if (isUnix()) {
                        sh '''
                            echo "Validando Java..."
                            java -version
                        '''
                    } else {
                        bat '''
                            @echo off
                            echo Validando Java...
                            java -version
                        '''
                    }
                }
            }
        }

        stage('Build da API') {
            steps {
                script {
                    if (isUnix()) {
                        sh '''
                            echo "Gerando WAR da API..."

                            chmod +x gradlew

                            ./gradlew clean bootWar \
                                -x test \
                                --stacktrace \
                                --no-daemon
                        '''
                    } else {
                        bat '''
                            @echo off

                            echo Gerando WAR da API...

                            call gradlew.bat clean bootWar ^
                                -x test ^
                                --stacktrace ^
                                --no-daemon

                            exit /b %ERRORLEVEL%
                        '''
                    }
                }
            }
        }

        stage('Localizar WAR') {
            steps {
                script {
                    if (isUnix()) {
                        sh '''
                            echo "Arquivos WAR gerados:"
                            find build/libs -name "*.war" -type f
                        '''
                    } else {
                        bat '''
                            @echo off

                            echo Arquivos WAR gerados:
                            dir build\\libs\\*.war /b

                            if errorlevel 1 (
                                echo ERRO: Nenhum arquivo WAR encontrado.
                                exit /b 1
                            )
                        '''
                    }
                }
            }
        }

        stage('Deploy no Tomcat') {
            steps {
                script {
                    if (isUnix()) {
                        sh '''
                            echo "Parando Tomcat..."

                            "$TOMCAT_MAC/bin/shutdown.sh" || true

                            sleep 5

                            echo "Removendo versão anterior..."

                            rm -f "$TOMCAT_MAC/webapps/$WAR_NAME"
                            rm -rf "$TOMCAT_MAC/webapps/criandoAPI"

                            echo "Copiando novo WAR..."

                            WAR_FILE=$(find build/libs -name "*.war" -type f | head -n 1)

                            if [ -z "$WAR_FILE" ]; then
                                echo "ERRO: Arquivo WAR não encontrado."
                                exit 1
                            fi

                            cp "$WAR_FILE" "$TOMCAT_MAC/webapps/$WAR_NAME"

                            echo "Iniciando Tomcat..."

                            "$TOMCAT_MAC/bin/startup.sh"

                            echo "Deploy enviado para o Tomcat."
                        '''
                    } else {
                        bat '''
                            @echo off
                            setlocal enabledelayedexpansion

                            echo Parando servico do Tomcat...

                            net stop Tomcat11 >nul 2>&1

                            powershell.exe -NoProfile -Command ^
                                "Start-Sleep -Seconds 5"

                            echo Removendo versao anterior...

                            if exist "%TOMCAT_WINDOWS%\\webapps\\%WAR_NAME%" (
                                del /F /Q "%TOMCAT_WINDOWS%\\webapps\\%WAR_NAME%"
                            )

                            if exist "%TOMCAT_WINDOWS%\\webapps\\criandoAPI" (
                                rmdir /S /Q "%TOMCAT_WINDOWS%\\webapps\\criandoAPI"
                            )

                            set "WAR_FILE="

                            for %%F in (build\\libs\\*.war) do (
                                set "WAR_FILE=%%F"
                            )

                            if not defined WAR_FILE (
                                echo ERRO: Arquivo WAR nao encontrado.
                                exit /b 1
                            )

                            echo Copiando !WAR_FILE!...

                            copy /Y "!WAR_FILE!" ^
                                "%TOMCAT_WINDOWS%\\webapps\\%WAR_NAME%"

                            if errorlevel 1 (
                                echo ERRO: Falha ao copiar o WAR.
                                exit /b 1
                            )

                            echo Iniciando servico do Tomcat...

                            net start Tomcat11

                            if errorlevel 1 (
                                echo ERRO: Nao foi possivel iniciar o Tomcat.
                                exit /b 1
                            )

                            echo Deploy enviado para o Tomcat.

                            endlocal
                        '''
                    }
                }
            }
        }

        stage('Aguardar API iniciar') {
            steps {
                script {
                    if (isUnix()) {
                        sh '''
                            echo "Aguardando API iniciar..."

                            for i in $(seq 1 24); do {
                                echo "Tentativa $i de 24..."

                                if curl \
                                    --silent \
                                    --fail \
                                    "$API_HEALTH_URL" > /dev/null; then

                                    echo "API disponível."
                                    exit 0
                                fi

                                echo "API ainda não está disponível."
                                sleep 5
                            }

                            echo "ERRO: API não iniciou no tempo esperado."
                            exit 1
                        '''
                    } else {
                        bat '''
                            @echo off

                            echo Aguardando API iniciar...

                            for /L %%i in (1,1,24) do (
                                echo Tentativa %%i de 24...

                                curl.exe ^
                                    --silent ^
                                    --fail ^
                                    "%API_HEALTH_URL%" >nul 2>&1

                                if not errorlevel 1 (
                                    echo API disponivel.
                                    exit /b 0
                                )

                                echo API ainda nao esta disponivel.

                                powershell.exe -NoProfile -Command ^
                                    "Start-Sleep -Seconds 5"
                            )

                            echo ERRO: API nao iniciou no tempo esperado.
                            exit /b 1
                        '''
                    }
                }
            }
        }

        stage('Disparar Testes Automatizados') {
            steps {
                script {
                    echo 'Disparando pipeline testeCriandoAPI...'

                    try {
                        build(
                            job: 'testeCriandoAPI',
                            wait: false,
                            propagate: false
                        )

                        echo 'Pipeline de testes disparada de forma independente.'
                        echo 'A pipeline da API não aguardará o resultado dos testes.'
                    } catch (Exception exception) {
                        echo 'AVISO: não foi possível disparar a pipeline de testes.'
                        echo "Motivo: ${exception.message}"
                        echo 'O build e o deploy da API continuam válidos.'
                    }
                }
            }
        }
    }

    post {
        success {
            echo 'Build e deploy da API concluídos com sucesso.'
            echo 'Os testes automatizados estão sendo executados em outra pipeline.'
        }

        failure {
            echo 'A pipeline da API falhou durante build, deploy ou inicialização.'
            echo 'A pipeline de testes não será considerada responsável por esta falha.'
        }

        always {
            archiveArtifacts(
                artifacts: 'build/libs/*.war',
                allowEmptyArchive: true,
                fingerprint: true
            )

            echo 'Pipeline da API concluída.'
        }
    }
}