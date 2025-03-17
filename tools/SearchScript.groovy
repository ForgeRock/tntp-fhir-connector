/*
 * Copyright 2014-2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

import static groovyx.net.http.Method.POST
import static groovyx.net.http.ContentType.URLENC

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

// Cast input parameters
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
def logPrefix = "[Akamai] [SearchScript]: "
log.error(logPrefix + "Entering " + operation + " Script")

// Build basic auth header
def up = configuration.getUsername() + ":" + SecurityUtil.decrypt(configuration.getPassword())
def bauth = up.getBytes().encodeBase64()

/**
 * ================================
 * VIEW USER PROFILE FROM AKAMAI IDENTITY CLOUD
 * ================================
 *
 * If filter is provided:
 *   - Extract the UID from the filter
 *   - Make a POST request to the /entity endpoint
 *   - Parse and map key attributes from the returned JSON profile
 */
if (filter != null) {
    def uuid = FrameworkUtil.getUidIfGetOperation(filter)
    log.error("UUID from FILTER: {0}", new Object[]{uuid})
    if (uuid != null) {
        // def uuidValue = uuid.getUidValue()
        // Clean up any cached data for this UID if present
        def special = configuration.getPropertyBag().get(uuid.uidValue)
        if (special != null) {
            configuration.getPropertyBag().remove(uuid.uidValue)
        }

        Map<String, String> pairs = new HashMap<String, String>();
        pairs.put("type_name", "user");
        pairs.put("id", Integer.parseInt(uuid.getUidValue()))

        connection.request(POST, URLENC) { req ->
            uri.path = '/entity'
            headers.'Authorization' = "Basic " + bauth
            headers.'Content-Type' = "application/x-www-form-urlencoded"
            body = pairs
            log.error("Searching Akamai Identity Cloud for user profile...")
            
            response.success = { resp, json ->
                assert resp.status == 200
                log.error("Search Success")
                log.error("SEARCH - JSON Response: {0}", new Object[]{json})

                def parsed = new JsonSlurper().parseText(json.keySet().toArray()[0])
                log.error("SEARCH - JSON String: " + parsed)

                Map<String, Object> map = new LinkedHashMap<>()
                if (parsed.result.givenName) { map.put("givenName", parsed.result.givenName) }
                if (parsed.result.familyName) { map.put("familyName", parsed.result.familyName) }
                if (parsed.result.displayName) { map.put("userName", parsed.result.displayName) }
                if (parsed.result.email) { map.put("email", parsed.result.email) }
                // if (parsed.result.birthday) { map.put("birthday", json.result.birthday) }
                // if (parsed.result.gender) { map.put("gender", json.result.gender) }
                // if (telephoneNumber) { map.put("telephoneNumber", telephoneNumber) }
                // if (json.result.primaryAddress) {
                //     map.put("city", json.result.primaryAddress.city)
                //     map.put("state", json.result.primaryAddress.stateAbbreviation)
                //     map.put("postalCode", json.result.primaryAddress.zip)
                //     map.put("country", json.result.primaryAddress.country)
                //     map.put("postalAddress", json.result.primaryAddress.address1)
                // }

                handler {
                    uid parsed.result.id.toString()
                    id parsed.result.id.toString()
                    attributes ScriptedRESTUtils.MapToAttributes(map, [], false, false)
                }
                return new org.identityconnectors.framework.common.objects.SearchResult()
            }

            response.failure = { resp, json ->
                log.error "Akamai API request failed with status: " + resp.status
                if (resp.status > 400 && resp.status != 404) {
                    throw new ConnectorException("View profile request failed")
                }
            }
        }
    }
/**
* ================================
* LIST ALL PATIENTS (No Filter Provided)
* ================================
*
* When no filter is provided,
*   - 1st:  The script performs a list operation by querying the /fhir/Patient endpoint. 
*   - 2nd:  It processes the initial response to capture a pagination URL (if available) 
*   - 3rd:  Iterates through pages (up to 10 pages) to retrieve and process all patient entries, passing each to the handler.
*
*/
} else {
    Map<String, String> pairs = new HashMap<String, String>();
    pairs.put("type_name", "user");
    pairs.put("max_results", 1000)
    log.error("Pairs: {0}", new Object[]{pairs})

    connection.request(POST, URLENC) { req ->
        uri.path = '/entity.find'
        headers.'Authorization' = "Basic " + bauth
        headers.'Content-Type' = "application/x-www-form-urlencoded"
        body = pairs
        log.error("Searching Akamai Identity Cloud for all user profiles...")
            
        response.success = { resp, json ->
            assert resp.status == 200
            log.error("Bulk Search Success")
            log.error("RESPONSE: {0}", resp)
            log.error("BULK SEARCH - RAW JSON Response: {0}", new Object[]{json})

            def parsed = new JsonSlurper().parseText(json.keySet().toArray()[0])
            log.error("BULK SEARCH - JSON String: " + parsed)

            parsed.results.each { item ->
                Map<String, Object> map = new LinkedHashMap<>();
                if(item.displayName != null) {
                    map.put("userName", item.displayName)
                }
                if(item.email != null) {
                    map.put("email", item.email)
                }
                if(item.givenName != null) {
                    map.put("givenName", item.givenName)
                }
                if(item.familyName != null) {
                    map.put("sn", item.familyName);
                }
                if(item.birthday != null) {
                    map.put("dateOfBirth", item.birthday);
                }
                if(item.gender != null) {
                    map.put("gender", item.gender);
                }
                handler {
                    uid item.id.toString()
                    id item.id.toString()
                    attributes ScriptedRESTUtils.MapToAttributes(map, [], false, false)
                }
            }
        }
        
        response.failure = { resp, json ->
            log.error "Akamai API request failed with status: " + resp.status
            if (resp.status > 400 && resp.status != 404) {
                throw new ConnectorException("View profile request failed")
            }
        }
    }
}