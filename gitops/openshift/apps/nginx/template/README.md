### Template for Nginx - To reverse proxy and split traffic between new jag-adobe-common-service api & the other Nginx 2 server container.
* defaultNetworkPolicies.yaml (downloaded QuickStart.yaml from above link)

### Command to execute template
1) Login to OC using login command
2) Run below command in each env. namespace dev/test/prod/tools
   ``oc process -f nginx-jag-adobe-common-service.yaml --param-file=nginx-jag-adobe-common-service.env | oc apply -f -``
