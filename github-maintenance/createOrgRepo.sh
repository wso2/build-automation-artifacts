#!/bin/bash

#CREATE SINGLE REPOS IN AN ORGANIZATION WITH GITIGONE AND LICENSE

#REQUEST FOR ORAGANIZATION NAME
read -p 'Oragnization Name: ' orgName

#REQUEST FOR REQUIRED REPO NAME
read -p 'Repository Name: ' repoName

#SELECT PRIVATE OR PUBLIC TYPE REPO
read -p 'Private Repo [true/false]: ' repoType

curl https://api.github.com/orgs/$orgName/repos?access_token=<TOKEN> -d "{\"name\":\"$repoName\", \"private\":$repoType, \"gitignore_template\":\"Java\", \"license_template\":\"apache-2.0\"}"

echo "Repository is created"
