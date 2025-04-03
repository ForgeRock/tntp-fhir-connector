/*
 * Copyright 2014-2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */


import static groovyx.net.http.Method.GET
import org.forgerock.openicf.connectors.scriptedrest.ScriptedRESTConnector
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
import org.identityconnectors.common.security.SecurityUtil
import groovy.json.JsonSlurper

def operation = operation as OperationType
def configuration = configuration as ScriptedRESTConfiguration
def httpClient = connection as HttpClient
def connection = customizedConnection as RESTClient
def filter = filter as Filter
def log = Log.getLog(ScriptedRESTConnector.class) 
def objectClass = objectClass as ObjectClass
def options = options as OperationOptions
def bearer = ""

        
def customConfig = configuration.getPropertyBag().get("config") as ConfigObject
def up = configuration.getUsername() + ":" + SecurityUtil.decrypt(configuration.getPassword())
def bauth = up.getBytes().encodeBase64()

if (filter != null) {
    def uuid = FrameworkUtil.getUidIfGetOperation(filter)
    if (uuid != null) {
        // Get user
        def special = configuration.getPropertyBag().get(uuid.uidValue)
        if (special != null ) {
            configuration.getPropertyBag().remove(uuid.uidValue)
        }

        

        connection.request(GET, JSON) { req ->
            uri.path = '/fhir/Patient/' + uuid.uidValue
            uri.query = [_format: "json"]
            headers.'Authorization' = "Basic " + bauth
            log.error("Searching....")
            

            
            

            response.success = { resp, json ->
                // resp is HttpResponseDecorator
                assert resp.status == 200
                log.error 'request was successful'
                log.error resp.contentType
                log.error json.resourceType
                def map = json

                
                handler {
                    uid json.id
                    id json.id
                    attributes ScriptedRESTUtils.MapToAttributes(map, [], false, false)
                    
                }
                return new org.identityconnectors.framework.common.objects.SearchResult()
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
    
    log.error( "Searching all...");
    next = null;

    connection.request(GET, JSON) { req ->
        uri.path = "/fhir/Patient"
        headers.'Authorization' = "Basic " + bauth
        response.success = { resp, json ->
            assert resp.status == 200
            telephoneNumber = null;
            email = null;
            if(json.link[1] && json.link[1].relation && json.link[1].relation == "next") {
                next = json.link[1].url

            } else {
                next = null;
            }
            json.entry.each { item ->
                def map = item.resource

                handler {
                    uid item.resource.id
                    id item.resource.id
                    attributes ScriptedRESTUtils.MapToAttributes(map, [], false, false)
                }
            }

            
                         
        }

        response.failure = { resp, json ->
            log.error 'request failed'
            log.error(resp.status)
            assert resp.status >= 400
            throw new ConnectorException("List all Failed")
        }
    }
    counter = 0; 
    while(next != null && counter < 10) {
        counter = counter+1
        URI uri1 = new URI(next)
        String qy = uri1.getQuery();
        getPage = null
        getPageOffset = null
        Map<String, String> params = new HashMap<>();
        for (String param : qy.split("&")) {
            String[] parts = param.split("=");
            if(parts[0] == "_getpages") {
                getPage =  parts[1]

            } 
            else if (parts[0] == "_getpagesoffset") {
                getPageOffset = parts[1]
            }
        }
        connection.request(GET, JSON) { req ->
            uri.path = "/fhir"
            uri.query = [_getpages: getPage, _getpagesoffset: getPageOffset, _count: "50", _bundletype: "searchset",]
            headers.'Authorization' = "Basic " + bauth
            response.success = { resp, json ->
                assert resp.status == 200
                telephoneNumber = null;
                email = null;
                if(json.link[1].relation && json.link[1].relation == "next") {
                    next = json.link[1].url;
                    

                } else {
                    next = null;
                }
                json.entry.each { item ->
                    def map = item.resource
                    handler {
                        uid item.resource.id
                        id item.resource.id
                        attributes ScriptedRESTUtils.MapToAttributes(map, [], false, false)
                    }
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
}
