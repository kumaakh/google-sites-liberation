package com.google.sites.liberation.export;

import java.net.URL;

import com.google.gdata.data.sites.AttachmentEntry;
import com.google.gdata.data.sites.BasePageEntry;
import com.google.gdata.data.sites.CommentEntry;
import com.google.gdata.data.sites.ListItemEntry;
import com.google.gdata.data.sites.WebAttachmentEntry;
import com.google.sites.liberation.export.EntryStore.NewPage;
import com.google.sites.liberation.util.ProgressListener;

public class ConflUpdateVisitor extends ConflUploadVisitor {

	
	public ConflUpdateVisitor(ConflUploadVisitor that)
			throws Exception {
		super(that.attachmentDownloader,that.pageExporter,that.sitesService,that.rootDirectory,that.progressListener,that.totalEntries, that.siteURL);
	}
	
	public ConflUpdateVisitor(PageExporter pageExporter, ProgressListener progressListener, int totalEntries, URL siteURL)
			throws Exception {
		super(null,pageExporter,null,null,progressListener,totalEntries, siteURL);
	}

	@Override
	protected void visitPageBefore(BasePageEntry<?> e) throws Exception {
		pagePath.push(currentPageId);
		
		StringBuffer sb = new StringBuffer();
		
		pageExporter.exportPage(e, store, sb, false,siteURL);
		
		NewPage page=store.getCreatedPage(e);
		
		client.updatePage(page.id, 1+page.version, e.getTitle().getPlainText(), sb.toString());
		
		store.addCreatedPage(e, page.id, 1+page.version); //replaces

		currentPageId =page.id;
		logBefore(e);
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
		//not doing anything on visit attachment
	}

	@Override
	protected void visitAttachment(WebAttachmentEntry c) {
		//not doing anything on visit attachment
	}
}
