#DGHV Solid App
A Solid App implements binary computation that enables users to
make private request to Solid App to request for file, while no information 
of the request is leaked to the Server, nor to any third party. 
The Solid App does not know which file the user requested for.

Solid is a project, refer to https://solidproject.org/

code reference: https://docs.inrupt.com/developer-tools/java/client-libraries/

The app currently only supports single user scenario since the Solid Java Client Library
is still under development, but surely this could be adjusted to multi-users app in the future.

###How to use?
Change `clientId, clientSecret, podStorageCrypto, storage` to your own version before use.
And simply run the server! 

###API
POST  `http://localhost:8080/secure` To make private request to the Solid App
