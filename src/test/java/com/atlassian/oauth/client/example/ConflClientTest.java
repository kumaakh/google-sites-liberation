package com.atlassian.oauth.client.example;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isEmptyOrNullString;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;

import org.apache.commons.io.FileUtils;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.sites.liberation.export.ConflUploadVisitor;


public class ConflClientTest {

	static ConflClient client;
	static String rootPage=ConflUploadVisitor.rootpageID;
	@BeforeClass
	public static void setUp() throws Exception
	{
		if(client==null)
		{
			client= new ConflClient();
			client.setSpaceKey(ConflUploadVisitor.testSpace);
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
		assertEquals(rootPage, client.getPageId("Old Sites Home"));
	}
	

	@Test
	public void testAddPage() throws Exception {
		String id=client.addPage(rootPage, "Hello world Page", "<p>Hello world</p>");
		assertThat(id,not(isEmptyOrNullString()));
		
		String page=client.getPageContentById(id);
		assertThat(page,not(isEmptyOrNullString()));
		
		int v=client.updatePage(id, 2, "Hello world 2", "<p>Hello world 23</p>");
		assertThat(v,is(2));
		
		String attId=client.makeAttachment(id, "anImg.jpeg", "added a new image", true);
		assertThat(attId,not(isEmptyOrNullString()));
		
	}
	
	@Test
	public void testMakeAttachment() throws Exception {
		String attId=client.makeAttachment(rootPage, "anImg.jpeg", "added a new image", true);
		assertThat(attId,not(isEmptyOrNullString()));
		
	}
	
	@Test
	public void testMakeMultipleAttachments() throws Exception {
		
		String id=client.addPage(rootPage, "Page with two attachments", "<p>refer other page <ac:link><ri:page ri:content-title='Hello world Page' /></ac:link></p><p><br /></p><p>link to attachment&nbsp;<ac:link><ri:attachment ri:filename='bImg.jpeg' /></ac:link></p><p><br /></p><p><ac:link><ri:attachment ri:filename='anImg.jpeg' /><ac:plain-text-link-body><![CDATA[link]]></ac:plain-text-link-body></ac:link> to att</p><p><br /></p><p>inlined image</p><p><ac:image><ri:attachment ri:filename='bImg.jpeg' /></ac:image></p><p><br /></p>");
		assertThat(id,not(isEmptyOrNullString()));
		
		String page=client.getPageContentById(id);
		assertThat(page,not(isEmptyOrNullString()));
		
		HashMap<String, String> atts= new HashMap<String, String>();
		atts.put("anImg.jpeg", "image #1");
		atts.put("bImg.jpeg", "image #2");
		
		Collection<String> attIds=client.makeMultipleAttachments(id, atts.entrySet(), true);
		assertThat(attIds.size(), is(2));
		
	}
	@Test
	public void testComplexPage() throws Exception {
		String id=client.addPage(rootPage, "How to update old Ubuntu Packages 2",FileUtils.readFileToString(new File("test3.txt"),"UTF-8"));
		assertThat(id,not(isEmptyOrNullString()));
	}

}
