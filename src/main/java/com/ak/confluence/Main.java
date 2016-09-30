package com.ak.confluence;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;

/**
 * Demonstrates how to update a page using the Confluence 5.5 REST API.
 */
public class Main
{
	//https://mamigo.atlassian.net/wiki/display/OL/OldSites+Home
    private static final String BASE_URL = "https://mamigo.atlassian.net/wiki";
    private static final String USERNAME = "akhil.kumar@mamigoinc.com";
    private static final String PASSWORD = "";
    private static final String ENCODING = "utf-8";

    private static String postContentUrl()
    {
    	return String.format("%s/rest/api/content", BASE_URL);
    }
    private static String getContentRestUrl(final String spaceKey, final String title, final String[] expansions) throws UnsupportedEncodingException
    {
        final String expand = URLEncoder.encode(StringUtils.join(expansions, ","), ENCODING);

        return String.format("%s/rest/api/content?spaceKey=%s&Title=%s&expand=%s", BASE_URL, spaceKey,title ,expand);
        //return String.format("%s/rest/api/content?spaceKey=%s&Title=%s&os_authType=basic&os_username=%s&os_password=%s", BASE_URL, spaceKey,title ,URLEncoder.encode(USERNAME, ENCODING), URLEncoder.encode(PASSWORD, ENCODING));
    }

    public static void main(final String[] args) throws Exception
    {

    	HttpClient client = HttpClientBuilder.create().setDefaultCredentialsProvider(credentialsProvider)
    	
    	HttpClient client = new DefaultHttpClient();

        // Get current page version
        String pageObj = null;
        HttpEntity pageEntity = null;
        try
        {
            HttpGet getPageRequest = new HttpGet(getContentRestUrl("OL","OldSites+Home", new String[] {"body.storage", "version", "ancestors"}));
            
            getPageRequest.addHeader(BasicScheme.authenticate( new UsernamePasswordCredentials(USERNAME, PASSWORD),ENCODING,false));
            
            HttpResponse getPageResponse = client.execute(getPageRequest);
            pageEntity = getPageResponse.getEntity();

            pageObj = IOUtils.toString(pageEntity.getContent());

            System.out.println("Get Page Request returned " + getPageResponse.getStatusLine().toString());
            System.out.println("");
            System.out.println(pageObj);
        }
        finally
        {
            if (pageEntity != null)
            {
                EntityUtils.consume(pageEntity);
            }
        }

        // Parse response into JSON
        JSONObject page = new JSONObject(pageObj);

        // Update page
        // The updated value must be Confluence Storage Format (https://confluence.atlassian.com/display/DOC/Confluence+Storage+Format), NOT HTML.
        page.getJSONObject("body").getJSONObject("storage").put("value", "hello, world");

        int currentVersion = page.getJSONObject("version").getInt("number");
        page.getJSONObject("version").put("number", currentVersion + 1);

        // Send update request
        HttpEntity putPageEntity = null;

        try
        {
        	HttpPost postPageReq= new HttpPost(postContentUrl());

        	StringEntity entity = new StringEntity(page.toString(), ContentType.APPLICATION_JSON);
        	postPageReq.setEntity(entity);

            HttpResponse putPageResponse = client.execute(postPageReq);
            putPageEntity = putPageResponse.getEntity();

            System.out.println("Put Page Request returned " + putPageResponse.getStatusLine().toString());
            System.out.println("");
            System.out.println(IOUtils.toString(putPageEntity.getContent()));
        }
        finally
        {
            EntityUtils.consume(putPageEntity);
        }
    }
}
