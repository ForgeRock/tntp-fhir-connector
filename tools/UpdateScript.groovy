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
import org.forgerock.openicf.connectors.scriptedrest.ScriptedRESTConnector
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
import org.identityconnectors.common.security.SecurityUtil

def operation = operation as OperationType
def updateAttributes = new AttributesAccessor(attributes as Set<Attribute>)
def configuration = configuration as ScriptedRESTConfiguration
def httpClient = connection as HttpClient
def connection = customizedConnection as RESTClient
def name = id as String
def log = Log.getLog(ScriptedRESTConnector.class) 
def objectClass = objectClass as ObjectClass
import static groovyx.net.http.ContentType.JSON
import static groovyx.net.http.Method.PUT
def options = options as OperationOptions
def uid = uid as Uid

log.error("Entering " + operation + " Script");

def customConfig = configuration.getPropertyBag().get("config") as ConfigObject
def up = configuration.getUsername() + ":" + SecurityUtil.decrypt(configuration.getPassword())
def bauth = up.getBytes().encodeBase64()

switch (operation) {
    case OperationType.UPDATE:
        def builder = new JsonBuilder()
        switch (objectClass) {
            case ObjectClass.ACCOUNT:
                HashMap hm = new HashMap();

                for(Iterator i = attributes.iterator();i.hasNext();){
                    Attribute thisAt = i.next();
                    log.error(logPrefix + "Here is thisAt name: " + thisAt.getName() + " and here is thisAts value: " + thisAt.getValue());
                    hm.put(thisAt.getName(), thisAt.getValue());
                }
                hm.put("resourceType", "Patient");
                def builder = new JsonBuilder(hm)
                def jsonString = builder.toString()
                return connection.request(PUT, JSON) { req ->
                    uri.path = "/fhir/Patient/" + uid
                    headers.'Authorization' = "Basic " + bauth
                    body = jsonString

                    response.success = { resp, json ->
                        return json.id
                    }
                }
            default:
                throw new ConnectorException("UpdateScript can not handle object type: " + objectClass.objectClassValue)
        }
        break
    default:
        throw new ConnectorException("UpdateScript can not handle operation:" + operation.name())
}
return uid
