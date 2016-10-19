package com.google.sites.liberation.export;

import java.io.File;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

import com.atlassian.oauth.client.example.ConflClient;
import com.google.gdata.client.sites.SitesService;
import com.google.gdata.data.Link;
import com.google.gdata.data.Person;
import com.google.gdata.data.sites.AttachmentEntry;
import com.google.gdata.data.sites.BaseContentEntry;
import com.google.gdata.data.sites.BasePageEntry;
import com.google.gdata.data.sites.CommentEntry;
import com.google.gdata.data.sites.ListItemEntry;
import com.google.gdata.data.sites.WebAttachmentEntry;
import com.google.gdata.util.common.base.Pair;
import com.google.sites.liberation.export.EntryStore.NewPage;
import com.google.sites.liberation.util.ProgressListener;

public class ConflUploadVisitor extends LoggingVisitor {
	public static final String rootpageID="4785797";
	public static final String testSpace="OS";
	ConflClient client;
	Stack<String> pagePath = new Stack<String>();
	String currentPageId;
	PageExporter pageExporter;
	SitesService sitesService;
	AttachmentDownloader attachmentDownloader;
	File rootDirectory;
	int totalEntries;
	int doneEntries=0;
	boolean createPageContent=true;
	NewPage currentNewPage;
	URL siteURL;
	

	
	public void setCreatePageContent(boolean createPageContent) {
		this.createPageContent = createPageContent;
	}

	public ConflUploadVisitor(AttachmentDownloader attachmentDownloader, PageExporter pageExporter, SitesService sitesService, 
			File rootDirectory, ProgressListener progressListener, int totalEntries, URL siteURL)
			throws Exception {
		super(progressListener);
		this.attachmentDownloader = attachmentDownloader;
		this.sitesService = sitesService;
		this.pageExporter=pageExporter;
		this.rootDirectory = rootDirectory;
		this.totalEntries=totalEntries;
		this.siteURL=siteURL;
		
		client = new ConflClient();
		
		//TODO improve
		client.setSpaceKey(testSpace);
		currentPageId = rootpageID;
	}

	@Override
	protected void visitPageBefore(BasePageEntry<?> e) throws Exception {
		StringBuffer sb = new StringBuffer();
		pagePath.push(currentPageId);
		if(createPageContent){
			pageExporter.exportPage(e, store, sb, false,siteURL);
		}
		currentPageId = client.addPage(currentPageId, e.getTitle().getPlainText(), sb.toString());
		currentNewPage= store.addCreatedPage(e, currentPageId,1);

		super.visitPageBefore(e);
	}

	@Override
	protected void visitPageAfter(BasePageEntry<?> e) {
		currentPageId = pagePath.pop();
		stepProgress();
		super.visitPageAfter(e);
	}

	@Override
	protected void visitListItem(ListItemEntry c) {
		// TODO Auto-generated method stub
		super.visitListItem(c);
	}

	@Override
	protected void visitComment(CommentEntry c) {
		// TODO Auto-generated method stub
		super.visitComment(c);
	}

	@Override
	protected void visitAttachment(AttachmentEntry attachment) throws Exception {
		BasePageEntry<?> parent = store.getParent(attachment.getId());
		File folder = new File(rootDirectory, parent.getPageName().getValue());
		folder.mkdirs();
		String fName=attachment.getTitle().getPlainText();
		File file = new File(folder, fName);

		attachmentDownloader.download(attachment, file, sitesService);

		String comment = makeOldSiteAttachRef(attachment);
		client.makeAttachment(currentPageId, file.getAbsolutePath(), comment, true);
		store.addUploadedAttachment(attachment.getLink(Link.Rel.ALTERNATE,null).getHref(), fName);
		stepProgress();
		super.visitAttachment(attachment);
	}

	@Override
	protected void visitAttachment(WebAttachmentEntry c) {
		// TODO Auto-generated method stub
		super.visitAttachment(c);
	}
	private void stepProgress()
	{
		doneEntries++;
		if(doneEntries%100==0)
		{
			progressListener.setProgress(doneEntries/(1.0*totalEntries));
		}
	}
	static String makeOldSiteAttachRef(BaseContentEntry<?> entry) {
		Person author = entry.getAuthors().get(0);
		String name = author.getName();
		String email = author.getEmail();
		if(name==null)
			name=email;
		String updated = entry.getUpdated().toUiString();
		int rev = 1;
		if (entry.getRevision() != null) {
			rev = entry.getRevision().getValue();
		}
		return String.format("Attached rev(%d) by %s on %s", rev, name, updated);
	}

}
