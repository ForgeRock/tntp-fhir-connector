/*
 * Copyright 2014-2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */


import static groovyx.net.http.Method.GET

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

import org.identityconnectors.framework.common.exceptions.ConnectorException
import org.forgerock.openicf.connectors.scriptedrest.ScriptedRESTUtils
import static groovyx.net.http.ContentType.JSON
def operation = operation as OperationType
def configuration = configuration as ScriptedRESTConfiguration
def httpClient = connection as HttpClient
def connection = customizedConnection as RESTClient
def filter = filter as Filter
def log = log as Log
def objectClass = objectClass as ObjectClass
def options = options as OperationOptions
def bearer = ""
        
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
            headers.'Authorization' = "Bearer " + bearer
            

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
    
    def content_location = ""
    connection.request(GET, JSON) { req ->
        uri.path = '/interconnect-fhir-oauth/api/FHIR/R4/Group//$export'
        uri.query = [_type: "Patient"]
        headers.'Authorization' = "Bearer " + bearer
        headers.'Accept' = 'application/fhir+json'
        headers.'Prefer' = 'respond-async'
        response.success = { resp, json ->
            content_location = resp.headers['Content-Location'].toString()
            return content_location
        }

        response.failure = { resp, json ->
            log.error 'first request failed'
            log.error(resp.status)
            assert resp.status >= 400
            throw new ConnectorException("List all Failed")
        }
    }

    
    def file_url = ""
    def status1 = "400"
    content_location = content_location.replaceAll("Content-Location: https://fhir.epic.com", "")
    while(status1.equals("200") == false) {

	    connection.request(GET) { req ->
	        uri.path = content_location
	        headers.'Authorization' = "Bearer " + bearer
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
	
	
	log.error(content_location)
	def next_url = ""
	connection.request(GET, JSON) { req ->
	        uri.path = content_location
	        headers.'Authorization' = "Bearer " + bearer
	       
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

    next_url = next_url.replaceAll("https://fhir.epic.com", "")

    return connection.request(GET, JSON) { req ->
        uri.path = next_url
        headers.'Authorization' = "Bearer " + bearer
        response.success = { resp, json ->

        	def first = json.get(0)
        	log.error(json.id)
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