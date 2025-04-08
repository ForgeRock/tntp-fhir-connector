/*
 * Copyright 2014-2020 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

import static groovyx.net.http.ContentType.JSON

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
import java.util.Iterator
import java.util.HashMap
import org.identityconnectors.common.security.SecurityUtil
import static groovyx.net.http.Method.POST
import static groovyx.net.http.Method.PUT
import org.forgerock.openicf.connectors.scriptedrest.ScriptedRESTConnector
def log = Log.getLog(ScriptedRESTConnector.class) 

def operation = operation as OperationType

def configuration = configuration as ScriptedRESTConfiguration
def httpClient = connection as HttpClient
def connection = customizedConnection as RESTClient
def name = id as String

def objectClass = objectClass as ObjectClass
def options = options as OperationOptions

def logPrefix = "[FHIR] [CreateScripts]: "
log.error(logPrefix + "Entering " + operation + " Script");
def createAttributes = new AttributesAccessor(attributes as Set<Attribute>)

def customConfig = configuration.getPropertyBag().get("config") as ConfigObject
def up = configuration.getUsername() + ":" + SecurityUtil.decrypt(configuration.getPassword())
def bauth = up.getBytes().encodeBase64()

switch (objectClass) {
    case ObjectClass.ACCOUNT:
        log.error(logPrefix + "Entering ObjectClass.ACCOUNT case in " + operation + " Script");

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
        def userId = null
        try {
            connection.request(POST, JSON) { req ->
                uri.path = "/fhir/Patient"
                headers.'Authorization' = "Basic " + bauth
                body = jsonString

                response.success = { resp, json ->
                    userId = json.id
                }

            }
            return new Uid(userId)
        } catch (Exception ex) {
            log.error("exception")
            println ex
        }


        
        break
}
return name