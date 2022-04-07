## Templates to create openshift components related to jag-adobe-common-service-tests api deployment

### Command to execute template
1) Login to OC using login command.
2) Run below command in each env. namespace dev/test/prod
   ``oc process -f jag-adobe-common-service-tests.yaml --param-file=jag-adobe-common-service-tests.env | oc apply -f -``


