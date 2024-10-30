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
import org.forgerock.openicf.connectors.scriptedrest.ScriptedRESTUtils
import org.forgerock.openicf.connectors.scriptedrest.ScriptedRESTConfiguration
import org.identityconnectors.common.logging.Log
import org.identityconnectors.framework.common.objects.ObjectClass
import org.identityconnectors.framework.common.objects.SyncToken
import org.identityconnectors.framework.common.exceptions.ConnectorException
import static groovyx.net.http.ContentType.JSON
import static groovyx.net.http.Method.GET
def operation = operation as OperationType
def configuration = configuration as ScriptedRESTConfiguration
def httpClient = connection as HttpClient
def connection = customizedConnection as RESTClient
def log = log as Log
def logPrefix = "[FHIR] [SyncScript]: "
def objectClass = objectClass as ObjectClass
def customConfig = configuration.getPropertyBag().get("config") as ConfigObject
def up = customConfig.username + ":" + customConfig.password
def bauth = up.getBytes().encodeBase64()
log.error("syncing")
log.error("Entering " + operation + " Script");

if (OperationType.GET_LATEST_SYNC_TOKEN.equals(operation)) {
    log.error "LAST TOKEN: {0}", "20220222080554Z"
    def lastToken = 0
    return new SyncToken(lastToken)

} else if (OperationType.SYNC.equals(operation)) {
    log.error("TOKEN" +token)

    def token = 0 as Object

    log.error("Making request")
    return connection.request(GET, JSON) { req ->
        uri.path = '/fhir/Patient'
        uri.query = [_lastUpdated: "gt2024-01-01"]
        headers.'Authorization' = "Basic " + bauth
        log.error("Making request2")
        response.success = { resp, json ->
            // resp is HttpResponseDecorator
            assert resp.status == 200
            json.entry.each() { entry ->
                    handler({
                        syncToken 2
                        CREATE_OR_UPDATE()
                        log.error("Executing object create")
                        object {
                            uid entry.resource.id
                            id entry.resource.id
                            attribute 'sn', "description-update"
                        }

                    })
                
            }
            return new SyncToken(2)
        }

        response.failure = { resp, json ->
            log.error 'request failed'
            log.error(resp.status)
            assert resp.status >= 400
            throw new ConnectorException("List all Failed")
        }
    }
}
