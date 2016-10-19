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

package com.google.sites.liberation.export;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.sites.liberation.util.EntryType.ATTACHMENT;
import static com.google.sites.liberation.util.EntryType.getType;
import static com.google.sites.liberation.util.EntryType.isPage;
import static com.google.sites.liberation.util.EntryUtils.getParentId;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.common.collect.Sets;
import com.google.gdata.client.sites.SitesService;
import com.google.gdata.data.PlainTextConstruct;
import com.google.gdata.data.TextConstruct;
import com.google.gdata.data.sites.AttachmentEntry;
import com.google.gdata.data.sites.BaseContentEntry;
import com.google.gdata.data.sites.BasePageEntry;
import com.google.gdata.util.common.base.Nullable;
import com.google.inject.Inject;
import com.google.sites.liberation.util.ProgressListener;
import com.google.sites.liberation.util.UrlUtils;

/**
 * Implements {@link SiteExporter} to export an entire Site 
 * to a given root folder.
 * 
 * @author bsimon@google.com (Benjamin Simon)
 */
final class ConfluenceSiteExporterImpl implements SiteExporter {
  
  private static final Logger LOGGER = Logger.getLogger(
      ConfluenceSiteExporterImpl.class.getCanonicalName());
  
  private final AbsoluteLinkConverter linkConverter;
  private final AppendableFactory appendableFactory;
  private final AttachmentDownloader attachmentDownloader;
  private final EntryStoreFactory entryStoreFactory;
  private final FeedProvider feedProvider;
  private final PageExporter pageExporter;
  private final RevisionsExporter revisionsExporter;
  
  /**
   * Creates a new SiteExporter with the given dependencies.
   */
  @Inject
  ConfluenceSiteExporterImpl(AbsoluteLinkConverter linkConverter,
      AppendableFactory appendableFactory,
      AttachmentDownloader attachmentDownloader,
      EntryStoreFactory entryStoreFactory,
      FeedProvider feedProvider,
      PageExporter pageExporter,
      RevisionsExporter revisionsExporter) {
    this.linkConverter = checkNotNull(linkConverter);
    this.appendableFactory = checkNotNull(appendableFactory);
    this.attachmentDownloader = checkNotNull(attachmentDownloader);
    this.entryStoreFactory = checkNotNull(entryStoreFactory);
    this.feedProvider = checkNotNull(feedProvider);
    this.pageExporter = checkNotNull(pageExporter);
    this.revisionsExporter = checkNotNull(revisionsExporter);   
  }
  
  @Override
  public void exportSiteContinue(String host,@Nullable String domain, String webspace, String tsCont,
	      boolean exportRevisions, SitesService sitesService, File rootDirectory, 
	      ProgressListener progressListener){
	  
	  InMemoryEntryStore entryStore = new InMemoryEntryStore(); 
	  int totalEntries = fetchFromSites(host, domain, webspace, null, sitesService, rootDirectory, progressListener, entryStore);
	  entryStore.load(tsCont);
	  
	  progressListener.setStatus("starting page contents updation...");
	  try{
	  URL siteUrl = UrlUtils.getSiteUrl(host, domain, webspace);
	    ConflUpdateVisitor uv= new ConflUpdateVisitor(pageExporter,progressListener,totalEntries,siteUrl);
	    uv.visit(entryStore);
	    progressListener.setStatus("page updation complete");
	  }
	  catch(Exception ex)
	  {
		  LOGGER.log(Level.SEVERE,"Can not export due to",ex);
	  }
	  finally
	  {
		  progressListener.setStatus("overwrting serialized data...");
		  entryStore.save(tsCont);
	  }
	  
  }
  
  @Override
  public void exportSite(String host, @Nullable String domain, String webspace, 
      boolean exportRevisions, SitesService sitesService, File rootDirectory, 
      ProgressListener progressListener) {
	  
	  exportSitePath(host, domain, webspace, null,exportRevisions, sitesService, rootDirectory,progressListener);
	  
  }
  @Override
  public void exportSitePath(String host, @Nullable String domain, String webspace, String path,
      boolean exportRevisions, SitesService sitesService, File rootDirectory, 
      ProgressListener progressListener) {
	  
	  InMemoryEntryStore entryStore = new InMemoryEntryStore(); 
	  
    int totalEntries = fetchFromSites(host, domain, webspace, path, sitesService, rootDirectory, progressListener, entryStore);
    progressListener.setStatus("Starting upload to confluence");
    try{
    	URL siteUrl = UrlUtils.getSiteUrl(host, domain, webspace);
  	    ConflUploadVisitor tv = new ConflUploadVisitor(attachmentDownloader, pageExporter, sitesService, rootDirectory,progressListener,totalEntries,siteUrl);
  	    tv.setCreatePageContent(false); //create empty titles
  	    tv.visit(entryStore);
  	    progressListener.setStatus("page creation complete...starting page contents");
  	    
  	    ConflUpdateVisitor uv= new ConflUpdateVisitor(tv);
  	    uv.visit(entryStore);
  	    progressListener.setStatus("page updation complete");

      }
      catch(Exception ex)
      {
      	LOGGER.log(Level.SEVERE,"Can not export due to",ex);
      	
      }
    finally{
    	entryStore.save();
    }

    
    
  }

protected int fetchFromSites(String host, String domain, String webspace, String path, SitesService sitesService, File rootDirectory,
		ProgressListener progressListener, InMemoryEntryStore entryStore) {
	checkNotNull(host, "host");
    checkNotNull(webspace, "webspace");
    checkNotNull(sitesService, "sitesService");
    checkNotNull(rootDirectory, "rootDirectory");
    checkNotNull(progressListener, "progressListener");
    
    
    URL	feedUrl = UrlUtils.getFeedUrl(host, domain, webspace);
    Set<BaseContentEntry<?>> pages = Sets.newHashSet();
    Set<AttachmentEntry> attachments = Sets.newHashSet();
    Set<String> pageTitles = Sets.newHashSet(); //check if we have name collisions
    
    progressListener.setStatus("Retrieving site data (this may take a few minutes).");
    Iterable<BaseContentEntry<?>> entries = feedProvider.getEntries(feedUrl,path ,sitesService);
    int num = 1;
    for (BaseContentEntry<?> entry : entries) {
      if (entry != null) {
        if (num % 20 == 0) {
          progressListener.setStatus("Retrieved " + num + " entries.");
        }
        entryStore.addEntry(entry);
        if (isPage(entry)) {
          pages.add((BasePageEntry<?>) entry);
          checkAndFixNameCollision(pageTitles,entry,progressListener);
        } else if (getType(entry) == ATTACHMENT) {
          // TODO(gk5885): remove extra cast for
          // http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6302214
          attachments.add((AttachmentEntry) entry);
        }
        else
        {
          //  progressListener.setStatus("The class of page is not supported! The class of page:" + entry.getClass());
        }
        num++;
      } else {
        LOGGER.log(Level.WARNING, "Error parsing entries!");
      }
    }
    
    int totalEntries = pages.size() + attachments.size();
    progressListener.setStatus("We have "+pages.size()+" with "+attachments.size()+" attachments.");
	return totalEntries;
}
  
  private void checkAndFixNameCollision(Set<String> pageTitles, BaseContentEntry<?> entry, ProgressListener progressListener) {
	  String orig=entry.getTitle().getPlainText();
	  //progressListener.setStatus(orig);
	  int i=1;
	while(!pageTitles.add(entry.getTitle().getPlainText().toLowerCase()))
	{
		String newTitle=orig+"("+i+")";
		progressListener.setStatus("duplicate title found trying "+newTitle);
		PlainTextConstruct tc= new PlainTextConstruct(newTitle);
		entry.setTitle(tc);
		i++;
	}
  }
}