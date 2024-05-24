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
import org.identityconnectors.framework.common.objects.*

import org.forgerock.openicf.connectors.scriptedrest.ScriptedRESTUtils
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

def logPrefix = "[Epic] [CreateScript]: "
log.error(logPrefix + "Entering " + operation + " Script");
def createAttributes = new AttributesAccessor(attributes as Set<Attribute>)


switch (objectClass) {
    case ObjectClass.ACCOUNT:
        log.error(logPrefix + "Entering ObjectClass.ACCOUNT case in " + operation + " Script");

        HashMap hm = new HashMap();

        for(Iterator i = attributes.iterator();i.hasNext();){
            Attribute thisAt = i.next();
            hm.put(thisAt.getName(), thisAt.getValue());
        }
        def builder = new JsonBuilder()
        def dob = hm.get("dateOfBirth");
        def sn = hm.get("sn");
        def givenName = hm.get("givenName");
        def telephoneNumber = hm.get("telephoneNumber");
        def postalAddress = hm.get("postalAddress");
        def city = hm.get("city");
        def state = hm.get("stateProvince");
        def postalCode = hm.get("postalCode");
        def country = hm.get("country");
        def description = hm.get("description");

        dob = dob.get(0)
        log.error(dob)
        sn = sn.get(0)
        givenName = givenName.get(0)
        telephoneNumber = telephoneNumber.get(0)
        postalAddress = postalAddress.get(0)
        city = city.get(0)
        state = state.get(0)
        postalCode = postalCode.get(0)
        country = country.get(0)

        def jsonString = "{\n" +
                "  \"resourceType\": \"Patient\",\n" +
                "  \"identifier\": [\n" +
                "    {\n" +
                "      \"use\": \"usual\",\n" +
                "      \"system\": \"urn:oid:2.16.840.1.113883.4.1\",\n" +
                "      \"value\": \"${description}\"\n" +
                "    }\n" +
                "  ],\n" +
                "  \"gender\": \"male\",\n" +
                "  \"name\": [\n" +
                "    {\n" +
                "      \"use\": \"usual\",\n" +
                "      \"family\": \"${sn}\",\n" +
                "      \"given\": [\n" +
                "        \"${givenName}\"\n" +
                "      ]\n" +
                "    }\n" +
                "  ],\n" +
                "  \"birthDate\": \"${dob}\"\n" +
                "  \n" +
                "}"

        println jsonString

        // connection.request(POST, JSON) { req ->
        //     uri.path = "/interconnect-fhir-oauth/oauth2/token/"
        //     headers.'Content-Type' = 'application/x-www-form-urlencoded'
        //     body = jsonString

        //     response.success = { resp, json ->
        //         log.error(resp.status.toString())
        //         log.error(resp)
        //         return local
        //     }

        // }

        return connection.request(POST, JSON) { req ->
            uri.path = "/interconnect-fhir-oauth/api/FHIR/R4/Patient/"
            headers.'Authorization' = "Bearer "
            body = jsonString

            response.success = { resp, json ->
                log.error(resp.status.toString())
                location = resp.headers['location'].toString()
                local = location.substring(location.lastIndexOf("/") + 1)
                log.error(local)
                return local
            }

        }



        break
}
return name
