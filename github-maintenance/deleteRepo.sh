#!/bin/bash
#DELETE MULTIPLE REPOS IN AN ORGANIZATION

#REQUEST FOR ORAGANIZATION NAME
read -p 'Organization Name: ' orgName


#POINT TO THE FILE WITH REPO NAMES
repoList="<File Name>.txt"

count=0

cat $repoList | while read LINE

do

let count++

curl -X DELETE -H 'Authorization: token <TOKEN>' https://api.github.com/repos/$orgName/$LINE

echo 'Repository deleted'

done

