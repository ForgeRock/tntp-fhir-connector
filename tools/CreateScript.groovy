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

import static groovyx.net.http.Method.POST
import static groovyx.net.http.Method.PUT

def operation = operation as OperationType

def configuration = configuration as ScriptedRESTConfiguration
def httpClient = connection as HttpClient
def connection = customizedConnection as RESTClient
def name = id as String
def log = log as Log
def objectClass = objectClass as ObjectClass
def options = options as OperationOptions

def logPrefix = "[FHIR] [CreateScript]: "
log.error(logPrefix + "Entering " + operation + " Script");
def createAttributes = new AttributesAccessor(attributes as Set<Attribute>)

def customConfig = configuration.getPropertyBag().get("config") as ConfigObject
def up = customConfig.username + ":" + customConfig.password
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
        def builder = new JsonBuilder()
        def dob = hm.get("dateOfBirth");
        dob = dob.get(0)
        log.error("JSON STRING")
        def jsonString = "{ \"resourceType\": \"Patient\", \"birthDate\": \"${dob}\"}"
        println jsonString

        try {
            return connection.request(POST, JSON) { req ->
                uri.path = "/fhir/Patient"
                body = jsonString

                response.success = { resp, json ->
                    return json.id
                }

            }
        } catch (Exception ex) {
            log.error("exception")
            println ex
        }


        
        break
}
return name
