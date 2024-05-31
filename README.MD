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

`None`
## Configurations

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
<tr>
    <td>customConfiguration</td>
    <td>config { groupId = ''; iss = ''; sub=''; aud = ''; clientId=''; key='{jwk}'}</td>
</tr>

</table>

