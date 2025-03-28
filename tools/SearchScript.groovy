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
                telephoneNumber = null;
                email = null;
                for(def i = 0; json.telecom != null && i < json.telecom.size(); i++) {
                    if(json.telecom[i].system == "email") {
                        email = json.telecom[i].value;
                    }
                    if(json.telecom[i].system == "phone") {
                        telephoneNumber = json.telecom[i].value;
                    }
                }
                Map<String, Object> map = new LinkedHashMap<>();
                if(json.name != null && json.name[0].given[0] != null) {
                    map.put("givenName", json.name[0].given[0])
                }
                if(json.name != null && json.name[0].family != null) {
                    map.put("sn", json.name[0].family);
                }
                if(json.birthDate != null) {
                    map.put("dateOfBirth", json.birthDate);
                }
                if(json.gender != null) {
                    map.put("gender", json.gender);
                }
                if(telephoneNumber != null) {
                    map.put("telephoneNumber", telephoneNumber)
                }
                if(email != null) {
                    map.put("email", email)
                }
                if(json.address != null && json.address[0].city != null) {
                    map.put("city", json.address[0].city);
                }
                if(json.address != null && json.address[0].state != null) {
                    map.put("state", json.address[0].state);
                }
                if(json.address != null && json.address[0].state != null) {
                    map.put("stateProvince", json.address[0].state);
                }
                if(json.address != null && json.address[0].postalCode != null) {
                    map.put("postalCode", json.address[0].postalCode);
                }
                if(json.address != null && json.address[0].line[0] != null) {
                    map.put("postalAddress", json.address[0].line[0]);
                }
                if(json.address != null && json.address[0].country != null) {
                    map.put("country", json.address[0].country);
                }

                
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
                telephoneNumber = null;
                email = null;
                for(def i = 0; item.resource.telecom != null && i < item.resource.telecom.size(); i++) {
                    if(item.resource.telecom[i].system == "email") {
                        email = item.resource.telecom[i].value;
                    }
                    if(item.resource.telecom[i].system == "phone") {
                        telephoneNumber = item.resource.telecom[i].value;
                    }
                }
                Map<String, Object> map = new LinkedHashMap<>();
                if(item.resource.name != null && item.resource.name[0].given[0] != null) {
                    map.put("givenName", item.resource.name[0].given[0])
                }
                if(item.resource.name != null && item.resource.name[0].family != null) {
                    map.put("sn", item.resource.name[0].family);
                }
                if(item.resource.birthDate != null) {
                    map.put("dateOfBirth", item.resource.birthDate);
                }
                if(item.resource.gender != null) {
                    map.put("gender", item.resource.gender);
                }
                if(telephoneNumber != null) {
                    map.put("telephoneNumber", telephoneNumber)
                }
                if(email != null) {
                    map.put("email", email)
                }
                if(item.resource.address != null && item.resource.address[0].city != null) {
                    map.put("city", item.resource.address[0].city);
                }
                if(item.resource.address != null && item.resource.address[0].state != null) {
                    map.put("state", item.resource.address[0].state);
                }
                if(item.resource.address != null && item.resource.address[0].state != null) {
                    map.put("stateProvince", item.resource.address[0].state);
                }
                if(item.resource.address != null && item.resource.address[0].postalCode != null) {
                    map.put("postalCode", item.resource.address[0].postalCode);
                }
                if(item.resource.address != null && item.resource.address[0].line[0] != null) {
                    map.put("postalAddress", item.resource.address[0].line[0]);
                }
                if(item.resource.address != null && item.resource.address[0].country != null) {
                    map.put("country", item.resource.address[0].country);
                }

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
                    telephoneNumber = null;
                    email = null;
                    for(def i = 0; item.resource.telecom != null && i < item.resource.telecom.size(); i++) {
                        if(item.resource.telecom[i].system == "email") {
                            email = item.resource.telecom[i].value;
                        }
                        if(item.resource.telecom[i].system == "phone") {
                            telephoneNumber = item.resource.telecom[i].value;
                        }
                    }
                    Map<String, Object> map = new LinkedHashMap<>();
                    if(item.resource.name != null && item.resource.name[0].given[0] != null) {
                        map.put("givenName", item.resource.name[0].given[0])
                    }
                    if(item.resource.name != null && item.resource.name[0].family != null) {
                        map.put("sn", item.resource.name[0].family);
                    }
                    if(item.resource.birthDate != null) {
                        map.put("dateOfBirth", item.resource.birthDate);
                    }
                    if(item.resource.gender != null) {
                        map.put("gender", item.resource.gender);
                    }
                    if(telephoneNumber != null) {
                        map.put("telephoneNumber", telephoneNumber)
                    }
                    if(email != null) {
                        map.put("email", email)
                    }
                    if(item.resource.address != null && item.resource.address[0].city != null) {
                        map.put("city", item.resource.address[0].city);
                    }
                    if(item.resource.address != null && item.resource.address[0].state != null) {
                        map.put("state", item.resource.address[0].state);
                    }
                    if(item.resource.address != null && item.resource.address[0].state != null) {
                        map.put("stateProvince", item.resource.address[0].state);
                    }
                    if(item.resource.address != null && item.resource.address[0].postalCode != null) {
                        map.put("postalCode", item.resource.address[0].postalCode);
                    }
                    if(item.resource.address != null && item.resource.address[0].line[0] != null) {
                        map.put("postalAddress", item.resource.address[0].line[0]);
                    }
                    if(item.resource.address != null && item.resource.address[0].country != null) {
                        map.put("country", item.resource.address[0].country);
                    }
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
