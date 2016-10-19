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
import static com.google.sites.liberation.util.EntryType.isPage;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.google.gdata.data.sites.BaseContentEntry;
import com.google.gdata.data.sites.BasePageEntry;
import com.google.sites.liberation.util.EntryUtils;

/**
 * An in-memory implementation of {@link EntryStore}.
 * 
 * @author bsimon@google.com (Benjamin Simon)
 */
final class InMemoryEntryStore implements EntryStore {

  private static final Logger LOGGER = Logger.getLogger(
      InMemoryEntryStore.class.getCanonicalName());
  
  private final Map<String, BaseContentEntry<?>> entries;
  private final Set<BasePageEntry<?>> topLevelEntries;
  private final Multimap<String, BaseContentEntry<?>> children;
  private Map<String,NewPage> createdPages;
  private Map<String,String> uploadedAttachments;
	//e.g.  https://sites.google.com/a/mamigoinc.com/development/Home/Company -> <4784403,1>
  
  /**
   * Creates a new InMemoryEntryStore which provides constant time storage 
   * and retrieval of entries by id or parent id.
   */
  public InMemoryEntryStore() {
    entries = Maps.newHashMap();
    topLevelEntries = Sets.newHashSet();
    children = HashMultimap.create();
    createdPages = Maps.newHashMap();
    uploadedAttachments = Maps.newHashMap();
  }

  @Override
  public NewPage addCreatedPage(BasePageEntry<?> entry, String newPageId, int version)
  {
	  return createdPages.put(entry.getHtmlLink().getHref(),new NewPage(newPageId, entry.getTitle().getPlainText(),version));
  }
  @Override
  public NewPage getCreatedPage(BasePageEntry<?> entry)
  {
	  return createdPages.get(entry.getHtmlLink().getHref());
  }
  @Override
  public NewPage getCreatedPage(String link)
  {
	  return createdPages.get(link);
  }
  @Override
  public void addUploadedAttachment(String oldLink, String newLink)
  {
	  uploadedAttachments.put(oldLink, newLink);
  }
  @Override
  public String getNewAttachmentLink(String oldLink)
  {
	  return uploadedAttachments.get(oldLink);
  }

  @Override
  public void addEntry(BaseContentEntry<?> entry) {
    checkNotNull(entry);
    String id = entry.getId();
    if (id != null && entries.get(id) == null) {
      entries.put(id, entry);
      String parentId = EntryUtils.getParentId(entry);
      if (parentId == null) {
        if (isPage(entry)) {
          topLevelEntries.add((BasePageEntry<?>) entry);
        } else {
          LOGGER.log(Level.WARNING, "All non-page entries must have a parent!");
        }
      } else {
        children.put(parentId, entry);
      }
    } else {
      LOGGER.log(Level.WARNING, "All entries should have a unique non-null id!");
    }
  }
  
  @Override
  public Collection<BaseContentEntry<?>> getChildren(String id) {
    checkNotNull(id);
    return children.get(id);
  }

  @Override
  public BaseContentEntry<?> getEntry(String id) {
    checkNotNull(id);
    return entries.get(id);
  }
  
  @Override
  public BasePageEntry<?> getParent(String id) {
    checkNotNull(id);
    BaseContentEntry<?> child = getEntry(id);
    String parentId = EntryUtils.getParentId(child);
    if (parentId == null) {
      return null;
    }
    return (BasePageEntry<?>) getEntry(parentId);
  }
  
  @Override
  public Collection<BasePageEntry<?>> getTopLevelEntries() {
    return topLevelEntries;
  }

	public void save() {
		save(""+(new Date()).getTime());
	}
	public void save(String ts) {
		try {
			String fName = "progress_"+ts+".ser";
			FileOutputStream fileOut = new FileOutputStream(fName);
			ObjectOutputStream out = new ObjectOutputStream(fileOut);
			out.writeObject(createdPages);
			out.writeObject(uploadedAttachments);
			out.close();
			fileOut.close();
			System.out.printf("Serialized data is saved in " + fName);
		} catch (IOException i) {
			i.printStackTrace();
		}
	}
  public void load(String ts){
	  try {
			String fName = "progress_"+ts+".ser";
			FileInputStream fileIn = new FileInputStream(fName);
			ObjectInputStream in = new ObjectInputStream(fileIn);
			createdPages=(Map<String, NewPage>) in.readObject();
			uploadedAttachments=(Map<String, String>) in.readObject();
			in.close();
			in.close();
			System.out.printf("Serialized data loaded from " + fName);
		} catch (Exception i) {
			i.printStackTrace();
		}
  }
  
}
