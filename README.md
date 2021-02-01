# OpenMRS OAuth 2.0 Login Module

## Description
This module delegates user authentication to an OAuth 2.0 resource provider. In effect it turns OpenMRS into an OAuth 2.0 client as soon as the module is installed and running.


## Overview
It suffices to install the module for OpenMRS' default authentication scheme to become inactive and for the module custom authentication scheme to take over.

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
1. The usual OAuth 2 properties:
    * The [client ID and secret](https://www.oauth.com/oauth2-servers/client-registration/client-id-secret/).
    * A couple of URIs to transact with the OAuth 2 provider: user authorization URI, access token URI and user info URI.

2. OpenMRS users properties mappings with the OAuth 2 'user info'.
<br/>For new users the master information is first maintained with the OAuth 2 provider, starting with their _username_. This information is obtained through a JSON response from the user info URI. A simple one-to-one mapping between what is needed from an OpenMRS user's perspective and what is given by the OAuth 2 provider can be provided through the OAuth 2 properties file.

The module ships with test resources that show how the OAuth 2 properties file should look like when using JBoss' Keycloak and Google API as OAuth 2 providers, see [here](./omod/src/test/resources/).

## Authentication Mechanism
#### Overview
OpenMRS requires persisted OpenMRS users with roles to perform actions within the application. For the OAuth 2 provider to be able to take care of authentication there has to be a duplication of users in both systems. A user will exist both with the OAuth 2 provider and the corresponding user will also exist within OpenMRS \*.

The authentication is based on the **username**.

<sub>\* _This duplication of users could be avoided if OpenMRS was fully leveraging Spring Security. This is not yet the case and as of now authorization is made based on users that are persisted and accessed through the DAO layer._</sub>

#### On-the-fly user creation
However at first the user might not exist yet in OpenMRS and as a convenience the module will create new OpenMRS users on the fly. This is why a mapping mechanism must exist between the OAuth 2 provider users and the OpenMRS users, in particular to find out what the OpenMRS username should be.

#### Initial set of roles
A list of OpenMRS role can be provided through the user info JSON so that they get assigned to a first time logged in user. This can be done through the `openmrs.mapping.user.roles` mapping property that holds a pointer to the user info JSON key whose value is a comma-separated list of OpenMRS role names.

The user info JSON might look like that:
```json
{
  "sub": "4e3074d6-5e9f-4707-84f1-ccb2aa2ab3bc",
  "email": "jdoe@example.com",
  "creation_roles": "Nurse, Clinical Advisor"
}
```
With an OAuth 2 properties mapping set as such:
```
openmrs.mapping.user.roles=creation_roles
```
Where "Nurse" and "Clinical Advisor" are expected to be OpenMRS role _names_. It is important to note that role names that cannot be found in OpenMRS will cause a user creation error, with the consequence that the user will not be able to log into OpenMRS.

This is a convenience to streamline the first login experience with OpenMRS, in order for new user to not be role-less. **This is not an external management of user roles for OpenMRS**.

Finally this convenience can only work when we have control over the what the user info response JSON can return.

#### Example
If a user authenticates as 'jdoe' with the OAuth 2 provider, OpenMRS will attempt to fetch the user 'jdoe'.
* If a 'jdoe' user can be found in OpenMRS, then it will become the authenticated user.
* If a 'jdoe' user cannot be found in OpenMRS, it will be created with a initial set of roles and will become the authenticated user.

## Redirect URL after successful login
By default the user will be redirected to the root URL `/` after a successul login. The redirect URL can be modified by using the global property (GP) `oauth2login.redirectUriAfterLogin`.
For example when the module is used within the Reference Application with the two-screen login enabled, this GP can be used to enforce a redirect to the login GSP page (hence kicking in its Java controller logic):
```
/referenceapplication/login.page?redirectUrl=/index.html
```

## Configuration Guides

1. [Guide for Keycloak](readme/Keycloak.md)
2. [Guide for Google API](readme/GoogleAPI.md)

## Requirements
OpenMRS Core 2.2.1 or Core 2.3.0 and above.
