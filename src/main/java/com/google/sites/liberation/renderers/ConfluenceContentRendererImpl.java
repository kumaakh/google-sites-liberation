/*
 * Copyright (C) 2009 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.sites.liberation.renderers;

import java.io.StringReader;
import java.io.StringWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import com.google.gdata.data.sites.AttachmentEntry;
import com.google.gdata.data.sites.BaseContentEntry;
import com.google.gdata.data.sites.BasePageEntry;
import com.google.gdata.data.sites.CommentEntry;
import com.google.gdata.data.sites.ListItemEntry;
import com.google.gdata.data.sites.WebAttachmentEntry;
import com.google.sites.liberation.export.EntryStore;
import com.google.sites.liberation.export.EntryStore.NewPage;
import com.google.sites.liberation.util.EntryType;
import com.google.sites.liberation.util.EntryUtils;
import com.google.sites.liberation.util.XmlElement;

/**
 * Renders a page's main content.
 * 
 * @author bsimon@google.com (Benjamin Simon)
 */
final class ConfluenceContentRendererImpl implements ContentRenderer {
	
	
	DocumentBuilder builder;
	XPathExpression xPathEx;
	Transformer transformer;
  public ConfluenceContentRendererImpl() {
		super();
		DocumentBuilderFactory factory =DocumentBuilderFactory.newInstance();
		try{
			builder = factory.newDocumentBuilder();
			xPathEx =  XPathFactory.newInstance().newXPath().compile("/div/table/tbody/tr/td/div");
			transformer = TransformerFactory.newInstance().newTransformer();
		    transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
		    
		}
		catch(Exception ex)
		{
			throw new Error(ex);
		}
	}

@Override
  public XmlElement renderContent(BaseContentEntry<?> entry, EntryStore store, boolean revisionsExported, URL siteURL) {
	  String xml = reformat(entry,store,siteURL);
	  XmlElement div = new XmlElement("div");
	  div.addXml(xml);
      div.addElement(new XmlElement("hr"));
      XmlElement info = new XmlElement("small");
      info.addElement(RendererUtils.makeOldSiteRef(entry));
      div.addElement(info);
      appendChildPageTree(entry,store,div);
      return div;
  }

	private void appendChildPageTree(BaseContentEntry<?> e, EntryStore store, XmlElement div) {
		Collection<BaseContentEntry<?>> children = store.getChildren(e.getId());
		if(children.isEmpty()) return;
		boolean hasChildren=false;
		boolean hasAtt=false;
		for(BaseContentEntry<?> c: store.getChildren(e.getId()))
		{
			switch (EntryType.getType(c))
			{
			case WEB_ATTACHMENT:
			case ATTACHMENT:
				hasAtt=true;
				break;
			case ANNOUNCEMENT:
			case ANNOUNCEMENTS_PAGE:
			case FILE_CABINET_PAGE:
			case LIST_PAGE:
			case WEB_PAGE:
			case OTHER:
				hasChildren=true;
				break;
			}
		}
		if(hasChildren)
		{
			div.addElement(new XmlElement("hr"));
			div.addXml("<h4>Child Pages</h4>");
			div.addXml("<ac:structured-macro ac:name='children' ac:schema-version='2' ac:macro-id='c5781a7d-c17a-4b73-b23c-895b0604bb94'/>");
		}
		if(hasAtt)
		{
			div.addElement(new XmlElement("hr"));
			div.addXml("<h4>Attachments</h4>");
			div.addXml("<ac:structured-macro ac:name='attachments' ac:schema-version='1' ac:macro-id='11566e9a-401e-4f3d-88d6-3f3be648e712'><ac:parameter ac:name='preview'>false</ac:parameter><ac:parameter ac:name='old'>false</ac:parameter></ac:structured-macro>");
		}
	}

	private String reformat(BaseContentEntry<?> entry, EntryStore store, URL siteURL) {
		String e= EntryUtils.getXhtmlContent(entry);
		try{
			
			Document doc = builder.parse(new InputSource(new StringReader(e)));
			{
				NodeList linkNodes = doc.getElementsByTagName("a");
				ArrayList<Node> nodes= new ArrayList<Node>();
				for(int i=0;i<linkNodes.getLength();i++)
				{
					nodes.add(linkNodes.item(i));
				}
			
				for(Node link:nodes)
				{
					fixLink(link,entry, store, doc,siteURL);
				}
			}
			//take out the useful part of page.
			Node node= (Node)xPathEx.evaluate(doc,XPathConstants.NODE);
			doc.getDocumentElement().setAttribute("xmlns:ri", "http://example/ri");
			doc.getDocumentElement().setAttribute("xmlns:ac", "http://example/ac");
			NodeList nodes= doc.getDocumentElement().getChildNodes();
			for(int i=0;i<nodes.getLength();i++)
			{
				doc.getDocumentElement().removeChild(nodes.item(0));
			}
			if(node!=null) 
				doc.getDocumentElement().appendChild(node);
			StreamResult xmlOutput = new StreamResult(new StringWriter());
		    transformer.transform(new DOMSource(doc), xmlOutput);
		    return xmlOutput.getWriter().toString();
			
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
			return e; //as-is
		}
	}

	private boolean fixLink(Node link, BaseContentEntry<?> entry, EntryStore store, Document doc, URL siteURL) throws Exception {
		Node asIsLinkhRef=link.getAttributes().getNamedItem("href");
		if(asIsLinkhRef==null) {
			return false;
		}
		String asIsLink=asIsLinkhRef.getNodeValue();
		URL asIsURL=null;
		try{
			asIsURL=new URL(asIsLink);
			asIsURL = new URL(asIsURL.getProtocol(),asIsURL.getHost(),asIsURL.getPort(),asIsURL.getPath());//remove ? and #
		}
		catch(Exception ex)
		{
			System.out.println("Can not handle "+asIsLink);
			return false;
		}
		String asIslinkBody=link.getTextContent();
		if(asIsLink.startsWith(siteURL.toString()))
		{
			System.out.println("Fixing "+asIsLink);
			
			NewPage page=store.getCreatedPage(asIsURL.toString());
			String attLink=store.getNewAttachmentLink(asIsURL.toString());
			boolean isImage=isImage(link);
			if(page!=null || (attLink!=null && !isImage))
			{
				//<ac:link><ri:page ri:content-title="Projects" /><ac:plain-text-link-body><![CDATA[Products]]></ac:plain-text-link-body></ac:link>
				Element n1 = doc.createElementNS("", "ac:link");
				Element n2 = doc.createElementNS("", (page!=null)?"ri:page":"ri:attachment");
				Element n3 = doc.createElementNS("", "ac:plain-text-link-body");
				n3.appendChild(doc.createCDATASection(asIslinkBody));
				if((page!=null))
					n2.setAttributeNS("","ri:content-title", page.title);
				else
					n2.setAttributeNS("","ri:filename", attLink);
				n2.appendChild(n3);
				n1.appendChild(n2);
				Node p=link.getParentNode();
				p.replaceChild(n1,link);
				return true;
			}
			else if(attLink!=null && isImage)
			{
				System.out.println("Fixing as an attached image: "+attLink);
				//<ac:image ac:height="250"><ri:attachment ri:filename="IMAG0037.jpg.1347311362011.jpg" /></ac:image>
				Element n1 = doc.createElementNS("", "ac:image");
				Element n2 = doc.createElementNS("", "ri:attachment");
				n2.setAttributeNS("","ri:filename", attLink);
				n1.appendChild(n2);
				Node p=link.getParentNode();
				p.replaceChild(n1,link);
				return true;
			}
			else
			{
				System.out.println("Can not fix :"+asIsURL.toString());
			}
		}
		return false;
		
	}

	private boolean isImage(Node link) {
		NodeList nodes= link.getChildNodes();
		for(int i=0;i<nodes.getLength();i++)
		{
			if(nodes.item(i).getNodeName().equalsIgnoreCase("img"))
				return true;
		}
		return false;
	}
	
}
