/*
 * This code is to be used exclusively in connection with Ping Identity Corporation software or services. 
 * Ping Identity Corporation only offers such software or services to legal entities who have entered into 
 * a binding license agreement with Ping Identity Corporation.
 *
 * Copyright 2024 Ping Identity Corporation. All Rights Reserved
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
import static groovyx.net.http.Method.PATCH
def operation = operation as OperationType
def configuration = configuration as ScriptedRESTConfiguration
def httpClient = connection as HttpClient
def connection = customizedConnection as RESTClient
def log = log as Log
def logPrefix = "[FHIR] [SyncScript]: "
def objectClass = objectClass as ObjectClass

// log.error("syncing")
// log.error("Entering " + operation + " Script");

// if (OperationType.GET_LATEST_SYNC_TOKEN.equals(operation)) {
//     log.error "LAST TOKEN: {0}", "20220222080554Z"
//     def lastToken = 0
//     return new SyncToken(lastToken)

// } else if (OperationType.SYNC.equals(operation)) {
//     log.error("TOKEN" +token)

//     def token = 0 as Object

//     log.error("Making request")
//     def content_location = ""
//     connection.request(GET, JSON) { req ->
//         uri.path = '/interconnect-fhir-oauth/api/FHIR/R4/Group/e3iabhmS8rsueyz7vaimuiaSmfGvi.QwjVXJANlPOgR83/$export'
//         uri.query = [_type: "Patient"]
//         headers.'Authorization' = "Bearer"
//         response.success = { resp, json ->
//             log.error(resp.status.toString())
//             content_location = resp.headers['Content-Location'].toString()
//             log.error(content_location)
//             return content_location
//         }

//         response.failure = { resp, json ->
//             log.error 'request failed'
//             log.error(resp.status)
//             assert resp.status >= 400
//             throw new ConnectorException("List all Failed")
//         }
//     }
//     log.error("waiting")
//     wait(100000)
//     log.error("Making next request")
//     def file_url = ""
//     connection.request(GET, JSON) { req ->
//         uri.path = content_location
//         headers.'Authorization' = "Bearer "
//         response.success = { resp, json ->
//             log.error(resp.status.toString())
//             file_url = resp.body['output']['url'].toString()
//             return
//         }

//         response.failure = { resp, json ->
//             log.error 'request failed'
//             log.error(resp.status)
//             assert resp.status >= 400
//             throw new ConnectorException("List all Failed")
//         }
//     }
//     wait(100000)

//     return connection.request(GET, JSON) { req ->
//         uri.path = file_url
//         headers.'Authorization' = "Bearer "
//         log.error("Making request2")
//         response.success = { resp, json ->
//             // resp is HttpResponseDecorator
//             assert resp.status == 200
//             json.entry.each() { entry ->
//                 return handler({
//                     syncToken 2
//                     CREATE_OR_UPDATE()
//                     log.error("Executing object create")
//                     object {
//                         uid entry.resource.id
//                         id entry.resource.id
//                         attribute 'description', "description-update"
//                     }

//                 })
                
//             }
//             return new SyncToken(2)
//         }

//         response.failure = { resp, json ->
//             log.error 'request failed'
//             log.error(resp.status)
//             assert resp.status >= 400
//             throw new ConnectorException("List all Failed")
//         }
//     }
// }

