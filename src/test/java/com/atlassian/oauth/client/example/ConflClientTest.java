package com.atlassian.oauth.client.example;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isEmptyOrNullString;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.Collection;
import java.util.HashMap;

import org.junit.BeforeClass;
import org.junit.Test;

public class ConflClientTest {

	static ConflClient client;
	@BeforeClass
	public static void setUp() throws Exception
	{
		if(client==null)
		{
			client= new ConflClient();
			client.setSpaceKey("OL");
			//all properties are loaded from xml file
		}
		if(client.getAccessToken()==null)
		{
			client.fetchAuthToken();
		}
		assertNotNull(client.getAccessToken());
		assertTrue(client.getAccessToken().length()>10);
	}
	
	
	@Test
	public void testGetPageId() throws Exception {
		assertEquals("3178555", client.getPageId("OldSites Home"));
	}
	

	@Test
	public void testAddPage() throws Exception {
		String id=client.addPage("3178555", "Hello world Page", "<p>Hello world</p>");
		assertThat(id,not(isEmptyOrNullString()));
		
		String page=client.getPageContentById(id);
		assertThat(page,not(isEmptyOrNullString()));
		
		String attId=client.makeAttachment(id, "anImg.jpeg", "added a new image", true);
		assertThat(attId,not(isEmptyOrNullString()));
		
	}
	
	@Test
	public void testMakeAttachment() throws Exception {
		String attId=client.makeAttachment("3178555", "anImg.jpeg", "added a new image", true);
		assertThat(attId,not(isEmptyOrNullString()));
		
	}
	@Test
	public void testMakeMultipleAttachments() throws Exception {
		
		String id=client.addPage("3178555", "Page with two attachments", "<p>Hello world</p>");
		assertThat(id,not(isEmptyOrNullString()));
		
		String page=client.getPageContentById(id);
		assertThat(page,not(isEmptyOrNullString()));
		
		HashMap<String, String> atts= new HashMap<String, String>();
		atts.put("anImg.jpeg", "image #1");
		atts.put("bImg.jpeg", "image #2");
		
		Collection<String> attIds=client.makeMultipleAttachments(id, atts.entrySet(), true);
		assertThat(attIds.size(), is(2));
		
	}
}
