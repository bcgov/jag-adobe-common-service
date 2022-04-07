## Templates to create/update nginx containers for reverse proxy & split traffic between old webmethods and new jag-adobe-common-service api

### Template for Nginx 1 - To reverse proxy and split traffic between new jag-adobe-common-service api & the other Nginx 2 server container.
* defaultNetworkPolicies.yaml (downloaded QuickStart.yaml from above link)


### Command to execute template
1) Login to OC using login command
2) Run below command in each env. namespace dev/test/prod/tools
   ``oc process -f nginx-jag-adobe-common-service.yaml --param-file=nginx-jag-adobe-common-service.env | oc apply -f -``

### Template for Nginx 2 - To reverse proxy the traffic from Nginx 1 to the old webmethods api.


### Command to execute template
1) Login to OC using login command
2) Run below command in each env. namespace dev/test/prod/tools
   ``oc process -f nginx-jag-adobe-common-service-oldwm.yaml --param-file=nginx-jag-adobe-common-service-oldwm.env | oc apply -f -``

