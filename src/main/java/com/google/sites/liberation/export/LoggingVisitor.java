package com.google.sites.liberation.export;

import com.google.gdata.data.sites.AttachmentEntry;
import com.google.gdata.data.sites.BasePageEntry;
import com.google.gdata.data.sites.CommentEntry;
import com.google.gdata.data.sites.ListItemEntry;
import com.google.gdata.data.sites.WebAttachmentEntry;
import com.google.sites.liberation.util.ProgressListener;

public class LoggingVisitor extends EntryStoreVisitor {

	ProgressListener progressListener;
	String indent="";
	public LoggingVisitor(ProgressListener progressListener)
	{
		this.progressListener=progressListener;
	}
	@Override
	protected void visitPageBefore(BasePageEntry<?> e) throws Exception {
		logBefore(e);
		super.visitPageBefore(e);
	}
	protected void logBefore(BasePageEntry<?> e) {
		progressListener.setStatus(indent+"create page "+e.getTitle().getPlainText());
		indent=indent+".";
	}

	@Override
	protected void visitPageAfter(BasePageEntry<?> e) {
		logAfter(e);
		super.visitPageAfter(e);
	}
	protected void logAfter(BasePageEntry<?> e) {
		indent=indent.substring(0, indent.length()-1);
		progressListener.setStatus(indent+"done "+e.getTitle().getPlainText());
	}

	@Override
	protected void visitListItem(ListItemEntry c) {
		progressListener.setStatus(indent+"append list item "+c.getTitle().getPlainText());
		super.visitListItem(c);
	}

	@Override
	protected void visitComment(CommentEntry c) {
		progressListener.setStatus(indent+"append comment "+c.getTitle().getPlainText());
		super.visitComment(c);
	}

	@Override
	protected void visitAttachment(AttachmentEntry c) throws Exception {
		progressListener.setStatus(indent+"make attachment "+c.getTitle().getPlainText());
		super.visitAttachment(c);
	}

	@Override
	protected void visitAttachment(WebAttachmentEntry c) {
		progressListener.setStatus(indent+"make web attachment "+c.getTitle().getPlainText());
		super.visitAttachment(c);
	}

}
