#!/usr/bin/env bash

#USED TO ADD LABELS, PR TEMPLATE, ISSUE TEMPLATE AND PROTECT REPOS FOR NEW REPO.

#GIVE REPO NAME ALONG WITH THE ORGANIZATION NAME

LINE='<ORGANIZATION_NAME>/<REPO_NAME>'

echo START

#ADDING LABELS

#adding type
curl --include --request POST --data '{"name":"Type/Bug","color":"1d76db"}' "https://api.github.com/repos/$LINE/labels?access_token=<ACCESS_TOKEN>"
curl --include --request POST --data '{"name":"Type/New Feature","color":"1d76db"}' "https://api.github.com/repos/$LINE/labels?access_token=<ACCESS_TOKEN>"
curl --include --request POST --data '{"name":"Type/Epic","color":"1d76db"}' "https://api.github.com/repos/$LINE/labels?access_token=<ACCESS_TOKEN>"
curl --include --request POST --data '{"name":"Type/Improvement","color":"1d76db"}' "https://api.github.com/repos/$LINE/labels?access_token=<ACCESS_TOKEN>"
curl --include --request POST --data '{"name":"Type/Task","color":"1d76db"}' "https://api.github.com/repos/$LINE/labels?access_token=<ACCESS_TOKEN>"
curl --include --request POST --data '{"name":"Type/UX","color":"1d76db"}' "https://api.github.com/repos/$LINE/labels?access_token=<ACCESS_TOKEN>"
curl --include --request POST --data '{"name":"Type/Question","color":"1d76db"}' "https://api.github.com/repos/$LINE/labels?access_token=<ACCESS_TOKEN>"
curl --include --request POST --data '{"name":"Type/Docs","color":"1d76db"}' "https://api.github.com/repos/$LINE/labels?access_token=<ACCESS_TOKEN>"

#adding severity
curl --include --request POST --data '{"name":"Severity/Blocker","color":"b60205"}' "https://api.github.com/repos/$LINE/labels?access_token=<ACCESS_TOKEN>"
curl --include --request POST --data '{"name":"Severity/Critical","color":"b60205"}' "https://api.github.com/repos/$LINE/labels?access_token=<ACCESS_TOKEN>"
curl --include --request POST --data '{"name":"Severity/Major","color":"b60205"}' "https://api.github.com/repos/$LINE/labels?access_token=<ACCESS_TOKEN>"
curl --include --request POST --data '{"name":"Severity/Minor","color":"b60205"}' "https://api.github.com/repos/$LINE/labels?access_token=<ACCESS_TOKEN>"
curl --include --request POST --data '{"name":"Severity/Trivial","color":"b60205"}' "https://api.github.com/repos/$LINE/labels?access_token=<ACCESS_TOKEN>"

#adding priority
curl --include --request POST --data '{"name":"Priority/Highest","color":"ff9900"}' "https://api.github.com/repos/$LINE/labels?access_token=<ACCESS_TOKEN>"
curl --include --request POST --data '{"name":"Priority/High","color":"ff9900"}' "https://api.github.com/repos/$LINE/labels?access_token=<ACCESS_TOKEN>"
curl --include --request POST --data '{"name":"Priority/Normal","color":"ff9900"}' "https://api.github.com/repos/$LINE/labels?access_token=<ACCESS_TOKEN>"
curl --include --request POST --data '{"name":"Priority/Low","color":"ff9900"}' "https://api.github.com/repos/$LINE/labels?access_token=<ACCESS_TOKEN>"

#adding resolution
curl --include --request POST --data '{"name":"Resolution/Fixed","color":"93c47d"}' "https://api.github.com/repos/$LINE/labels?access_token=<ACCESS_TOKEN>"
curl --include --request POST --data '{"name":"Resolution/Won’t Fix","color":"93c47d"}' "https://api.github.com/repos/$LINE/labels?access_token=<ACCESS_TOKEN>"
curl --include --request POST --data '{"name":"Resolution/Duplicate","color":"93c47d"}' "https://api.github.com/repos/$LINE/labels?access_token=<ACCESS_TOKEN>"
curl --include --request POST --data '{"name":"Resolution/Cannot Reproduce","color":"93c47d"}' "https://api.github.com/repos/$LINE/labels?access_token=<ACCESS_TOKEN>"
curl --include --request POST --data '{"name":"Resolution/Not a bug","color":"93c47d"}' "https://api.github.com/repos/$LINE/labels?access_token=<ACCESS_TOKEN>"
curl --include --request POST --data '{"name":"Resolution/Invalid","color":"93c47d"}' "https://api.github.com/repos/$LINE/labels?access_token=<ACCESS_TOKEN>"
curl --include --request POST --data '{"name":"Resolution/Postponed","color":"93c47d"}' "https://api.github.com/repos/$LINE/labels?access_token=<ACCESS_TOKEN>"

#adding issue template.please change the branch if "master" branch is not the default
curl -i -X PUT -H 'Authorization: token <ACCESS_TOKEN>' -d '{"path": "issue_template.md", "message": "Add Issue Template", "committer": {"name": "maheshika", "email": "maheshika@wso2.com"}, "content": "KipEZXNjcmlwdGlvbjoqKg0KPCEtLSBHaXZlIGEgYnJpZWYgZGVzY3JpcHRpb24gb2YgdGhlIGlzc3VlIC0tPg0KDQoqKlN1Z2dlc3RlZCBMYWJlbHM6KioNCjwhLS0gT3B0aW9uYWwgY29tbWEgc2VwYXJhdGVkIGxpc3Qgb2Ygc3VnZ2VzdGVkIGxhYmVscy4gTm9uIGNvbW1pdHRlcnMgY2Fu4oCZdCBhc3NpZ24gbGFiZWxzIHRvIGlzc3Vlcywgc28gdGhpcyB3aWxsIGhlbHAgaXNzdWUgY3JlYXRvcnMgd2hvIGFyZSBub3QgYSBjb21taXR0ZXIgdG8gc3VnZ2VzdCBwb3NzaWJsZSBsYWJlbHMtLT4NCg0KKipTdWdnZXN0ZWQgQXNzaWduZWVzOioqDQo8IS0tT3B0aW9uYWwgY29tbWEgc2VwYXJhdGVkIGxpc3Qgb2Ygc3VnZ2VzdGVkIHRlYW0gbWVtYmVycyB3aG8gc2hvdWxkIGF0dGVuZCB0aGUgaXNzdWUuIE5vbiBjb21taXR0ZXJzIGNhbuKAmXQgYXNzaWduIGlzc3VlcyB0byBhc3NpZ25lZXMsIHNvIHRoaXMgd2lsbCBoZWxwIGlzc3VlIGNyZWF0b3JzIHdobyBhcmUgbm90IGEgY29tbWl0dGVyIHRvIHN1Z2dlc3QgcG9zc2libGUgYXNzaWduZWVzLS0+DQoNCioqQWZmZWN0ZWQgUHJvZHVjdCBWZXJzaW9uOioqDQoNCioqT1MsIERCLCBvdGhlciBlbnZpcm9ubWVudCBkZXRhaWxzIGFuZCB2ZXJzaW9uczoqKiAgICANCg0KKipTdGVwcyB0byByZXByb2R1Y2U6KioNCg0KDQoqKlJlbGF0ZWQgSXNzdWVzOioqDQo8IS0tIEFueSByZWxhdGVkIGlzc3VlcyBzdWNoIGFzIHN1YiB0YXNrcywgaXNzdWVzIHJlcG9ydGVkIGluIG90aGVyIHJlcG9zaXRvcmllcyAoZS5nIGNvbXBvbmVudCByZXBvc2l0b3JpZXMpLCBzaW1pbGFyIHByb2JsZW1zLCBldGMuIC0tPg==", "branch": "master"}' https://api.github.com/repos/$LINE/contents/issue_template.md

echo "issue_template added"

#adding issue template. please change the branch if "master" branch is not the default
curl -i -X PUT -H 'Authorization: token <ACCESS_TOKEN>' -d '{"path": "pull_request_template.md", "message": "Add Pull Request Template", "committer": {"name": "maheshika", "email": "maheshika@wso2.com"}, "content": "IyMgUHVycG9zZQ0KPiBEZXNjcmliZSB0aGUgcHJvYmxlbXMsIGlzc3Vlcywgb3IgbmVlZHMgZHJpdmluZyB0aGlzIGZlYXR1cmUvZml4IGFuZCBpbmNsdWRlIGxpbmtzIHRvIHJlbGF0ZWQgaXNzdWVzIGluIHRoZSBmb2xsb3dpbmcgZm9ybWF0OiBSZXNvbHZlcyBpc3N1ZTEsIGlzc3VlMiwgZXRjLg0KDQojIyBHb2Fscw0KPiBEZXNjcmliZSB0aGUgc29sdXRpb25zIHRoYXQgdGhpcyBmZWF0dXJlL2ZpeCB3aWxsIGludHJvZHVjZSB0byByZXNvbHZlIHRoZSBwcm9ibGVtcyBkZXNjcmliZWQgYWJvdmUNCg0KIyMgQXBwcm9hY2gNCj4gRGVzY3JpYmUgaG93IHlvdSBhcmUgaW1wbGVtZW50aW5nIHRoZSBzb2x1dGlvbnMuIEluY2x1ZGUgYW4gYW5pbWF0ZWQgR0lGIG9yIHNjcmVlbnNob3QgaWYgdGhlIGNoYW5nZSBhZmZlY3RzIHRoZSBVSSAoZW1haWwgZG9jdW1lbnRhdGlvbkB3c28yLmNvbSB0byByZXZpZXcgYWxsIFVJIHRleHQpLiBJbmNsdWRlIGEgbGluayB0byBhIE1hcmtkb3duIGZpbGUgb3IgR29vZ2xlIGRvYyBpZiB0aGUgZmVhdHVyZSB3cml0ZS11cCBpcyB0b28gbG9uZyB0byBwYXN0ZSBoZXJlLg0KDQojIyBVc2VyIHN0b3JpZXMNCj4gU3VtbWFyeSBvZiB1c2VyIHN0b3JpZXMgYWRkcmVzc2VkIGJ5IHRoaXMgY2hhbmdlPg0KDQojIyBSZWxlYXNlIG5vdGUNCj4gQnJpZWYgZGVzY3JpcHRpb24gb2YgdGhlIG5ldyBmZWF0dXJlIG9yIGJ1ZyBmaXggYXMgaXQgd2lsbCBhcHBlYXIgaW4gdGhlIHJlbGVhc2Ugbm90ZXMNCg0KIyMgRG9jdW1lbnRhdGlvbg0KPiBMaW5rKHMpIHRvIHByb2R1Y3QgZG9jdW1lbnRhdGlvbiB0aGF0IGFkZHJlc3NlcyB0aGUgY2hhbmdlcyBvZiB0aGlzIFBSLiBJZiBubyBkb2MgaW1wYWN0LCBlbnRlciDigJxOL0HigJ0gcGx1cyBicmllZiBleHBsYW5hdGlvbiBvZiB3aHkgdGhlcmXigJlzIG5vIGRvYyBpbXBhY3QNCg0KIyMgVHJhaW5pbmcNCj4gTGluayB0byB0aGUgUFIgZm9yIGNoYW5nZXMgdG8gdGhlIHRyYWluaW5nIGNvbnRlbnQgaW4gaHR0cHM6Ly9naXRodWIuY29tL3dzbzIvV1NPMi1UcmFpbmluZywgaWYgYXBwbGljYWJsZQ0KDQojIyBDZXJ0aWZpY2F0aW9uDQo+IFR5cGUg4oCcU2VudOKAnSB3aGVuIHlvdSBoYXZlIHByb3ZpZGVkIG5ldy91cGRhdGVkIGNlcnRpZmljYXRpb24gcXVlc3Rpb25zLCBwbHVzIGZvdXIgYW5zd2VycyBmb3IgZWFjaCBxdWVzdGlvbiAoY29ycmVjdCBhbnN3ZXIgaGlnaGxpZ2h0ZWQgaW4gYm9sZCksIGJhc2VkIG9uIHRoaXMgY2hhbmdlLiBDZXJ0aWZpY2F0aW9uIHF1ZXN0aW9ucy9hbnN3ZXJzIHNob3VsZCBiZSBzZW50IHRvIGNlcnRpZmljYXRpb25Ad3NvMi5jb20gYW5kIE5PVCBwYXN0ZWQgaW4gdGhpcyBQUi4gSWYgdGhlcmUgaXMgbm8gaW1wYWN0IG9uIGNlcnRpZmljYXRpb24gZXhhbXMsIHR5cGUg4oCcTi9B4oCdIGFuZCBleHBsYWluIHdoeS4NCg0KIyMgTWFya2V0aW5nDQo+IExpbmsgdG8gZHJhZnRzIG9mIG1hcmtldGluZyBjb250ZW50IHRoYXQgd2lsbCBkZXNjcmliZSBhbmQgcHJvbW90ZSB0aGlzIGZlYXR1cmUsIGluY2x1ZGluZyBwcm9kdWN0IHBhZ2UgY2hhbmdlcywgdGVjaG5pY2FsIGFydGljbGVzLCBibG9nIHBvc3RzLCB2aWRlb3MsIGV0Yy4sIGlmIGFwcGxpY2FibGUNCg0KIyMgQXV0b21hdGlvbiB0ZXN0cw0KIC0gVW5pdCB0ZXN0cyANCiAgID4gQ29kZSBjb3ZlcmFnZSBpbmZvcm1hdGlvbg0KIC0gSW50ZWdyYXRpb24gdGVzdHMNCiAgID4gRGV0YWlscyBhYm91dCB0aGUgdGVzdCBjYXNlcyBhbmQgY292ZXJhZ2UNCg0KIyMgU2VjdXJpdHkgY2hlY2tzDQogLSBGb2xsb3dlZCBzZWN1cmUgY29kaW5nIHN0YW5kYXJkcyBpbiBodHRwOi8vd3NvMi5jb20vdGVjaG5pY2FsLXJlcG9ydHMvd3NvMi1zZWN1cmUtZW5naW5lZXJpbmctZ3VpZGVsaW5lcz8geWVzL25vDQogLSBSYW4gRmluZFNlY3VyaXR5QnVncyBwbHVnaW4gYW5kIHZlcmlmaWVkIHJlcG9ydD8geWVzL25vDQogLSBDb25maXJtZWQgdGhhdCB0aGlzIFBSIGRvZXNuJ3QgY29tbWl0IGFueSBrZXlzLCBwYXNzd29yZHMsIHRva2VucywgdXNlcm5hbWVzLCBvciBvdGhlciBzZWNyZXRzPyB5ZXMvbm8NCg0KIyMgU2FtcGxlcw0KPiBQcm92aWRlIGhpZ2gtbGV2ZWwgZGV0YWlscyBhYm91dCB0aGUgc2FtcGxlcyByZWxhdGVkIHRvIHRoaXMgZmVhdHVyZQ0KDQojIyBSZWxhdGVkIFBScw0KPiBMaXN0IGFueSBvdGhlciByZWxhdGVkIFBScw0KDQojIyBNaWdyYXRpb25zIChpZiBhcHBsaWNhYmxlKQ0KPiBEZXNjcmliZSBtaWdyYXRpb24gc3RlcHMgYW5kIHBsYXRmb3JtcyBvbiB3aGljaCBtaWdyYXRpb24gaGFzIGJlZW4gdGVzdGVkDQoNCiMjIFRlc3QgZW52aXJvbm1lbnQNCj4gTGlzdCBhbGwgSkRLIHZlcnNpb25zLCBvcGVyYXRpbmcgc3lzdGVtcywgZGF0YWJhc2VzLCBhbmQgYnJvd3Nlci92ZXJzaW9ucyBvbiB3aGljaCB0aGlzIGZlYXR1cmUvZml4IHdhcyB0ZXN0ZWQNCiANCiMjIExlYXJuaW5nDQo+IERlc2NyaWJlIHRoZSByZXNlYXJjaCBwaGFzZSBhbmQgYW55IGJsb2cgcG9zdHMsIHBhdHRlcm5zLCBsaWJyYXJpZXMsIG9yIGFkZC1vbnMgeW91IHVzZWQgdG8gc29sdmUgdGhlIHByb2JsZW0u", "branch": "master"}' https://api.github.com/repos/$LINE/contents/pull_request_template.md

echo "pull_request_template added"

#adding protection to branch. please change the branch if "master" branch is not the default
curl -v https://api.github.com/repos/$LINE/branches/master/protection?access_token=<ACCESS_TOKEN> -X PUT -d '{ "required_status_checks": null, "enforce_admins": null, "required_pull_request_reviews": { "include_admins": false }, "restrictions": null }' -H "Accept: application/vnd.github.loki-preview"

echo "master branch protected"

#Adding Infra team Team.
#wso2
curl -i -X PUT -H "" https://api.github.com/teams/<GROUP_ID>/repos/$LINE?access_token=<ACCESS_TOKEN>&permission=push
curl -i -X PUT -H "" https://api.github.com/teams/<GROUP_ID>/repos/$LINE?access_token=<ACCESS_TOKEN>&permission=push

echo "granted infra team access"