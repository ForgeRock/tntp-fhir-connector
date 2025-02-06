/*
 * Copyright 2014-2020 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */


import static groovyx.net.http.Method.DELETE

import org.identityconnectors.framework.common.exceptions.ConnectorException
import org.identityconnectors.framework.common.exceptions.InvalidCredentialException
import org.identityconnectors.framework.common.exceptions.PermissionDeniedException
import org.identityconnectors.framework.common.exceptions.UnknownUidException

import groovyx.net.http.RESTClient
import org.apache.http.client.HttpClient
import org.forgerock.openicf.connectors.groovy.OperationType
import org.forgerock.openicf.connectors.scriptedrest.ScriptedRESTConfiguration
import org.identityconnectors.common.logging.Log
import org.identityconnectors.framework.common.objects.ObjectClass
import org.identityconnectors.framework.common.objects.OperationOptions
import org.identityconnectors.framework.common.objects.Uid
import org.forgerock.openicf.connectors.scriptedrest.SimpleCRESTFilterVisitor
import org.identityconnectors.common.security.SecurityUtil

def operation = operation as OperationType
def configuration = configuration as ScriptedRESTConfiguration
def httpClient = connection as HttpClient
def connection = customizedConnection as RESTClient
def log = log as Log
def objectClass = objectClass as ObjectClass
def options = options as OperationOptions
def uid = uid as Uid

def up = configuration.getUsername() + ":" + SecurityUtil.decrypt(configuration.getPassword())
def bauth = up.getBytes().encodeBase64()
log.error("Entering " + operation + " Delete Script")

log.error("uid:" + uid.uidValue)
switch (objectClass) {
    case ObjectClass.ACCOUNT:
        connection.request(DELETE) { req ->
            uri.path = "/fhir/Patient/" +uid.uidValue
            headers.'Authorization' = "Basic " + bauth
            response.success = { resp, json ->
                assert resp.status == 200
            }
        }
    break
}
