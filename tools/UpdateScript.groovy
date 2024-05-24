/*
 * Copyright 2014-2020 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */


import static groovyx.net.http.ContentType.JSON
import static groovyx.net.http.Method.GET
import static groovyx.net.http.Method.PUT

import org.identityconnectors.framework.common.exceptions.InvalidCredentialException
import org.identityconnectors.framework.common.exceptions.PermissionDeniedException
import org.identityconnectors.framework.common.exceptions.UnknownUidException

import groovy.json.JsonBuilder
import groovyx.net.http.RESTClient
import org.apache.http.client.HttpClient
import org.forgerock.openicf.connectors.groovy.OperationType
import org.forgerock.openicf.connectors.scriptedrest.ScriptedRESTConfiguration
import org.identityconnectors.common.logging.Log
import org.identityconnectors.framework.common.exceptions.ConnectorException
import org.identityconnectors.framework.common.objects.Attribute
import org.identityconnectors.framework.common.objects.AttributesAccessor
import org.identityconnectors.framework.common.objects.ObjectClass
import org.identityconnectors.framework.common.objects.OperationOptions
import org.identityconnectors.framework.common.objects.Uid

def operation = operation as OperationType
def updateAttributes = new AttributesAccessor(attributes as Set<Attribute>)
def configuration = configuration as ScriptedRESTConfiguration
def httpClient = connection as HttpClient
def connection = customizedConnection as RESTClient
def name = id as String
def log = log as Log
def objectClass = objectClass as ObjectClass
import static groovyx.net.http.ContentType.JSON
import static groovyx.net.http.Method.PUT
def options = options as OperationOptions
def uid = uid as Uid

// log.error("Entering " + operation + " Script");

// log.error("uid:" + uid)
// switch (operation) {
//     case OperationType.UPDATE:
//         def builder = new JsonBuilder()
//         switch (objectClass) {
//             case ObjectClass.ACCOUNT:
//                 def dob = "1998-08-18"
//                 log.error(dob)
//                 def jsonString = "{ \"resourceType\": \"Patient\", \"birthDate\": \"${dob}\"}"
//                 connection.request(PUT, JSON) { req ->
//                     uri.path = "/fhir/Patient/127759"
//                     body = jsonString

//                     response.success = { resp, json ->
//                         return json.id
//                     }
//                 }
//             default:
//                 throw new ConnectorException("UpdateScript can not handle object type: " + objectClass.objectClassValue)
//         }
//         break
//     default:
//         throw new ConnectorException("UpdateScript can not handle operation:" + operation.name())
// }
// return uid