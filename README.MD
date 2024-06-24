<!--
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2022 ForgeRock AS.
-->
# FHIR connector

FHIR connector. 


## Setup

Install and setup the Java RCS: https://backstage.forgerock.com/docs/openicf/latest/connector-reference/java-server.html

Once Java RCS openicf is downloaded, create a tools directory in the openicf directory. Place these groovy scripts in this tools directory.

There are also 3 libraries that need to be installed. These are called secrets-api, chf-http-core, and json-web-token. These can be installed in the /lib/framework directory of openicf.

## Epic Application Setup

Generate a JWK
You will need a JWKS.  For example purposes, I used https://mkjwk.org/ to generate a JWK.
Navigate to https://mkjwk.org/
Use the RSA tab
Set Key Size to 2048
Set Key Use to Signature
Set Algorithm to RS256: RSASSA-PKCS1-v1_5 using SHA-256
Set Key ID to Timestamp
Click Generate button
Save the 
Public and Private Keypair
Public Key
Modify the Public Key to make it a set.  Similar to https://raw.githubusercontent.com/justinchinFR/epic/main/pub.json 
Save the Public Key set to a public URL, that can be accessed by Epic.

Setup Epic FHIR App
Login into your Epic FHIR environment.  In this example, that is:
https://fhir.epic.com/
Navigate to the Build Apps tab
Click the + Create button
Enter an Application Name
Select Backend Systems for Application Audience
In the Available column add the following APIs:
Bulk Data Delete Request
Bulk Data File Request
Bulk Data Kick-off
Bulk Data Status Request
Patient.$match (R4)
Patient.Create (R4)
Patient.Read (R4)
Patient.Search (R4)
Add the URL of the Public Key Set created in step 10 above, to the Non-Production JWK Set URL

Click the Save button
Select SMART on FHIR Version R4
Add a Summary
Review and check off I accept the terms of use of open.epic
Click the Save & Ready for Sandbox button


## Configurations

Using the Platform UI, go to applications and browse app catalog. Select the Scripted Rest Connector.  

After creating the connector, set these configurations:

<table>
<thead>
<th>Property</th>
<th>Usage</th>
</thead>
<tr>
    <td>Service Address</td>
    <td>https://fhir.epic.com</td>
</tr>
<tr>
    <td>Proxy Address</td>
    <td></td>
</tr>
<tr>
    <td>Username</td>
    <td>Any string</td>
</tr>
<tr>
    <td>Password</td>
    <td>Any string</td>
</tr>
<tr>
    <td>Default Content Type</td>
    <td></td>
</tr>
<tr>
    <td>Default Request Headers</td>
    <td></td>
</tr>
<tr>
    <td>Default Authentication Method</td>
    <td>BASIC</td>
</tr>
<tr>
    <td>Custom Sensitive Configuration</td>
    <td></td>
</tr>
<tr>
    <td>customConfiguration</td>
    <td>config { groupId = ''; iss = ''; sub=''; aud = ''; clientId=''; key='{jwk}'}</td>
</tr>
<tr>
    <td>Script Roots</td>
    <td>tools/</td>
</tr>
<tr>
    <td>Authenticate Script</td>
    <td>AuthenticateScript.groovy</td>
</tr>
<tr>
    <td>Create Script</td>
    <td>CreateScript.groovy</td>
</tr>
<tr>
    <td>Update Script</td>
    <td>UpdateScript.groovy</td>
</tr>
<tr>
    <td>Delete Script</td>
    <td>DeleteScript.groovy</td>
</tr>
<tr>
    <td>Search Script</td>
    <td>SearchScript.groovy</td>
</tr>
<tr>
    <td>Test Script</td>
    <td>TestScript.groovy</td>
</tr>
<tr>
    <td>Sync Script</td>
    <td>SyncScript.groovy</td>
</tr>
<tr>
    <td>Schema Script</td>
    <td>SchemaScript.groovy</td>
</tr>
<tr>
    <td>Resolve Username Script Script</td>
    <td>ResolveUsernameScript.groovy</td>
</tr>
<tr>
    <td>Script On Resource Script</td>
    <td>ScriptOnResourceScript.groovy</td>
</tr>
<tr>
    <td>Customizer Script</td>
    <td>CustomizerScript.groovy</td>
</tr>


</table>

