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
            steps {
                script {

                    if (isUnix()) {

                        sh '''
                            echo "Aguardando API..."

                            for i in {1..12}; do

                                if curl --silent --fail "$API_URL_UNIX" >/dev/null; then
                                    echo "API disponível."
                                    exit 0
                                fi

                                echo "Tentativa $i de 12..."
                                sleep 5

                            done

                            echo "API não iniciou."
                            exit 1
                        '''

                    } else {

                        bat '''
                            @echo off

                            echo Aguardando API...

                            for /L %%i in (1,1,12) do (

                                curl --silent --fail %API_URL_WINDOWS% >nul

                                if not errorlevel 1 (
                                    echo API disponível.
                                    exit /b 0
                                )

                                echo Tentativa %%i de 12...
                                timeout /t 5 /nobreak >nul
                            )

                            echo API não iniciou.
                            exit /b 1
                        '''
                    }
                }
            }
        }
    }

post {

    always {
        echo 'Pipeline da API concluída.'
    }

    success {
        script {
            echo 'Build e Deploy executados com sucesso!'
            echo 'Disparando pipeline de testes...'

            def resultadoTestes = build(
                job: 'testeCriandoAPI',
                wait: true,
                propagate: false
            )

            echo "Resultado da pipeline de testes: ${resultadoTestes.result}"
            echo "Execução dos testes: ${resultadoTestes.fullDisplayName}"

            if (resultadoTestes.result == 'SUCCESS') {
                echo 'API publicada e testes automatizados aprovados!'
            } else {
                error("""
                    A API foi publicada, mas os testes automatizados falharam.

                    Pipeline: ${resultadoTestes.fullDisplayName}
                    Resultado: ${resultadoTestes.result}

                    Consulte o Console Output da pipeline testeCriandoAPI.
                """)
            }
        }
    }

    failure {
        echo 'Falha durante o build, deploy ou execução dos testes.'
    }
}