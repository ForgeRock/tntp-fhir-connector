/*
 * Copyright 2014-2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

import static groovyx.net.http.Method.POST

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
    if (uuid != null) {
        // Clean up any cached data for this UID if present
        def special = configuration.getPropertyBag().get(uuid.uidValue)
        if (special != null) {
            configuration.getPropertyBag().remove(uuid.uidValue)
        }

        connection.request(POST, JSON) { req ->
            uri.path = '/entity'
            headers.'Authorization' = "Basic " + bauth
            headers.'Content-Type' = "application/x-www-form-urlencoded"
            body = [ type_name: "user", id: uuid.uidValue ]
            log.error(body)
            log.error("Searching Akamai Identity Cloud for user profile...")
            
            response.success = { resp, json ->
                assert resp.status == 200
                def telephoneNumber = json.result.primaryAddress.phone ?: null
                def email = json.result.email ?: null
                Map<String, Object> map = new LinkedHashMap<>()
                if (json.result.givenName) { map.put("givenName", json.result.givenName) }
                if (json.result.familyName) { map.put("familyName", json.result.familyName) }
                if (json.result.birthday) { map.put("birthday", json.result.birthday) }
                if (json.result.gender) { map.put("gender", json.result.gender) }
                if (telephoneNumber) { map.put("telephoneNumber", telephoneNumber) }
                if (email) { map.put("email", email) }
                if (json.primaryAddress) {
                    map.put("city", json.result.primaryAddress.city)
                    map.put("state", json.result.primaryAddress.stateAbbreviation)
                    map.put("postalCode", json.result.primaryAddress.zip)
                    map.put("country", json.result.primaryAddress.country)
                    map.put("postalAddress", json.result.primaryAddress.address1)
                }

                handler {
                    uid json.result.id
                    id json.result.id
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
// } else {
//     log.error( "Searching all...");
//     next = null;
//
//     connection.request(GET, JSON) { req ->
//         uri.path = "/fhir/Patient"
//         headers.'Authorization' = "Basic " + bauth
//
//         response.success = { resp, json ->
//             assert resp.status == 200
//
//             telephoneNumber = null;
//             email = null;
//
//             if(json.link[1] && json.link[1].relation && json.link[1].relation == "next") {
//                 next = json.link[1].url
//             } else {
//                 next = null;
//             }
//             json.entry.each { item ->
//                 telephoneNumber = null;
//                 email = null;
//
//                 for(def i = 0; item.resource.telecom != null && i < item.resource.telecom.size(); i++) {
//                     if(item.resource.telecom[i].system == "email") {
//                         email = item.resource.telecom[i].value;
//                     }
//                     if(item.resource.telecom[i].system == "phone") {
//                         telephoneNumber = item.resource.telecom[i].value;
//                     }
//                 }
//                 Map<String, Object> map = new LinkedHashMap<>();
//                 if(item.resource.name != null && item.resource.name[0].given[0] != null) {
//                     map.put("givenName", item.resource.name[0].given[0])
//                 }
//                 if(item.resource.name != null && item.resource.name[0].family != null) {
//                     map.put("sn", item.resource.name[0].family);
//                 }
//                 if(item.resource.birthDate != null) {
//                     map.put("dateOfBirth", item.resource.birthDate);
//                 }
//                 if(item.resource.gender != null) {
//                     map.put("gender", item.resource.gender);
//                 }
//                 if(telephoneNumber != null) {
//                     map.put("telephoneNumber", telephoneNumber)
//                 }
//                 if(email != null) {
//                     map.put("email", email)
//                 }
//                 if(item.resource.address != null && item.resource.address[0].city != null) {
//                     map.put("city", item.resource.address[0].city);
//                 }
//                 if(item.resource.address != null && item.resource.address[0].state != null) {
//                     map.put("state", item.resource.address[0].state);
//                 }
//                 if(item.resource.address != null && item.resource.address[0].state != null) {
//                     map.put("stateProvince", item.resource.address[0].state);
//                 }
//                 if(item.resource.address != null && item.resource.address[0].postalCode != null) {
//                     map.put("postalCode", item.resource.address[0].postalCode);
//                 }
//                 if(item.resource.address != null && item.resource.address[0].line[0] != null) {
//                     map.put("postalAddress", item.resource.address[0].line[0]);
//                 }
//                 if(item.resource.address != null && item.resource.address[0].country != null) {
//                     map.put("country", item.resource.address[0].country);
//                 }
//                 handler {
//                     uid item.resource.id
//                     id item.resource.id
//                     attributes ScriptedRESTUtils.MapToAttributes(map, [], false, false)
//                 }
//             }
//         }
//
//         response.failure = { resp, json ->
//             log.error 'request failed'
//             log.error(resp.status)
//             assert resp.status >= 400
//             throw new ConnectorException("List all Failed")
//         }
//     }
// 
//     // Loop through paginated results (up to 10 pages)
//     counter = 0; 
//     while(next != null && counter < 10) {
//         counter = counter+1
//         URI uri1 = new URI(next)
//         String qy = uri1.getQuery();
//         getPage = null
//         getPageOffset = null
//         Map<String, String> params = new HashMap<>();
//         for (String param : qy.split("&")) {
//             String[] parts = param.split("=");
//             if(parts[0] == "_getpages") {
//                 getPage = parts[1]
//             } 
//             else if (parts[0] == "_getpagesoffset") {
//                 getPageOffset = parts[1]
//             }
//         }
//
//         connection.request(GET, JSON) { req ->
//             uri.path = "/fhir"
//             uri.query = [_getpages: getPage, _getpagesoffset: getPageOffset, _count: "50", _bundletype: "searchset",]
//             headers.'Authorization' = "Basic " + bauth
//
//             response.success = { resp, json ->
//                 assert resp.status == 200
//                 telephoneNumber = null;
//                 email = null;
//                 if(json.link[1].relation && json.link[1].relation == "next") {
//                     next = json.link[1].url;
//                 } else {
//                     next = null;
//                 }
//                 json.entry.each { item ->
//                     telephoneNumber = null;
//                     email = null;
//                     for(def i = 0; item.resource.telecom != null && i < item.resource.telecom.size(); i++) {
//                         if(item.resource.telecom[i].system == "email") {
//                             email = item.resource.telecom[i].value;
//                         }
//                         if(item.resource.telecom[i].system == "phone") {
//                             telephoneNumber = item.resource.telecom[i].value;
//                         }
//                     }
//                     Map<String, Object> map = new LinkedHashMap<>();
//                     if(item.resource.name != null && item.resource.name[0].given[0] != null) {
//                         map.put("givenName", item.resource.name[0].given[0])
//                     }
//                     if(item.resource.name != null && item.resource.name[0].family != null) {
//                         map.put("sn", item.resource.name[0].family);
//                     }
//                     if(item.resource.birthDate != null) {
//                         map.put("dateOfBirth", item.resource.birthDate);
//                     }
//                     if(item.resource.gender != null) {
//                         map.put("gender", item.resource.gender);
//                     }
//                     if(telephoneNumber != null) {
//                         map.put("telephoneNumber", telephoneNumber)
//                     }
//                     if(email != null) {
//                         map.put("email", email)
//                     }
//                     if(item.resource.address != null && item.resource.address[0].city != null) {
//                         map.put("city", item.resource.address[0].city);
//                     }
//                     if(item.resource.address != null && item.resource.address[0].state != null) {
//                         map.put("state", item.resource.address[0].state);
//                     }
//                     if(item.resource.address != null && item.resource.address[0].state != null) {
//                         map.put("stateProvince", item.resource.address[0].state);
//                     }
//                     if(item.resource.address != null && item.resource.address[0].postalCode != null) {
//                         map.put("postalCode", item.resource.address[0].postalCode);
//                     }
//                     if(item.resource.address != null && item.resource.address[0].line[0] != null) {
//                         map.put("postalAddress", item.resource.address[0].line[0]);
//                     }
//                     if(item.resource.address != null && item.resource.address[0].country != null) {
//                         map.put("country", item.resource.address[0].country);
//                     }
//                     handler {
//                         uid item.resource.id
//                         id item.resource.id
//                         attributes ScriptedRESTUtils.MapToAttributes(map, [], false, false)
//                     }
//                 }
//             }
//
//             response.failure = { resp, json ->
//                 log.error 'request failed'
//                 log.error(resp.status)
//                 assert resp.status >= 400
//                 throw new ConnectorException("List all Failed")
//             }
//         }
//     }
// }
