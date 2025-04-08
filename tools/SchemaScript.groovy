/*
 * Copyright 2014-2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */


import static org.identityconnectors.framework.common.objects.AttributeInfo.Flags.NOT_READABLE
import static org.identityconnectors.framework.common.objects.AttributeInfo.Flags.NOT_RETURNED_BY_DEFAULT
import static org.identityconnectors.framework.common.objects.AttributeInfo.Flags.NOT_UPDATEABLE

import groovyx.net.http.RESTClient
import org.apache.http.client.HttpClient
import org.forgerock.openicf.connectors.groovy.OperationType
import org.forgerock.openicf.connectors.scriptedrest.ScriptedRESTConfiguration
import org.identityconnectors.common.logging.Log
import org.identityconnectors.framework.common.objects.AttributeInfoBuilder
import org.identityconnectors.framework.common.objects.ObjectClass

def operation = operation as OperationType
def configuration = configuration as ScriptedRESTConfiguration
def httpClient = connection as HttpClient
def connection = customizedConnection as RESTClient
def log = log as Log
def logPrefix = "[Epic] [SchemaScript]: "

log.info(logPrefix + "Entering " + operation + " Script!");

// Declare the __ACCOUNT__ attributes
// _id
def idAIB = new AttributeInfoBuilder("__NAME__", String.class);
idAIB.setCreateable(true);
idAIB.setMultiValued(false);
idAIB.setUpdateable(false);

def email = new AttributeInfoBuilder("email", String.class);
email.setMultiValued(false);

def birthDate = new AttributeInfoBuilder("birthDate", String.class);
birthDate.setMultiValued(false);

// Additional Patient attributes
def resourceType = new AttributeInfoBuilder("resourceType", String.class);
resourceType.setMultiValued(false);

def idAttr = new AttributeInfoBuilder("id", String.class);
idAttr.setMultiValued(false);

def gender = new AttributeInfoBuilder("gender", String.class);
gender.setMultiValued(false);

def multipleBirthBoolean = new AttributeInfoBuilder("multipleBirthBoolean", Boolean.class);
multipleBirthBoolean.setMultiValued(false);

// Complex or multi-entry fields as Map or Object
def meta = new AttributeInfoBuilder("meta", Map.class);
meta.setMultiValued(false);

def text = new AttributeInfoBuilder("text", Map.class);
text.setMultiValued(false);

def extension = new AttributeInfoBuilder("extension", Map.class);
extension.setMultiValued(true);

def identifier = new AttributeInfoBuilder("identifier", Map.class);
identifier.setMultiValued(true);

def nameAttr = new AttributeInfoBuilder("name", Map.class);
nameAttr.setMultiValued(true);

def telecom = new AttributeInfoBuilder("telecom", Map.class);
telecom.setMultiValued(true);

def address = new AttributeInfoBuilder("address", Map.class);
address.setMultiValued(true);

def maritalStatus = new AttributeInfoBuilder("maritalStatus", Map.class);
maritalStatus.setMultiValued(false);

def communication = new AttributeInfoBuilder("communication", Map.class);
communication.setMultiValued(true);

// Build schema
return builder.schema({
    objectClass {
        type ObjectClass.ACCOUNT_NAME
        attribute idAIB.build()
        attribute email.build()
        attribute birthDate.build()
        attribute resourceType.build()
        attribute idAttr.build()
        attribute gender.build()
        attribute multipleBirthBoolean.build()
        attribute meta.build()
        attribute text.build()
        attribute extension.build()
        attribute identifier.build()
        attribute nameAttr.build()
        attribute telecom.build()
        attribute address.build()
        attribute maritalStatus.build()
        attribute communication.build()
    }
})

log.error(logPrefix + "Schema script dones");
