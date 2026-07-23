pipeline {
    agent any

    environment {
        TOMCAT_WEBAPPS_WINDOWS = 'C:\\apache-tomcat-11.0.11\\webapps'
        TOMCAT_WEBAPPS_UNIX    = '/opt/homebrew/opt/tomcat/libexec/webapps'

        API_URL_WINDOWS = 'http://localhost:9999/criandoAPI/v1/actuator/health'
        API_URL_UNIX    = 'http://localhost:8080/criandoAPI/v1/actuator/health'
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
                        bat 'gradlew clean build -x test'
                    }
                }
            }
        }

        stage('Deploy WAR to Tomcat') {
            when {
                expression {
                    def gitBranch = (env.GIT_BRANCH ?: '').toLowerCase()

                    return gitBranch == 'origin/main' ||
                           gitBranch == 'origin/master'
                }
            }

            steps {
                script {
                    if (isUnix()) {
                        sh '''
                            export PATH="/opt/homebrew/bin:/usr/local/bin:$PATH"

                            echo "Procurando WAR..."

                            WAR_FILE=$(find build/libs -name "*.war" | head -n 1)

                            if [ -z "$WAR_FILE" ]; then
                                echo "ERRO: Nenhum WAR encontrado."
                                exit 1
                            fi

                            WAR_NAME=$(basename "$WAR_FILE")

                            echo "WAR encontrado: $WAR_NAME"

                            mkdir -p "$TOMCAT_WEBAPPS_UNIX"

                            cp -f "$WAR_FILE" "$TOMCAT_WEBAPPS_UNIX/$WAR_NAME"

                            echo "Deploy realizado com sucesso."
                        '''
                    } else {
                        bat '''
                            @echo off

                            echo Procurando arquivo WAR...

                            for %%F in ("%WORKSPACE%\\build\\libs\\*.war") do (
                                echo WAR encontrado: %%~nxF

                                copy /Y "%%~fF" "%TOMCAT_WEBAPPS_WINDOWS%\\%%~nxF"

                                echo Deploy concluido.

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
                    def gitBranch = (env.GIT_BRANCH ?: '').toLowerCase()

                    return gitBranch == 'origin/main' ||
                           gitBranch == 'origin/master'
                }
            }

            steps {
                script {
                    if (isUnix()) {
                        sh '''
                            echo "Aguardando API..."

                            for i in $(seq 1 12); do
                                echo "Tentativa $i de 12..."

                                if curl --silent --fail "$API_URL_UNIX" > /dev/null; then
                                    echo "API disponível."
                                    exit 0
                                fi

                                sleep 5
                            done

                            echo "ERRO: API não iniciou dentro do tempo esperado."
                            exit 1
                        '''
                    } else {
                        bat '''
                            @echo off

                            echo Aguardando API...

                            for /L %%i in (1,1,12) do (
                                echo Tentativa %%i de 12...

                                curl --silent --fail "%API_URL_WINDOWS%" >nul 2>&1

                                if not errorlevel 1 (
                                    echo API disponivel.
                                    exit /b 0
                                )

                                timeout /t 5 /nobreak >nul
                            )

                            echo ERRO: API nao iniciou dentro do tempo esperado.
                            exit /b 1
                        '''
                    }
                }
            }
        }

        stage('Executar pipeline de testes') {
            when {
                expression {
                    def gitBranch = (env.GIT_BRANCH ?: '').toLowerCase()

                    return gitBranch == 'origin/main' ||
                           gitBranch == 'origin/master'
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

                    echo "Resultado da pipeline de testes: ${resultadoTestes.result}"
                    echo "Execução: ${resultadoTestes.fullDisplayName}"

                    if (resultadoTestes.result != 'SUCCESS') {
                        error(
                            "A API foi publicada, mas a pipeline de testes terminou com status: " +
                            "${resultadoTestes.result}"
                        )
                    }

                    echo 'Todos os testes automatizados foram aprovados.'
                }
            }
        }
    }

    post {
        always {
            echo 'Pipeline concluída.'
        }

        success {
            echo 'Build, deploy e testes executados com sucesso!'
        }

        failure {
            echo 'Falha detectada no build, deploy ou nos testes automatizados.'
        }
    }
}