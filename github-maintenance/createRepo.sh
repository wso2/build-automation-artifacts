#!/bin/bash

#CREATE REPOS IN USERS ACCOUNT WITH GITIGONE AND LICENSE

#REQUEST FOR USER NAME
read -p 'User Name: ' userName

#REQUEST FOR REQUIRED REPO NAME
read -p 'Repository Name: ' repoName

#SELECT PRIVATE OR PUBLIC TYPE REPO
read -p 'Private Repo [true/false]: ' repoType

curl -u "$userName" https://api.github.com/user/repos -d "{\"name\":\"$repoName\", \"private\":$repoType, \"gitignore_template\":\"Java\", \"license_template\":\"apache-2.0\"}"

echo "Repository is created"



