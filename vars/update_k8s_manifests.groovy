#!/usr/bin/env groovy

def call(Map config = [:]) {

    def imageTag =
        config.imageTag ?: error("Image tag required")

    def manifestsPath =
        config.manifestsPath ?: 'kubernetes'

    def gitCredentials =
        config.gitCredentials ?: 'github-credentials'


    withCredentials([

        usernamePassword(

            credentialsId: gitCredentials,

            usernameVariable: 'GIT_USERNAME',

            passwordVariable: 'GIT_PASSWORD'

        )

    ]) {


        sh """

        git config user.name "Jenkins CI"

        git config user.email "bdhanore26@gmail.com"



        sed -i \
"s|image: .*easyshop-app:.*|image: bdhanore26/easyshop-app:${imageTag}|g" \
${manifestsPath}/08-easyshop-deployment.yaml



        if [ -f "${manifestsPath}/12-migration-job.yaml" ]; then

            sed -i \
"s|image: .*easyshop-migration:.*|image: bdhanore26/easyshop-migration:${imageTag}|g" \
${manifestsPath}/12-migration-job.yaml

        fi



        git add ${manifestsPath}



        git commit \
-m "Update image tags to ${imageTag} [skip ci]"



        git push \
https://${GIT_USERNAME}:${GIT_PASSWORD}@github.com/bdhanore26/e-commerce-app.git \
HEAD:master

        """

    }

}
