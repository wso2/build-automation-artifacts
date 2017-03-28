#!/bin/bash

#CREATE MULTIPLE REPOS IN AN ORGANIZATION WITH GITIGONE AND LICENSE

#REQUEST FOR ORAGANIZATION NAME
read -p 'Oragnization Name: ' orgName

#SELECT PRIVATE OR PUBLIC TYPE REPO
read -p 'Private Repo [true/false]: ' repoType

#POINT TO THE FILE WITH REPO NAMES
repoList="<File Name>.txt"

count=0

cat $repoList | while read LINE

do

let count++

curl https://api.github.com/orgs/$orgName/repos?access_token=<TOKEN> -d "{\"name\":\"$LINE\", \"private\":$repoType, \"gitignore_template\":\"Java\", \"license_template\":\"apache-2.0\"}"

echo "Repository is created $LINE \n"

done
