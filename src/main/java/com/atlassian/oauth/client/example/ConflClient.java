package com.atlassian.oauth.client.example;

import java.awt.Desktop;
import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

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
    protected static final String CONSUMER_KEY = "hardcoded-consumer";
    protected static final String CONSUMER_PRIVATE_KEY = "MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQDFkPMZQaTqsSXI+bSI65rSVaDzic6WFA3WCZMVMi7lYXJAUdkXo4DgdfvEBO21Bno3bXIoxqS411G8S53I39yhSp7z2vcB76uQQifi0LEaklZfbTnFUXcKCyfwgKPp0tQVA+JZei6hnscbSw8qEItdc69ReZ6SK+3LHhvFUUP1nLhJDsgdPHRXSllgZzqvWAXQupGYZVANpBJuK+KAfiaVXCgA71N9xx/5XTSFi5K+e1T4HVnKAzDasAUt7Mmad+1PE+56Gpa73FLk1Ww+xaAEvss6LehjyWHM5iNswoNYzrNS2k6ZYkDnZxUlbrPDELETbz/n3YgBHGUlyrXi2PBjAgMBAAECggEAAtMctqq6meRofuQbEa4Uq5cv0uuQeZLV086VPMNX6k2nXYYODYl36T2mmNndMC5khvBYpn6Ykk/5yjBmlB2nQOMZPLFPwMZVdJ2Nhm+naJLZC0o7fje49PrN2mFsdoZeI+LHVLIrgoILpLdBAz/zTiW+RvLvMnXQU4wdp4eO6i8J/Jwh0AY8rWsAGkk1mdZDwklPZZiwR3z+DDsDwPxFs8z6cE5rWJd2c/fhAQrHwOXyrQPsGyLHTOqS3BkjtEZrKRUlfdgV76VlThwrE5pAWuO0GPyfK/XCklwcNS1a5XxCOq3uUogWRhCsqUX6pYfAVS6xzX56MGDndQVlp7U5uQKBgQDyTDwhsNTWlmr++FyYrc6liSF9NEMBNDubrfLJH1kaOp590bE8fu3BG0UlkVcueUr05e33Kx1DMSFW72lR4dht1jruWsbFp6LlT3SUtyW2kcSet3fC8gySs2r6NncsZ2XFPoxTkalKpQ1atGoBe3XIKeT8RDZtgoLztQy7/7yANQKBgQDQvSHEKS5SttoFFf4YkUh2QmNX5m7XaDlTLB/3xjnlz8NWOweK1aVysb4t2Tct/SR4ZZ/qZDBlaaj4X9h9nlxxIMoXEyX6Ilc4tyCWBXxn6HFMSa/Rrq662Vzz228cPvW2XGOQWdj7IqwKO9cXgJkI5W84YtMtYrTPLDSjhfpxNwKBgGVCoPq/iSOpN0wZhbE1KiCaP8mwlrQhHSxBtS6CkF1a1DPm97g9n6VNfUdnB1Vf0YipsxrSBOe416MaaRyUUzwMBRLqExo1pelJnIIuTG+RWeeu6zkoqUKCAxpQuttu1uRo8IJYZLTSZ9NZhNfbveyKPa2D4G9B1PJ+3rSO+ztlAoGAZNRHQEMILkpHLBfAgsuC7iUJacdUmVauAiAZXQ1yoDDo0Xl4HjcvUSTMkccQIXXbLREh2w4EVqhgR4G8yIk7bCYDmHvWZ2o5KZtD8VO7EVI1kD0z4Zx4qKcggGbp2AINnMYqDetopX7NDbB0KNUklyiEvf72tUCtyDk5QBgSrqcCgYEAnlg3ByRd/qTFz/darZi9ehT68Cq0CS7/B9YvfnF7YKTAv6J2Hd/i9jGKcc27x6IMi0vf7zrqCyTMq56omiLdu941oWfsOnwffWRBInvrUWTj6yGHOYUtg2z4xESUoFYDeWwe/vX6TugL3oXSX3Sy3KWGlJhn/OmsN2fgajHRip0=";
    protected String accessToken;
    protected String baseURL;
    protected String spaceKey="OL";
    
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
    	return  new AtlassianOAuthClient(CONSUMER_KEY, CONSUMER_PRIVATE_KEY, null, CALLBACK_URI,accessToken); 
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
    public ConflClient()
    {
    	this.log = LogFactory.getLog(getClass());
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
