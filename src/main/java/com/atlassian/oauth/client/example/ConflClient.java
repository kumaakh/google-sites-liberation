package com.atlassian.oauth.client.example;

import java.awt.Desktop;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.client.HttpResponseException;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.json.JSONArray;
import org.json.JSONObject;


/**
 * @since v1.0
 */
public class ConflClient
{
	private final Log log;
	private static final String ENCODING = "utf-8";
    private static final String CALLBACK_URI = "http://consumer/callback";
    protected String CONSUMER_KEY;
    protected String CONSUMER_PRIVATE_KEY;
    protected String accessToken;
    protected String baseURL;
    protected String spaceKey;
    
	//https://mamigo.atlassian.net/wiki/rest/api/content?spaceKey=OL&Title=OldSites+Home
    private String contentURL(String pageTitle) throws UnsupportedEncodingException{
    	return baseURL+"/rest/api/content?spaceKey="+spaceKey+"&Title="+URLEncoder.encode(pageTitle,ENCODING)+"&expand=";
    }
    private String contentURL(){
    	return baseURL+"/rest/api/content";
    }
    private String attachmentURL(String pageId){
    	return baseURL+"/rest/api/content/"+pageId+"/child/attachment";
    }
    private String contentByIdURL(String pageId) throws UnsupportedEncodingException{
    	return baseURL+"/rest/api/content/"+pageId+"?expand=body.storage";
    }
    private AtlassianOAuthClient client(){
    	return  new AtlassianOAuthClient(CONSUMER_KEY, CONSUMER_PRIVATE_KEY, baseURL, CALLBACK_URI,accessToken); 
    }
    public void setBaseURL(String baseURL) {
		this.baseURL = baseURL;
	}
	public void setAccessToken(String accessToken) {
		this.accessToken = accessToken;
	}
	public String getAccessToken() {
		return accessToken;
	}
	public void setSpaceKey(String spaceKey) {
		this.spaceKey = spaceKey;
	}
	public ConflClient() throws Exception
    {
    	this.log = LogFactory.getLog(getClass());
    	Properties props= new Properties();
    	props.loadFromXML(new FileInputStream("ConflClient.xml"));
    	CONSUMER_PRIVATE_KEY=props.getProperty("CONSUMER_PRIVATE_KEY");
    	CONSUMER_KEY=props.getProperty("CONSUMER_KEY");
    	accessToken=props.getProperty("accessToken");
    	baseURL=props.getProperty("baseURL");
    }

    public void fetchAuthToken() throws Exception
    {
    	LocalServerCallbackReciever localServerReceiver = new LocalServerCallbackReciever.Builder().setPort( 8081 ).build();
    	// fetch client token and secret 

        AtlassianOAuthClient jiraoAuthClient = new AtlassianOAuthClient(CONSUMER_KEY, CONSUMER_PRIVATE_KEY, baseURL,localServerReceiver.getRedirectUri());
        TokenSecretVerifierHolder requestToken = jiraoAuthClient.getRequestToken();
        String authorizeUrl = jiraoAuthClient.getAuthorizeUrlForToken(requestToken.token);
        log.info("launching browser for "+ authorizeUrl);
        Desktop.getDesktop().browse(new URI(authorizeUrl));
        
        try{
	        requestToken.verifier=localServerReceiver.waitForCode(requestToken.token);
	        accessToken = jiraoAuthClient.swapRequestTokenForAccessToken(requestToken);
	        log.info("access Token "+accessToken);
	    }
        finally{
        	//localServerReceiver.stop();
        }
        
    }
    
    public String getPageId(String pageTitle) throws Exception
    {
		String url=contentURL(pageTitle);
        String resp=client().makeAuthenticatedRequest(url);
        JSONObject page = new JSONObject(resp);
        handleErrors(page);
    	JSONArray results = page.getJSONArray("results");
    	for(int i=0;i<results.length();i++)
    	{
    		if(results.getJSONObject(i).getString("title").equals(pageTitle))
    			return results.getJSONObject(i).getString("id"); 
    	}
    	throw new Exception("No Page found");
    }
    public String getPageContentById(String pageId) throws Exception
    {
    	
		String url=contentByIdURL(pageId);
        String page= client().makeAuthenticatedRequest(url);
        handleErrors(new JSONObject(page));
        return page;
    }
    
    
	public String addPage(String parentId, String pageTitle, String bodyContent) throws Exception
    {
		String url=contentURL();
		String resp=client().makeAuthenticatedPost(url, makePageCotentRequest(parentId, pageTitle, bodyContent));
		JSONObject jsonPage= new JSONObject(resp);
		handleErrors(jsonPage);
		return jsonPage.getString("id");
    }
	/**
	 * 
	 * @param pageId
	 * @param fileName
	 * @param Comment
	 * @return attachmentId
	 * @throws Exception 
	 */
	public String makeAttachment(String pageId, String fileName, String comment, boolean isMinorEdit) throws Exception
    {
		String url=attachmentURL(pageId);
		MultipartEntityBuilder mpeb=MultipartEntityBuilder.create().addBinaryBody("file",new File(fileName));
		if(StringUtils.isNotEmpty(comment))
				mpeb.addTextBody("comment", comment);
		if(isMinorEdit)
			mpeb.addTextBody("minorEdit","true");
		String resp=client().makeAuthenticatedPostWithMultiPart(url, mpeb.build());
		JSONObject jsonPage= new JSONObject(resp);
		handleErrors(jsonPage);
    	JSONArray results = jsonPage.getJSONArray("results");
    	if (results.length()>0) return results.getJSONObject(0).getString("id"); 
    	throw new Exception("No attacmment created");
    }

	/**
	 * 
	 * @param pageId
	 * @param filesAndComments
	 * @param isMinorEdit
	 * @return
	 * @throws Exception
	 */
	public Collection<String> makeMultipleAttachments(String pageId, Collection<Map.Entry<String,String>> filesAndComments, boolean isMinorEdit) throws Exception
    {
		if(filesAndComments.size()==0) throw new Exception("at least one file should be specified");
		String url=attachmentURL(pageId);
		MultipartEntityBuilder mpeb=MultipartEntityBuilder.create();
		for(Map.Entry<String, String> e:filesAndComments)
		{
			mpeb.addBinaryBody("file",new File(e.getKey())).addTextBody("comment", e.getValue());
			if(isMinorEdit)
				mpeb.addTextBody("minorEdit","true");
		}
		
		String resp=client().makeAuthenticatedPostWithMultiPart(url, mpeb.build());
		JSONObject jsonPage= new JSONObject(resp);
		handleErrors(jsonPage);
		JSONArray results = jsonPage.getJSONArray("results");
		if (results.length()!=filesAndComments.size()) 
			throw new Exception("Created only "+results.length()+" attachments , though requested "+filesAndComments.size());
    	ArrayList<String> attIds = new ArrayList<String>();
    	for(int i=0;i<results.length();i++){
    		attIds.add(results.getJSONObject(i).getString("id"));
    	}
    	return attIds;
    }
	
	private String makePageCotentRequest(String parentId, String pageTitle,String bodyContent) throws Exception {

		JSONObject req = new JSONObject();
		req.put("type", "page");
		req.put("title", pageTitle);
			JSONObject space = new JSONObject();
			space.put("key", spaceKey);
		req.put("space", space);
			JSONObject body = new JSONObject();
			JSONObject storage = new JSONObject();
				storage.put("value", bodyContent);
				storage.put("representation", "storage");
			body.put("storage", storage);
		req.put("body", body);
			JSONArray ancestors= new JSONArray();
				JSONObject parent = new JSONObject();
				parent.put("id", parentId);
			ancestors.put(parent);
		req.put("ancestors", ancestors);
		return req.toString();
	}
	
	
	private void handleErrors(JSONObject resp) throws Exception
	{
		if(resp.has("statusCode"))
		{
			int code=resp.getInt("statusCode");
			String msg=resp.getString("message");
			if(code<200 || code>299)
			{
				throw new HttpResponseException(code, msg);
			}
			else if(code !=200)
			{
				log.warn("HTTP Status:"+code+" "+msg);
			}
		}
	}
	
}
