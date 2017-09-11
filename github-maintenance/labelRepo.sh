#!/bin/bash

#ADD LABELS TO REPOS

#PROVIDE THE LOCATION TO THE FILE WITH LIST OF REPOS NAMES

repoList="/<PATH>/<FILE NAME>.txt"

FILELINE=`cat $repoList`

echo START

for LINE in $FILELINE ; do

#adding type
curl --include --request POST --data '{"name":"Type/Bug","color":"1d76db"}' "https://api.github.com/repos/$LINE/labels?access_token=<TOKEN>"
curl --include --request POST --data '{"name":"Type/New Feature","color":"1d76db"}' "https://api.github.com/repos/$LINE/labels?access_token=<TOKEN>"
curl --include --request POST --data '{"name":"Type/Epic","color":"1d76db"}' "https://api.github.com/repos/$LINE/labels?access_token=<TOKEN>"
curl --include --request POST --data '{"name":"Type/Improvement","color":"1d76db"}' "https://api.github.com/repos/$LINE/labels?access_token=<TOKEN>"
curl --include --request POST --data '{"name":"Type/Task","color":"1d76db"}' "https://api.github.com/repos/$LINE/labels?access_token=<TOKEN>"
curl --include --request POST --data '{"name":"Type/UX","color":"1d76db"}' "https://api.github.com/repos/$LINE/labels?access_token=<TOKEN>"
curl --include --request POST --data '{"name":"Type/Question","color":"1d76db"}' "https://api.github.com/repos/$LINE/labels?access_token=<TOKEN>"

#adding severity
curl --include --request POST --data '{"name":"Severity/Blocker","color":"b60205"}' "https://api.github.com/repos/$LINE/labels?access_token=<TOKEN>"
curl --include --request POST --data '{"name":"Severity/Critical","color":"b60205"}' "https://api.github.com/repos/$LINE/labels?access_token=<TOKEN>"
curl --include --request POST --data '{"name":"Severity/Major","color":"b60205"}' "https://api.github.com/repos/$LINE/labels?access_token=<TOKEN>"
curl --include --request POST --data '{"name":"Severity/Minor","color":"b60205"}' "https://api.github.com/repos/$LINE/labels?access_token=<TOKEN>"
curl --include --request POST --data '{"name":"Severity/Trivial","color":"b60205"}' "https://api.github.com/repos/$LINE/labels?access_token=<TOKEN>"

#adding priority
curl --include --request POST --data '{"name":"Priority/Highest","color":"ff9900"}' "https://api.github.com/repos/$LINE/labels?access_token=<TOKEN>"
curl --include --request POST --data '{"name":"Priority/High","color":"ff9900"}' "https://api.github.com/repos/$LINE/labels?access_token=<TOKEN>"
curl --include --request POST --data '{"name":"Priority/Normal","color":"ff9900"}' "https://api.github.com/repos/$LINE/labels?access_token=<TOKEN>"
curl --include --request POST --data '{"name":"Priority/Low","color":"ff9900"}' "https://api.github.com/repos/$LINE/labels?access_token=<TOKEN>"

#adding resolution
curl --include --request POST --data '{"name":"Resolution/Fixed","color":"93c47d"}' "https://api.github.com/repos/$LINE/labels?access_token=<TOKEN>"
curl --include --request POST --data '{"name":"Resolution/Won’t Fix","color":"93c47d"}' "https://api.github.com/repos/$LINE/labels?access_token=<TOKEN>"
curl --include --request POST --data '{"name":"Resolution/Duplicate","color":"93c47d"}' "https://api.github.com/repos/$LINE/labels?access_token=<TOKEN>"
curl --include --request POST --data '{"name":"Resolution/Cannot Reproduce","color":"93c47d"}' "https://api.github.com/repos/$LINE/labels?access_token=<TOKEN>"
curl --include --request POST --data '{"name":"Resolution/Not a bug","color":"93c47d"}' "https://api.github.com/repos/$LINE/labels?access_token=<TOKEN>"
curl --include --request POST --data '{"name":"Resolution/Invalid","color":"93c47d"}' "https://api.github.com/repos/$LINE/labels?access_token=<TOKEN>"
curl --include --request POST --data '{"name":"Resolution/Postponed","color":"93c47d"}' "https://api.github.com/repos/$LINE/labels?access_token=<TOKEN>"

echo "-------------- \n"
echo "Amended $LINE \n"

done




