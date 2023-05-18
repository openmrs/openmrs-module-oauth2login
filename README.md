# OpenMRS OAuth 2.0 Login Module

This OpenMRS module delegates user authentication to an OAuth 2.0 resource provider. It turns OpenMRS into an OAuth 2.0 _client_ as soon as the module is installed and running.

- [Overview](#overview)
- [Authentication Mechanism](#authentication-mechanism)
    + [Overview](#overview-1)
    + [On-the-fly user creation](#on-the-fly-user-creation)
    + [Keeping identities in sync with OpenMRS](#keeping-identities-in-sync-with-openmrs)
    + [Example](#example)
- [Redirect URL after successful login](#redirect-url-after-successful-login)
- [Two-step Login with OpenMRS 2.x](#two-step-login-with-openmrs-2x)
- [Service Accounts](#service-accounts)
  * [Service Accounts and Microsoft Azure AD](#service-accounts-and-microsoft-azure-ad)
- [IdP Configuration Guides](#idp-configuration-guides)
- [OpenMRS Platform Requirements](#openmrs-platform-requirements)

<small><i><a href='http://ecotrust-canada.github.io/markdown-toc/'>(Table of contents generated with markdown-toc)</a></i></small>

## Overview
It suffices to install the module for OpenMRS' default basic authentication scheme to become inactive and for the module OAuth 2.0-based authentication scheme to take over.

The module consumes a configuration file **oauth2.properties** that must be dropped in the OpenMRS app data directory:

<pre>
.
├── modules/
├── openmrs.war
├── openmrs-runtime.properties
├── ...
└── <b>oauth2.properties</b>
</pre>

The properties configuration contains two separate sets of settings:
1. The usual OAuth 2.0 properties:
    * The [client ID and secret](https://www.oauth.com/oauth2-servers/client-registration/client-id-secret/).
    * A couple of URIs to transact with the OAuth 2.0 provider: user authorization URI, access token URI and user info URI.

2. OpenMRS users properties mappings with the OAuth 2.0 'user info'.
<br/>For new users the master information is first maintained with the OAuth 2.0 provider, starting with their _username_. This information is obtained through a JSON response from the user info URI. A simple one-to-one mapping between what is needed from an OpenMRS user's perspective and what is given by the OAuth 2.0 provider can be provided through the OAuth 2.0 properties file.

The module ships with sample test resources that show how the OAuth 2.0 properties file should look like when using JBoss' Keycloak and Google API as OAuth 2.0 providers, see [here](./omod/src/test/resources/).

## Authentication Mechanism
#### Overview
OpenMRS requires persisted OpenMRS users in its database with roles to perform actions within the application. For the OAuth 2.0 provider to be able to take care of authentication there has to be a duplication of users in both systems: a user will exist both with the OAuth 2.0 provider and the corresponding user will also exist within OpenMRS \*.

The authentication is based on the **username**.

<sub>\* _This duplication of users could be avoided if OpenMRS was fully leveraging Spring Security. This is not yet the case and as of now authorization is made based on users that are persisted and accessed through the DAO layer._</sub>

#### On-the-fly user creation
However at first the user might not exist yet in OpenMRS and as a convenience the module will create a new OpenMRS user on the fly. This is why a mapping mechanism must exist between the OAuth 2.0 provider user infos and the OpenMRS users, at minima to find out what the OpenMRS username will be.

#### Keeping identities in sync with OpenMRS
The main use case is to help support the management of users and roles **outside** of OpenMRS, with the identity provider (IdP). The pieces of information about the OpenMRS' `User` that can be provided by the IdP through its user info JSON are listed here:

* [Username](https://github.com/openmrs/openmrs-core/blob/aeaa7094bbe365dad52e39b2e4935b1a364e6084/api/src/main/java/org/openmrs/User.java#L52) \*
  * A string, eg. `"jdoe"`.
* [System ID](https://github.com/openmrs/openmrs-core/blob/aeaa7094bbe365dad52e39b2e4935b1a364e6084/api/src/main/java/org/openmrs/User.java#L50) \*
  * A string, eg. `"4e3074d6-5e9f-4707-84f1-ccb2aa2ab3bc"`.
* [Email](https://github.com/openmrs/openmrs-core/blob/aeaa7094bbe365dad52e39b2e4935b1a364e6084/api/src/main/java/org/openmrs/User.java#L54)
  * A string, eg. `"jdoe@example.com"`.
* Given, middle and last names (managed through the underlying `Person` [here](https://github.com/openmrs/openmrs-core/blob/aeaa7094bbe365dad52e39b2e4935b1a364e6084/api/src/main/java/org/openmrs/Person.java#L57))
* Gender (same remark, [here](https://github.com/openmrs/openmrs-core/blob/aeaa7094bbe365dad52e39b2e4935b1a364e6084/api/src/main/java/org/openmrs/Person.java#L63))
* [Roles](https://github.com/openmrs/openmrs-core/blob/aeaa7094bbe365dad52e39b2e4935b1a364e6084/api/src/main/java/org/openmrs/User.java#L56)
  * A JSON array of strings, eg. `["Nurse", "Clinical Advisor"]`.
* Provider account activation (managed through the underlying `Person` [here](https://github.com/openmrs/openmrs-core/blob/aeaa7094bbe365dad52e39b2e4935b1a364e6084/api/src/main/java/org/openmrs/Provider.java#L26)).
  * A boolean as string `"true"` or `"false"`.
    * `true` activates the provider account for the authenticated user, this is also the default if nothing is specified.
    * `false` deactivates the provider account for the authenticated user.
    
\* _Username and system ID cannot be updated, they are set once and for all at the first authentication of the user._

##### Externalised role management
A list of OpenMRS roles can be provided through the user info JSON. This can be done through leveraging the `openmrs.mapping.user.roles` mapping property that holds a pointer to the user info JSON key whose value is a comma-separated list of OpenMRS role names.

For instance a basic user info JSON might look like that:

```json
{
  "sub": "4e3074d6-5e9f-4707-84f1-ccb2aa2ab3bc",
  "email": "jdoe@example.com",
  "roles": ["Nurse", "Clinical Advisor"]
}
```
With an OAuth 2.0 properties mapping set as such:
```
openmrs.mapping.user.roles=roles
```
Where "Nurse" and "Clinical Advisor" are expected to be OpenMRS role _names_. Those role names are then used to fetch OpenMRS roles to be assigned to the user. All the role names that do not point to existing OpenMRS roles are skipped.

This requires using an identity provider that allows the configuration of the user info JSON with custom members, such as this `"roles"` member in the above example.

##### Sample mapping
Let us start from a sample JSON to understand how the mappings should be set.

###### 1) Sample  user info  JSON:
```json
{
  "sub": "4e3074d6-5e9f-4707-84f1-ccb2aa2ab3bc",
  "preferred_username": "tatkins",
  "given_name": "Tommy",
  "family_name": "Atkins",
  "email": "tatkins@example.com",
  "roles": [
    "Provider",
    "Nurse"
  ],
  "provider": "true"
}
```

###### 2) Corresponding mappings needed in oauth2.properties:
```java
openmrs.mapping.user.systemId=sub
openmrs.mapping.user.username=preferred_username
openmrs.mapping.person.givenName=given_name
openmrs.mapping.person.familyName=family_name
openmrs.mapping.user.email=email
openmrs.mapping.user.roles=roles
openmrs.mapping.user.provider=provider
```

#### Example
If a user authenticates as 'jdoe' with the OAuth 2.0 provider, OpenMRS will attempt to fetch the user 'jdoe'.
* If a 'jdoe' user can be found in OpenMRS, then it will updated as per the user info JSON and become the authenticated user.
* If a 'jdoe' user cannot be found in OpenMRS, it will be created as per the user info JSON and become the authenticated user.
* 'jdoe' user is also associated to a new provider account if the `provider` is unspecified or set to `true`.
  * A new provider account will be created if it doesn't exist yet.
  * If the provider account already exists and is retired, it will be unretired.

## Redirect URL after successful login
By default the user will be redirected to the root URL `/` after a successul login. The redirect URL can be modified by using the global property (GP) `oauth2login.redirectUriAfterLogin`.
For example when the module is used within the Reference Application with the two-screen login enabled, this GP can be used to enforce a redirect to the login GSP page (hence kicking in its Java controller logic):
```
/referenceapplication/login.page?redirectUrl=/index.html
```

## Two-step Login with OpenMRS 2.x
In OpenMRS 2.x it is necessary to explicitely enable the two-step login for the OAuth 2.0 delegated authentication to work properly. To do so make sure that the following global property exists with a non-blank value: `referenceapplication.locationUserPropertyName`.

## Service Accounts
Service accounts are used to authenticate applications or clients that are not end (human) users. They support authenticated server-to-server interactions with OpenMRS when third party applications or clients need to access OpenMRS resources securely. Service accounts should be able to provide a token obtained from an IdP that, that can be trusted by OpenMRS, in order to authenticate and authorize them to access restricted resources.

### Service Accounts and Microsoft Azure AD
To activate a service account with Azure AD, a resource identifier (application ID URI) should be activated for the application. See
* [Microsoft identity platform and the OAuth 2.0 client credentials flow](https://learn.microsoft.com/en-us/azure/active-directory/develop/v2-oauth2-client-creds-grant-flow#first-case-access-token-request-with-a-shared-secret)

The access token provided by Azure AD won't contain the same claims as the current token (`email`, `first_name`, ...) and this could be an issue if the email is used as the username.
To support this token and be able to use another claim to retrieve the user in OpenMRS, the property `openmrs.mapping.user.username.serviceAccount` can be defined in the file `oauth2.properties`.

See [azure/oauth2.properties](./omod/src/test/resources/azure/oauth2.properties) for a configuration example.

In OpenMRS, a user with a `username` or `system_id` corresponding to this property should be created. `system_id` should be used if the email is used as `username`. 

#### How it Works
The third party system obtains a [JWT](https://jwt.io/) token from an identity provider and then for any subsequent requests, it
sets it as the value of the authorization header with the scheme set to `Bearer` or a special header named `X-JWT-Assertion`as shown below:

```Authorization: Bearer <YOUR-JWT-TOKEN>```

OR

```X-JWT-Assertion: <YOUR-JWT-TOKEN>```

Upon receiving the HTTP request, OpenMRS reads the JWT from the header and verifies its signature. If the signature can be verified it goes ahead and reads the username from the JWT payload and then uses it to authenticate the request using the module's OAuth 2.0-based authentication scheme. **This assumes a user account already exists in OpenMRS with the specified username.**

#### Configuration
OpenMRS needs a key to verify the signature of a JWT. For enhanced security, the module only 
supports asymmetric algorithms. Currently, only RSA-based algorithms (namely RS256, RS384, RS512, PS256, PS384, PS512) are 
supported. Therefore, you need to provide a public key from the identity provider to be used to verify the JWT 
signatures. The public key can be configured in 3 ways and below is the lookup order:
1. From the **oauth2.properties** file as the value of the `publicKey` property.
2. From a specific file located in the application data directory or its subdirectories, this file is configured via the **oauth2.properties** file as the value of the `publicKeyFilename` property.
3. The module fetches all known keys from the identity provider at the URL configured as the value of the `keysUrl` property in the **oauth2.properties** file.

## IdP Configuration Guides

1. [Guide for Keycloak](readme/Keycloak.md)
2. [Guide for Google API](readme/GoogleAPI.md)
    * This guide may be slightly outdated, see [this post](https://talk.openmrs.org/t/oauth-2-login-module-installation-error/38228/15?u=mksd) for complementary instructions.

## OpenMRS Platform Requirements
OpenMRS Core 2.2.1 or above.
