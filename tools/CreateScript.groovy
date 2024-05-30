/*
 * Copyright 2014-2020 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
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


def httpClient = connection as HttpClient
def connection = customizedConnection as RESTClient
def name = id as String
def log = log as Log
def objectClass = objectClass as ObjectClass
def options = options as OperationOptions

def logPrefix = "[Epic] [CreateScript]: "
log.error(logPrefix + "Entering " + operation + " Script");
def createAttributes = new AttributesAccessor(attributes as Set<Attribute>)
def configuration = configuration as ScriptedRESTConfiguration
def customConfig = configuration.getPropertyBag().get("config") as ConfigObject

switch (objectClass) {
    case ObjectClass.ACCOUNT:
        log.error(logPrefix + "Entering ObjectClass.ACCOUNT case in " + operation + " Script");

        HashMap hm = new HashMap();

        for(Iterator i = attributes.iterator();i.hasNext();){
            Attribute thisAt = i.next();
            hm.put(thisAt.getName(), thisAt.getValue());
        }
        def builder = new JsonBuilder()
        def dob = hm.get("dateOfBirth");
        def sn = hm.get("sn");
        def givenName = hm.get("givenName");
        def telephoneNumber = hm.get("telephoneNumber");
        def postalAddress = hm.get("postalAddress");
        def city = hm.get("city");
        def state = hm.get("stateProvince");
        def postalCode = hm.get("postalCode");
        def country = hm.get("country");
        def description = "111-55-3344"; //hm.get("description")

        dob = dob.get(0)
        log.error(dob)
        sn = sn.get(0)
        givenName = givenName.get(0)
        telephoneNumber = telephoneNumber.get(0)
        postalAddress = postalAddress.get(0)
        city = city.get(0)
        state = state.get(0)
        postalCode = postalCode.get(0)
        country = country.get(0)
        //description = description.get(0)

        def jsonString = "{\n" +
                "  \"resourceType\": \"Patient\",\n" +
                "  \"identifier\": [\n" +
                "    {\n" +
                "      \"use\": \"usual\",\n" +
                "      \"system\": \"urn:oid:2.16.840.1.113883.4.1\",\n" +
                "      \"value\": \"${description}\"\n" +
                "    }\n" +
                "  ],\n" +
                "  \"gender\": \"male\",\n" +
                "  \"name\": [\n" +
                "    {\n" +
                "      \"use\": \"usual\",\n" +
                "      \"family\": \"${sn}\",\n" +
                "      \"given\": [\n" +
                "        \"${givenName}\"\n" +
                "      ]\n" +
                "    }\n" +
                "  ],\n" +
                "  \"birthDate\": \"${dob}\"\n" +
                "  \n" +
                "}"

        println jsonString


        //Public and Private Keypair - you need to get one of your own for security purposes.  A free generator can be found here - https://mkjwk.org/
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


        log.error("Here is the signedJWT: " + theSignedJWTString);

        Map<String, String> pairs = new HashMap<String, String>();
        pairs.put("grant_type", "client_credentials");
        pairs.put("client_assertion", theSignedJWTString);
        pairs.put("client_assertion_type", "urn:ietf:params:oauth:client-assertion-type:jwt-bearer");

        def access_token
        def theResponse = connection.request(POST, URLENC) { req ->
            uri.path = "/interconnect-fhir-oauth/oauth2/token/"
            headers.'Content-Type' = 'application/x-www-form-urlencoded'
            headers.'Accept' = 'application/json'
            body = pairs
            log.error("Making access token request")


            


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


        //End JWT generation
        

        return connection.request(POST, JSON) { req ->
            uri.path = "/interconnect-fhir-oauth/api/FHIR/R4/Patient/"
            headers.'Authorization' = "Bearer " + access_token
            body = jsonString

            response.success = { resp, json ->
                log.error(resp.status.toString())
                location = resp.headers['location'].toString()
                local = location.substring(location.lastIndexOf("/") + 1)
                log.error(local)
                return local
            }

        }



        break
}
return name
