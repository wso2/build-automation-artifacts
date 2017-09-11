#!/bin/bash

#UPDATE LABELS TO REPOS

#PROVIDE THE LOCATION TO THE FILE WITH LIST OF REPOS NAMES

repoList="/<PATH>/<FILE NAME>.txt"

FILELINE=`cat $repoList`

echo START

for LINE in $FILELINE ; do

#adding type
#replace label bug
curl --include --request PATCH --data '{"name":"Type/Bug","color":"1d76db"}' "https://api.github.com/repos/$LINE/labels/bug?access_token=<TOKEN>"

#replace label severity
curl --include --request PATCH --data '{"name":"Severity/Blocker","color":"b60205"}' "https://api.github.com/repos/$LINE/labels/blocker?access_token=<TOKEN>"
curl --include --request PATCH --data '{"name":"Severity/Critical","color":"b60205"}' "https://api.github.com/repos/$LINE/labels/critical?access_token=<TOKEN>"
curl --include --request PATCH --data '{"name":"Severity/Major","color":"b60205"}' "https://api.github.com/repos/$LINE/labels/major?access_token=<TOKEN>"
curl --include --request PATCH --data '{"name":"Severity/Minor","color":"b60205"}' "https://api.github.com/repos/$LINE/labels/minor?access_token=<TOKEN>"
curl --include --request PATCH --data '{"name":"Severity/Trivial","color":"b60205"}' "https://api.github.com/repos/$LINE/labels/trivial?access_token=<TOKEN>"

echo "-------------- \n"
echo "Amended $LINE \n"

done




