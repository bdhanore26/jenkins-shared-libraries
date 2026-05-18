#!/usr/bin/env groovy

def call(Map config = [:]) {

    def imageTag = config.imageTag ?: error("Image tag is required")
    def manifestsPath = config.manifestsPath ?: 'kubernetes'
    def gitCredentials = config.gitCredentials ?: 'github-credentials'
    def gitUserName = config.gitUserName ?: 'Jenkins CI'
    def gitUserEmail = config.gitUserEmail ?: 'jenkins@example.com'

    echo "Updating Kubernetes manifests with image tag: ${imageTag}"

    withCredentials([
        usernamePassword(
            credentialsId: gitCredentials,
            usernameVariable: 'GIT_USERNAME',
            passwordVariable: 'GIT_PASSWORD'
        )
    ]) {

        sh """
        set -e

        git config user.name "${gitUserName}"
        git config user.email "${gitUserEmail}"

        echo "Updating Easyshop application image..."

        sed -i "s|image: .*easyshop-app:.*|image: bdhanore26/easyshop-app:${imageTag}|g" \
        ${manifestsPath}/08-easyshop-deployment.yaml

        if [ -f "${manifestsPath}/12-migration-job.yaml" ]; then

            echo "Updating migration image..."

            sed -i "s|image: .*easyshop-migration:.*|image: bdhanore26/easyshop-migration:${imageTag}|g" \
            ${manifestsPath}/12-migration-job.yaml
        fi

        echo "Checking for Kubernetes manifest changes..."

        if git diff --quiet; then
            echo "No changes detected"
            exit 0
        fi

        echo "Changes detected. Committing updates..."

        git add ${manifestsPath}/*.yaml

        git commit -m "[skip ci] Update image tags to ${imageTag}"

        echo "Pushing updated manifests to GitHub..."

        git push \
        https://${GIT_USERNAME}:${GIT_PASSWORD}@github.com/bdhanore26/e-commerce-app.git \
        HEAD:master

        echo "Kubernetes manifests updated successfully"
        """
    }
}
