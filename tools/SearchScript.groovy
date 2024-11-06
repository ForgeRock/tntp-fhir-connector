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

import groovy.json.JsonSlurper

def operation = operation as OperationType
def configuration = configuration as ScriptedRESTConfiguration
def httpClient = connection as HttpClient
def connection = customizedConnection as RESTClient
def filter = filter as Filter
def log = log as Log
def objectClass = objectClass as ObjectClass
def options = options as OperationOptions
def bearer = ""

        
def customConfig = configuration.getPropertyBag().get("config") as ConfigObject
def up = customConfig.username + ":" + customConfig.password
def bauth = up.getBytes().encodeBase64()

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
            uri.path = '/fhir/Patient/' + uuid.uidValue
            uri.query = [_format: "json"]
            headers.'Authorization' = "Basic " + bauth
            log.error("Searching....")
            log.error(uuid.uidValue)
            

            response.success = { resp, json ->
                // resp is HttpResponseDecorator
                assert resp.status == 200
                log.error 'request was successful'
                log.error resp.contentType
                log.error json.resourceType
                Map<String, Object> map = new LinkedHashMap<>();
                map.put("givenName", json.name[0].given[0]);
                map.put("sn", json.name[0].family);
                map.put("dateOfBirth", json.birthDate);
                map.put("gender", json.gender);
                map.put("telephoneNumber", json.telecom[0].value);
                map.put("city", json.address[0].city);
                map.put("state", json.address[0].state);
                map.put("stateProvince", json.address[0].state);
                map.put("postalCode", json.address[0].postalCode);
                map.put("postalAddress", json.address[0].line[0]);
                map.put("country", json.address[0].country);

                
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
    
    log.error( "Searching all...");

    return connection.request(GET, JSON) { req ->
        uri.path = "/fhir/Patient/"
        headers.'Authorization' = "Basic " + bauth
        response.success = { resp, json ->
            assert resp.status == 200
            json.entry.each { item ->
                Map<String, Object> map = new LinkedHashMap<>();
                map.put("givenName", item.resource.name[0].given[0]);
                map.put("sn", item.resource.name[0].family);
                map.put("dateOfBirth", item.resource.birthDate);
                map.put("gender", item.resource.gender);
                map.put("telephoneNumber", item.resource.telecom[0].value);
                map.put("city", item.resource.address[0].city);
                map.put("state", item.resource.address[0].state);
                map.put("stateProvince", item.resource.address[0].state);
                map.put("postalCode", item.resource.address[0].postalCode);
                map.put("postalAddress", item.resource.address[0].line[0]);
                map.put("country", item.resource.address[0].country);
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