pipeline {
    agent any

    environment {
        TOMCAT_WEBAPPS_WINDOWS = 'C:\\apache-tomcat-11.0.11\\webapps'
        TOMCAT_WEBAPPS_UNIX    = '/opt/homebrew/opt/tomcat/libexec/webapps'

        API_URL_WINDOWS = 'http://100.83.72.100:9999/criandoAPI/v1/actuator/health'
        API_URL_UNIX    = 'http://100.83.72.100:9999/criandoAPI/v1/actuator/health'
    }

    stages {

        stage('Checkout') {
            steps {
                echo 'Clonando repositório...'
                checkout scm
            }
        }

        stage('Build') {
            steps {
                script {
                    if (isUnix()) {
                        sh '''
                            export JAVA_HOME=$(/usr/libexec/java_home -v 21)
                            export PATH="$JAVA_HOME/bin:/opt/homebrew/bin:/usr/local/bin:$PATH"

                            chmod +x gradlew
                            ./gradlew clean build -x test
                        '''
                    } else {
                        bat '''
                            @echo off
                            echo Executando build com Gradle...

                            gradlew.bat clean build -x test
                        '''
                    }
                }
            }
        }

        stage('Deploy WAR to Tomcat') {
            when {
                expression {
                    def branch = (env.GIT_BRANCH ?: '').toLowerCase()

                    return branch == 'origin/main' ||
                           branch == 'origin/master'
                }
            }

            steps {
                script {
                    if (isUnix()) {
                        sh '''
                            echo "Procurando arquivo WAR..."

                            WAR_FILE=$(find build/libs -name "*.war" | head -n 1)

                            if [ -z "$WAR_FILE" ]; then
                                echo "ERRO: Nenhum WAR encontrado."
                                exit 1
                            fi

                            WAR_NAME=$(basename "$WAR_FILE")

                            echo "WAR encontrado: $WAR_NAME"

                            mkdir -p "$TOMCAT_WEBAPPS_UNIX"

                            cp -f "$WAR_FILE" \
                                "$TOMCAT_WEBAPPS_UNIX/$WAR_NAME"

                            echo "Deploy concluído com sucesso."
                        '''
                    } else {
                        bat '''
                            @echo off

                            echo Procurando arquivo WAR...

                            for %%F in ("%WORKSPACE%\\build\\libs\\*.war") do (
                                echo WAR encontrado: %%~nxF

                                copy /Y "%%~fF" "%TOMCAT_WEBAPPS_WINDOWS%\\%%~nxF"

                                if errorlevel 1 (
                                    echo ERRO: Falha ao copiar o WAR.
                                    exit /b 1
                                )

                                echo Deploy concluido com sucesso.
                                exit /b 0
                            )

                            echo ERRO: Nenhum WAR encontrado.
                            exit /b 1
                        '''
                    }
                }
            }
        }

        stage('Aguardar API iniciar') {
            when {
                expression {
                    def branch = (env.GIT_BRANCH ?: '').toLowerCase()

                    return branch == 'origin/main' ||
                           branch == 'origin/master'
                }
            }

            steps {
                script {
                    if (isUnix()) {
                        sh '''
                            echo "Aguardando API iniciar..."

                            for i in $(seq 1 12); do
                                echo "Tentativa $i de 12..."

                                if curl --silent \
                                        --fail \
                                        "$API_URL_UNIX" > /dev/null; then

                                    echo "API disponível."
                                    exit 0
                                fi

                                echo "API ainda não está disponível."

                                sleep 5
                            done

                            echo "ERRO: API não iniciou no tempo esperado."
                            exit 1
                        '''
                    } else {
                        bat '''
                            @echo off

                            echo Aguardando API iniciar...

                            for /L %%i in (1,1,12) do (
                                echo Tentativa %%i de 12...

                                curl.exe --silent --fail "%API_URL_WINDOWS%" >nul 2>&1

                                if not errorlevel 1 (
                                    echo API disponivel.
                                    exit /b 0
                                )

                                echo API ainda nao esta disponivel.

                                powershell.exe -NoProfile -Command "Start-Sleep -Seconds 5"
                            )

                            echo ERRO: API nao iniciou no tempo esperado.
                            exit /b 1
                        '''
                    }
                }
            }
        }

        stage('Executar pipeline de testes') {
            when {
                expression {
                    def branch = (env.GIT_BRANCH ?: '').toLowerCase()

                    return branch == 'origin/main' ||
                           branch == 'origin/master'
                }
            }

            steps {
                script {
                    echo 'Disparando pipeline testeCriandoAPI...'

                    def resultadoTestes = build(
                        job: 'testeCriandoAPI',
                        wait: true,
                        propagate: false
                    )

                    echo "Resultado dos testes: ${resultadoTestes.result}"
                    echo "Execução: ${resultadoTestes.fullDisplayName}"

                    if (resultadoTestes.result != 'SUCCESS') {
                        error(
                            "A pipeline testeCriandoAPI terminou com status: " +
                            "${resultadoTestes.result}"
                        )
                    }

                    echo 'Testes automatizados aprovados.'
                }
            }
        }
    }

    post {
        success {
            echo 'Build, deploy e testes concluídos com sucesso.'
        }

        failure {
            echo 'Falha no build, deploy ou testes automatizados.'
        }

        always {
            echo 'Pipeline concluída.'
        }
    }
}