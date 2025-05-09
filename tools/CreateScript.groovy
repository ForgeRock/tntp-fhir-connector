/*
 * This code is to be used exclusively in connection with Ping Identity Corporation software or services. 
 * Ping Identity Corporation only offers such software or services to legal entities who have entered into 
 * a binding license agreement with Ping Identity Corporation.
 *
 * Copyright 2024 Ping Identity Corporation. All Rights Reserved
 */

import static groovyx.net.http.ContentType.JSON
import static groovyx.net.http.ContentType.URLENC

import groovy.json.JsonBuilder
import groovyx.net.http.RESTClient
import org.apache.http.client.HttpClient
import org.forgerock.openicf.connectors.groovy.OperationType
import org.forgerock.openicf.connectors.scriptedrest.ScriptedRESTConfiguration
import org.identityconnectors.common.logging.Log
import org.identityconnectors.framework.common.objects.Attribute
import org.identityconnectors.framework.common.objects.AttributesAccessor
import org.identityconnectors.framework.common.objects.ObjectClass
import org.identityconnectors.framework.common.objects.OperationOptions
import org.identityconnectors.framework.common.objects.*
import org.apache.http.client.entity.UrlEncodedFormEntity
import org.forgerock.openicf.connectors.scriptedrest.ScriptedRESTUtils
import java.util.Iterator
import java.util.HashMap
import static groovyx.net.http.Method.POST
import static groovyx.net.http.Method.PUT
import org.apache.http.message.BasicHeader
import org.apache.http.message.BasicNameValuePair
import org.apache.http.NameValuePair

import org.forgerock.json.jose.builders.JwtBuilderFactory;
import org.forgerock.json.jose.jwk.JWK;
import org.forgerock.json.jose.jws.JwsAlgorithm;
import org.forgerock.json.jose.jws.SignedJwt;
import org.forgerock.json.jose.jws.SigningManager;
import org.forgerock.json.jose.jws.handlers.SigningHandler;
import org.forgerock.json.jose.jwt.JwtClaimsSet;
import org.forgerock.http.protocol.Response;
import java.util.UUID;
import java.util.Date;
import java.util.Calendar;
import groovy.json.JsonSlurper

import groovyx.net.http.HTTPBuilder.RequestConfigDelegate

def operation = operation as OperationType

def loggerPrefix = "[EPIC FHIR Scripted Rest Connector][Create] "
def httpClient = connection as HttpClient
def connection = customizedConnection as RESTClient
def name = id as String
def log = log as Log
def objectClass = objectClass as ObjectClass
def options = options as OperationOptions

log.info(loggerPrefix + "Entering " + operation + " Script");
def createAttributes = new AttributesAccessor(attributes as Set<Attribute>)
def configuration = configuration as ScriptedRESTConfiguration
def customConfig = configuration.getPropertyBag().get("config") as ConfigObject

switch (objectClass) {
    case ObjectClass.ACCOUNT:
        log.info(loggerPrefix + "Entering ObjectClass.ACCOUNT case in " + operation + " Script");

        HashMap hm = new HashMap();

        for(Iterator i = attributes.iterator();i.hasNext();){
            Attribute thisAt = i.next();
            log.error(logPrefix + "Here is thisAt name: " + thisAt.getName() + " and here is thisAts value: " + thisAt.getValue());
            hm.put(thisAt.getName(), thisAt.getValue());
        }
        hm.put("resourceType", "Patient");
        def builder = new JsonBuilder(hm)
        def jsonString = builder.toString()
        println jsonString


    

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
        

        return connection.request(POST, JSON) { req ->
            uri.path = "/FHIR/api/FHIR/R4/Patient/"
            headers.'Authorization' = "Bearer " + access_token
            body = jsonString

            response.success = { resp, json ->
                location = resp.headers['location'].toString()
                patient_id = location.substring(location.lastIndexOf("/") + 1)
                return patient_id
            }

        }




        break
}
return name
