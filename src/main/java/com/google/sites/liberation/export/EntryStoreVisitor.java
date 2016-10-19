package com.google.sites.liberation.export;

import java.util.Collection;

import com.google.gdata.data.sites.AttachmentEntry;
import com.google.gdata.data.sites.BaseContentEntry;
import com.google.gdata.data.sites.BasePageEntry;
import com.google.gdata.data.sites.CommentEntry;
import com.google.gdata.data.sites.ListItemEntry;
import com.google.gdata.data.sites.WebAttachmentEntry;
import com.google.sites.liberation.util.EntryType;

public class EntryStoreVisitor {

	EntryStore store;
	public void visit(EntryStore store)throws Exception 
	{
		this.store=store;
		visitPageColl(store.getTopLevelEntries());
	}
	private void visitPageColl(Collection<BasePageEntry<?>> c)throws Exception 
	{
		if(c==null) return;
		for (BasePageEntry<?> e: c) {
			visitPage(e);
		}
	}
	private void visitPage(BasePageEntry<?> e) throws Exception {
		visitPageBefore(e);
		visitPageChildren(e);
		visitPageAfter(e);
	}

	private void visitPageChildren(BasePageEntry<?> e) throws Exception {
		for(BaseContentEntry<?> c: store.getChildren(e.getId()))
		{
			switch (EntryType.getType(c))
			{
			
			case WEB_ATTACHMENT:
				visitAttachment((WebAttachmentEntry)c);
				break;
			case ATTACHMENT:
				visitAttachment((AttachmentEntry)c);
				break;
			case COMMENT:
				visitComment((CommentEntry)c);
				break;
			case LIST_ITEM:
				visitListItem((ListItemEntry)c);
				break;
			case ANNOUNCEMENT:
			case ANNOUNCEMENTS_PAGE:
			case FILE_CABINET_PAGE:
			case LIST_PAGE:
			case WEB_PAGE:
			case OTHER:
			default:
				visitPage((BasePageEntry<?>)c);
				break;
			}
		}
		
	}
	protected void visitPageBefore(BasePageEntry<?> e) throws Exception {
		// TODO Auto-generated method stub
		
	}
	protected void visitPageAfter(BasePageEntry<?> e) {
		// TODO Auto-generated method stub
		
	}
	protected void visitListItem(ListItemEntry c) {
		// TODO Auto-generated method stub
		
	}
	protected void visitComment(CommentEntry c) {
		// TODO Auto-generated method stub
		
	}
	protected void visitAttachment(AttachmentEntry c) throws Exception {
		// TODO Auto-generated method stub
		
	}
	protected void visitAttachment(WebAttachmentEntry c) {
		// TODO Auto-generated method stub
		
	}
	
}
