#!/bin/bash

#list repo details from organization. 
curl -i 'https://api.github.com/orgs/<ORGANIZATION_NAME>/repos?page=2&per_page=100' -o "<DETAIL_TEMP_FILE.txt>"

#filter full_name to temp file
grep full_name <DETAIL_TEMP_FILE>.txt >> <TEMP_FILE>.txt

#awk to filter required repo name
awk -F\" '/<REPO_NAME>/ {print $4}' <TEMP_FILE>.txt >> <REPO_LIST>.txt

echo "completed copying repo list!"

