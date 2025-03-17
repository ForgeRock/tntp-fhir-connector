/*
 * Copyright 2014-2020 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

import static groovyx.net.http.ContentType.JSON
import static groovyx.net.http.Method.POST
import static groovyx.net.http.ContentType.URLENC

import groovy.json.JsonBuilder
import groovy.json.JsonSlurper
import groovyx.net.http.RESTClient
import org.apache.http.client.HttpClient
import org.forgerock.openicf.connectors.groovy.OperationType
import org.forgerock.openicf.connectors.scriptedrest.ScriptedRESTConfiguration
import org.identityconnectors.common.logging.Log
import org.identityconnectors.framework.common.objects.Attribute
import org.identityconnectors.framework.common.objects.AttributesAccessor
import org.identityconnectors.framework.common.objects.ObjectClass
import org.identityconnectors.framework.common.objects.OperationOptions
import org.identityconnectors.common.security.SecurityUtil
import org.identityconnectors.framework.common.exceptions.ConnectorException

def operation = operation as OperationType
def configuration = configuration as ScriptedRESTConfiguration
def httpClient = connection as HttpClient
def connection = customizedConnection as RESTClient
def name = id as String
def log = log as Log
def objectClass = objectClass as ObjectClass
def options = options as OperationOptions
def logPrefix = "[Akamai] [CreateScript]: "
log.error(logPrefix + "Entering " + operation + " Script")

def createAttributes = new AttributesAccessor(attributes as Set<Attribute>)
def customConfig = configuration.getPropertyBag().get("config") as ConfigObject

// Build Basic Authentication header
def up = configuration.getUsername() + ":" + SecurityUtil.decrypt(configuration.getPassword())
def bauth = up.getBytes().encodeBase64()

switch (objectClass) {
    case ObjectClass.ACCOUNT:
        log.error(logPrefix + "Processing ObjectClass.ACCOUNT for " + operation + " Script")

        // Collect attributes into a map
        HashMap hm = new HashMap()
        for (Attribute attr : attributes) {
            log.error(logPrefix + "Attribute " + attr.getName() + ": " + attr.getValue())
            hm.put(attr.getName(), attr.getValue())
        }

        // Extract necessary fields
        def givenName = hm.get("givenName") ? hm.get("givenName").get(0) : ""
        def familyName = hm.get("sn") ? hm.get("sn").get(0) : ""
        def email = hm.get("mail") ? hm.get("mail").get(0) : ""

        // Build the attributes
        def attributesMap = [
            givenName: givenName,
            familyName: familyName,
            email: email
        ]
        def jsonAttributes = new JsonBuilder(attributesMap).toString()
        log.error("JSON Attributes: {0}", new Object[]{jsonAttributes})

        Map<String, String> pairs = new HashMap<String, String>();
        pairs.put("type_name", "user");
        // pairs.put("attributes", jsonAttributes);
        pairs.put("attributes", '{"givenName":"Matt1", "familyName":"Teets1", "email":"matt1@test.com"}');
        log.error("Pairs: {0}", new Object[]{pairs})

        try {
            return connection.request(POST, URLENC) { req ->
                uri.path = "/entity.create"
                headers.'Authorization' = "Basic " + bauth
                headers.'Content-Type' = "application/x-www-form-urlencoded"
                body = pairs
                
                response.success = { resp, json ->
                    log.error(logPrefix + "User profile created successfully")
                    log.error("CREATE - RAW JSON Response: {0}", new Object[]{json})

                    def parsed = new JsonSlurper().parseText(json.keySet().toArray()[0])
                    log.error("CREATE - JSON String: " + parsed)
                    def json_id = parsed.id

                    return json_id.toString()
                }
            }
        } catch (Exception ex) {
            log.error(logPrefix + "Exception occurred during create: " + ex)
        }
        break
}
return name
