/*
 * This code is to be used exclusively in connection with Ping Identity Corporation software or services. 
 * Ping Identity Corporation only offers such software or services to legal entities who have entered into 
 * a binding license agreement with Ping Identity Corporation.
 *
 * Copyright 2024 Ping Identity Corporation. All Rights Reserved
 */


import static groovyx.net.http.Method.DELETE

import org.identityconnectors.framework.common.exceptions.ConnectorException
import org.identityconnectors.framework.common.exceptions.InvalidCredentialException
import org.identityconnectors.framework.common.exceptions.PermissionDeniedException
import org.identityconnectors.framework.common.exceptions.UnknownUidException

import groovyx.net.http.RESTClient
import org.apache.http.client.HttpClient
import org.forgerock.openicf.connectors.groovy.OperationType
import org.forgerock.openicf.connectors.scriptedrest.ScriptedRESTConfiguration
import org.identityconnectors.common.logging.Log
import org.identityconnectors.framework.common.objects.ObjectClass
import org.identityconnectors.framework.common.objects.OperationOptions
import org.identityconnectors.framework.common.objects.Uid

def operation = operation as OperationType
def configuration = configuration as ScriptedRESTConfiguration
def httpClient = connection as HttpClient
def connection = customizedConnection as RESTClient
def log = log as Log
def objectClass = objectClass as ObjectClass
def options = options as OperationOptions
def uid = uid as Uid

log.info("Entering " + operation + " Script")

def configuration = configuration as ScriptedRESTConfiguration
def customConfig = configuration.getPropertyBag().get("config") as ConfigObject

switch (objectClass) {
    case ObjectClass.ACCOUNT:
        log.info(loggerPrefix + "Entering ObjectClass.ACCOUNT case in " + operation + " Script");

        


    

        JWK challengeSignatureKey = JWK.parse(customConfig.key);

        def myAppClientID = customConfig.clientId;

        JwtClaimsSet jwtClaims = new JwtClaimsSet();
        
        
        jwtClaims.setIssuer(customConfig.iss);
        jwtClaims.setSubject(customConfig.sub);
        jwtClaims.addAudience(customConfig.aud);
        jwtClaims.setJwtId(UUID.randomUUID().toString());
        Calendar c = Calendar.getInstance();
        Date now = c.getTime();
        c.add(Calendar.SECOND, 10);
        Date future = c.getTime();
        jwtClaims.setExpirationTime(future);
        jwtClaims.setIssuedAtTime(now);
        
        SigningManager SIGNING_MANAGER = new SigningManager();
        SigningHandler signingHandler = SIGNING_MANAGER.newSigningHandler(challengeSignatureKey);

        JwtBuilderFactory JWT_BUILDER_FACTORY = new JwtBuilderFactory();

        SignedJwt thisSignedJwt = JWT_BUILDER_FACTORY.jws(signingHandler)
                .headers()
                .alg(JwsAlgorithm.parseAlgorithm(challengeSignatureKey.getAlgorithm()))
                .headerIfNotNull("kid", challengeSignatureKey.getKeyId())
                .done()
                .claims(jwtClaims)
                .asJwt();

        def theSignedJWTString = thisSignedJwt.build();



        Map<String, String> pairs = new HashMap<String, String>();
        pairs.put("grant_type", "client_credentials");
        pairs.put("client_assertion", theSignedJWTString);
        pairs.put("client_assertion_type", "urn:ietf:params:oauth:client-assertion-type:jwt-bearer");

        def access_token
        def theResponse = connection.request(POST, URLENC) { req ->
            uri.path = "/FHIR/oauth2/token/"
            headers.'Content-Type' = 'application/x-www-form-urlencoded'
            headers.'Accept' = 'application/json'
            body = pairs

            response.success = { resp, val1 ->
                def access_token1 = val1
                def accessArray =  access_token1.keySet().toArray()[0]
                def json1 = new JsonSlurper()
                def returnedJson = json1.parseText(accessArray)

                access_token = returnedJson.access_token
                //access_token = val1[0].access_token
                return 
            }

        }
        

        return connection.request(DELETE, JSON) { req ->
            uri.path = "/FHIR/api/FHIR/R4/Patient/" +uid.uidValue
            headers.'Authorization' = "Bearer " + access_token
            response.success = { resp, json ->
                assert resp.status == 200
            }

        }

        break
}
return name

