/*
 * Copyright 2014-2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */


import static groovyx.net.http.Method.GET
import groovy.json.JsonBuilder
import groovyx.net.http.RESTClient
import org.apache.http.client.HttpClient
import org.forgerock.openicf.connectors.groovy.OperationType
import org.forgerock.openicf.connectors.scriptedrest.ScriptedRESTConfiguration
import org.forgerock.openicf.connectors.scriptedrest.SimpleCRESTFilterVisitor
import org.forgerock.openicf.connectors.scriptedrest.VisitorParameter
import org.identityconnectors.common.logging.Log
import org.identityconnectors.framework.common.objects.Attribute
import org.identityconnectors.framework.common.objects.AttributeUtil
import org.identityconnectors.framework.common.objects.Name
import org.identityconnectors.framework.common.objects.ObjectClass
import org.identityconnectors.framework.common.objects.OperationOptions
import org.identityconnectors.framework.common.objects.SearchResult
import org.identityconnectors.framework.common.objects.Uid
import org.identityconnectors.framework.common.objects.filter.Filter
import org.identityconnectors.framework.common.FrameworkUtil
import java.util.Iterator
import java.util.HashMap
import static groovyx.net.http.Method.POST
import static groovyx.net.http.Method.PUT
import org.identityconnectors.framework.common.exceptions.ConnectorException
import org.forgerock.openicf.connectors.scriptedrest.ScriptedRESTUtils
import static groovyx.net.http.ContentType.JSON
import static groovyx.net.http.ContentType.URLENC
import org.apache.http.client.entity.UrlEncodedFormEntity

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

def operation = operation as OperationType
def configuration = configuration as ScriptedRESTConfiguration
def httpClient = connection as HttpClient
def connection = customizedConnection as RESTClient
def filter = filter as Filter
def log = log as Log
def objectClass = objectClass as ObjectClass
def options = options as OperationOptions

def customConfig = configuration.getPropertyBag().get("config") as ConfigObject
// Start JWT generation

//Public and Private Keypair - you need to get one of your own for security purposes.  A free generator can be found here - https://mkjwk.org/
JWK challengeSignatureKey = JWK.parse(customConfig.key);

def myAppClientID = customConfig.clientId;

JwtClaimsSet jwtClaims = new JwtClaimsSet();

log.error("ISS" + customConfig)
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

if (filter != null) {
    def uuid = FrameworkUtil.getUidIfGetOperation(filter)
    log.error(uuid.uidValue)
    if (uuid != null) {
        // Get user
        def special = configuration.getPropertyBag().get(uuid.uidValue)
        if (special != null ) {
            configuration.getPropertyBag().remove(uuid.uidValue)
        }

        

        connection.request(GET, JSON) { req ->
            uri.path = '/interconnect-fhir-oauth/api/FHIR/R4/Patient/' + uuid.uidValue
            uri.query = [_format: "json"]
            headers.'Authorization' = "Bearer " + access_token
            

            response.success = { resp, json ->
                // resp is HttpResponseDecorator
                assert resp.status == 200
                log.error 'request was successful'
                log.error resp.contentType
                log.error json.resourceType
                Map<String, Object> map = new LinkedHashMap<>();
        		map.put("givenName", json.name[0].given[0]);
        		map.put("sn", json.name[0].family);
        		map.put("gender", json.gender);
        		map.put("description", json.birthDate);
        		
                handler {
                    uid json.id
                    id json.id
                    attributes ScriptedRESTUtils.MapToAttributes(map, [], false, false)
                    
                }
            }

            response.failure = { resp, json ->
                log.error 'request failed'
                if (resp.status > 400 && resp.status != 404) {
                    throw new ConnectorException("Get Failed")
                }
            }
        }

    }
} else {
    // List All
    
    log.error("Making request")
    def content_location = ""
    connection.request(GET, JSON) { req ->
        uri.path = '/interconnect-fhir-oauth/api/FHIR/R4/Group/'+customConfig.groupId+'/$export'
        uri.query = [_type: "Patient"]
        headers.'Authorization' = "Bearer " + access_token
        headers.'Accept' = 'application/fhir+json'
        headers.'Prefer' = 'respond-async'
        response.success = { resp, json ->
            log.error(resp.status.toString())
            content_location = resp.headers['Content-Location'].toString()
            log.error(content_location)
            return content_location
        }

        response.failure = { resp, json ->
            log.error 'first request failed'
            log.error(resp.status)
            assert resp.status >= 400
            throw new ConnectorException("List all Failed")
        }
    }

    log.error("Making next request")
    
    def file_url = ""
    def status1 = "400"
    content_location = content_location.replaceAll("Content-Location: https://fhir.epic.com", "")
    while(status1.equals("200") == false) {

	    connection.request(GET) { req ->
	        uri.path = content_location
	        headers.'Authorization' = "Bearer " + access_token
	        response.success = { resp, val  ->
	            log.error(resp.status.toString())
	            status1 = resp.status.toString()
	            //file_url = resp.body['output']['url'].toString()
	            return
	        }

	        response.failure = { resp  ->
	            log.error 'request failed'
	            log.error(resp.status)
	            assert resp.status >= 400
	            throw new ConnectorException("List all Failed")
	        }
	    }
	}
	log.error("Making request 3")
	
	
	log.error(content_location)
	def next_url = ""
	connection.request(GET, JSON) { req ->
	        uri.path = content_location
	        headers.'Authorization' = "Bearer " + access_token
	       
	        response.success = { resp, json  ->
	        	log.error("Here")
	            log.error(resp.status.toString())
	            body1 = resp.data.toString()
	            next_url = json.output[0].url
	            //file_url = resp.body['output']['url'].toString()
	            return
	        }

	        response.failure = { resp  ->
	            log.error 'request failed'
	            log.error(resp.status)
	            assert resp.status >= 400
	            throw new ConnectorException("List all Failed")
	        }
	    }

    log.error("Making request 4")
    next_url = next_url.replaceAll("https://fhir.epic.com", "")

    return connection.request(GET, JSON) { req ->
        uri.path = next_url
        headers.'Authorization' = "Bearer " + access_token
        response.success = { resp, json ->
        	log.error(json.getClass().getName())
        	log.error(json.size().toString())
        	def first = json.get(0)
        	log.error(json.id)
        	log.error("Next")
            // resp is HttpResponseDecorator
            assert resp.status == 200
            handler{
                uid json.id
                id json.id
                attribute 'description', json.birthDate
                attribute 'sn', json.name[0].family

            }
                
            
        }

        response.failure = { resp, json ->
            log.error 'request failed'
            log.error(resp.status)
            assert resp.status >= 400
            throw new ConnectorException("List all Failed")
        }
    }
}