/**
 * (c) Copyright IBM Corporation 2015, 2017.
 * This is licensed under the following license.
 * The Apache License, Version 2.0 (http://www.apache.org/licenses/LICENSE-2.0)
 * U.S. Government Users Restricted Rights:  Use, duplication or disclosure restricted by GSA ADP Schedule Contract with IBM Corp.
 */
package com.urbancode.ds.jenkins.plugins.urbandeploypublisher;

import com.urbancode.commons.util.IO;
import com.urbancode.ud.client.UDRestClient;

import hudson.AbortException;
import hudson.util.Secret;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;

import javax.ws.rs.core.UriBuilder;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.HttpResponse;
import org.apache.http.impl.client.DefaultHttpClient;
import org.kohsuke.stapler.DataBoundConstructor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class creates an HttpClient for running UCD rest commands
 *
 */
@SuppressWarnings("deprecation") // Triggered by DefaultHttpClient
public class UrbanDeploySite implements Serializable {
    public static final java.util.logging.Logger log = LoggerFactory.getLogger(UrbanDeploySite.class);

    private static final long serialVersionUID = -8723534991244260459L;

    private String profileName;

    private String url;

    private String user;

    private Secret password;

    private boolean trustAllCerts;

    transient private DefaultHttpClient client;

    /**
     * Instantiates a new UrbanDeploy site.
     */
    public UrbanDeploySite() {
    }

    /**
     * Necessary constructor to allow jenkins to treat the password as an encrypted value
     *
     * @param profileName
     * @param url the url of the UrbanDeploy instance
     * @param user
     * @param password
     * @param trustAllCerts
     */
    public UrbanDeploySite(
            String profileName,
            String url,
            String user,
            Secret password,
            boolean trustAllCerts)
    {
        this.profileName = profileName;
        this.url = url;
        this.user = user;
        this.password = password;
        this.trustAllCerts = trustAllCerts;
    }

    /**
     * Constructor used to bind json to matching parameter names in global.jelly
     *
     * @param profileName
     * @param url
     * @param user
     * @param password
     * @param trustAllCerts
     */
    @DataBoundConstructor
    public UrbanDeploySite(
            String profileName,
            String url,
            String user,
            String password,
            boolean trustAllCerts)
    {
        this(profileName, url, user, Secret.fromString(password), trustAllCerts);
    }

    public DefaultHttpClient getClient() {
        log.info("SOMETHING SOMETHING");
        client = UDRestClient.createHttpClient(user, password.toString(), trustAllCerts);
        log.info("SOMETHING SOMETHING 2");
        return client;
    }

    public DefaultHttpClient getTempClient(String tempUser, Secret tempPassword) {
        return UDRestClient.createHttpClient(tempUser, tempPassword.toString(), trustAllCerts);
    }

    /**
     * Gets the display name.
     *
     * @return the display name
     */
    public String getDisplayName() {
        if (StringUtils.isEmpty(profileName)) {
            return url;
        } else {
            return profileName;
        }
    }

    /**
     * Gets the profile name.
     *
     * @return the profile name
     */
    public String getProfileName() {
        return profileName;
    }

    /**
     * Sets the profile name.
     *
     * @param profileName
     *          the new profile name
     */
    public void setProfileName(String profileName) {
        this.profileName = profileName;
    }

    /**
     * Gets the url.
     *
     * @return the url
     */
    public String getUrl() {
        return url;
    }

    /**
     * Sets the url.
     *
     * @param url
     *          the new url
     */
    public void setUrl(String url) {
        this.url = url;
        if (this.url != null) {
            this.url = this.url.replaceAll("\\\\", "/");
        }
        while (this.url != null && this.url.endsWith("/")) {
            this.url = this.url.substring(0, this.url.length() - 2);
        }
    }

    public URI getUri() throws AbortException {
        URI udSiteUri;

        try {
            udSiteUri = new URI(url);
        }
        catch (URISyntaxException ex) {
            throw new AbortException("URL " + url + " is malformed: " + ex.getMessage());
        }

        return udSiteUri;
    }

    /**
     * Gets the username.
     *
     * @return the username
     */
    public String getUser() {
        return user;
    }

    /**
     * Sets the username.
     *
     * @param username
     *          the new username
     */
    public void setUser(String user) {
        this.user = user;
    }

    /**
     * Gets the password.
     *
     * @return the password
     */
    public Secret getPassword() {
        return password;
    }

    /**
     * Sets the password.
     *
     * @param password
     *          the new password
     */
    public void setPassword(Secret password) {
        this.password = password;
    }

    /**
     * Gets trustAllCerts
     *
     * @return if all certificates are trusted
     */
    public boolean isTrustAllCerts() {
        return trustAllCerts;
    }

    /**
     * Sets trustAllCerts to trust all ssl certificates or not
     *
     * @param trustAllCerts
     */
    public void setTrustAllCerts(boolean trustAllCerts) {
        this.trustAllCerts = trustAllCerts;
    }

    /**
     * Test whether the client can connect to the UCD site
     *
     * @throws Exception
     */
    public void verifyConnection() throws Exception {
        URI uri = UriBuilder.fromPath(url).path("rest").path("state").build();
        executeJSONGet(uri);
    }

    /**
     * Execute an HTTP GET request to the UCD server
     *
     * @param uri
     * @throws Exception
     */
    private void executeJSONGet(URI uri) throws Exception {
        String result = null;
        HttpClient client = getClient();
        HttpGet method = new HttpGet(uri.toString());
        try {
            HttpResponse response = client.execute(method);
            int responseCode = response.getStatusLine().getStatusCode();
            if (responseCode == 401) {
                throw new Exception("Error connecting to IBM UrbanCode Deploy: Invalid user and/or password");
            }
            else if (responseCode != 200) {
                throw new Exception("Error connecting to IBM UrbanCode Deploy: " + responseCode + "using URI: " + uri.toString());
            }
        }
        finally {
            method.releaseConnection();
        }
    }
}
