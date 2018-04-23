#!/usr/bin/env bash

#LIST ALL REPOS WHICH A TEAM HAS WRITE ACCESS.

curl -i 'https://api.github.com/teams/<GROUP_ID>/repos?access_token=<ACCESS_TOKEN>' -o "<FILE_NAME>"

echo "Completed copying team repo list!"

