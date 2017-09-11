#!/bin/bash

#REMOVE LABELS TO REPOS

#PROVIDE THE LOCATION TO THE FILE WITH LIST OF REPOS NAMES

repoList="/<PATH>/<FILE NAME>.txt"

FILELINE=`cat $repoList`

echo START

for LINE in $FILELINE ; do

#deleteing type/bug
curl --include --request DELETE "https://api.github.com/repos/$LINE/labels/Type/Bug?access_token=<TOKEN>"

#echo "Deleted $LINE Label:Type/Bug \n"

#deleteing severity
curl --include --request DELETE "https://api.github.com/repos/$LINE/labels/Severity/Blocker?access_token=<TOKEN>"
curl --include --request DELETE "https://api.github.com/repos/$LINE/labels/Severity/Critical?access_token=<TOKEN>"
curl --include --request DELETE "https://api.github.com/repos/$LINE/labels/Severity/Major?access_token=<TOKEN>"
curl --include --request DELETE "https://api.github.com/repos/$LINE/labels/Severity/Minor?access_token=<TOKEN>"
curl --include --request DELETE "https://api.github.com/repos/$LINE/labels/Severity/Trivial?access_token=<TOKEN>"

#echo "Deleted $LINE Labels:Severity/Blocker,Severity/Critical,Severity/Major,Severity/Minor and Severity/Trivial \n"

echo "-------------- \n"
echo "Amended $LINE \n"

done




