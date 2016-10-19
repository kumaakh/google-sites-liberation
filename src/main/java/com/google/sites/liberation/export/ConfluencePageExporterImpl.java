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

import java.io.IOException;
import java.net.URL;
import java.util.Comparator;
import java.util.List;

import com.google.common.collect.Lists;
import com.google.gdata.data.sites.BaseContentEntry;
import com.google.gdata.data.sites.BasePageEntry;
import com.google.inject.Inject;
import com.google.sites.liberation.renderers.AncestorLinksRenderer;
import com.google.sites.liberation.renderers.AnnouncementsRenderer;
import com.google.sites.liberation.renderers.AttachmentsRenderer;
import com.google.sites.liberation.renderers.CommentsRenderer;
import com.google.sites.liberation.renderers.ContentRenderer;
import com.google.sites.liberation.renderers.FileCabinetRenderer;
import com.google.sites.liberation.renderers.ListRenderer;
import com.google.sites.liberation.renderers.SubpageLinksRenderer;
import com.google.sites.liberation.renderers.TitleRenderer;
import com.google.sites.liberation.util.EntryUtils;
import com.google.sites.liberation.util.XmlElement;

/**
 * Implements {@link PageExporter} to export a single page in a 
 * Site to a confluence friendly format
 * 
 * @author akhil.kumar@gmail.com
 */
final class ConfluencePageExporterImpl implements PageExporter {
  
  private static final Comparator<BaseContentEntry<?>> updatedComparator =
      EntryUtils.getReverseUpdatedComparator();
  private static final Comparator<BaseContentEntry<?>> titleComparator =
      EntryUtils.getTitleComparator();
  
  private AncestorLinksRenderer ancestorLinksRenderer;
  private AnnouncementsRenderer announcementsRenderer;
  private AttachmentsRenderer attachmentsRenderer;
  private CommentsRenderer commentsRenderer;
  private ContentRenderer contentRenderer;
  private FileCabinetRenderer fileCabinetRenderer;
  private ListRenderer listRenderer;
  private SubpageLinksRenderer subpageLinksRenderer;
  private TitleRenderer titleRenderer;
  
  @Inject
  ConfluencePageExporterImpl(
      AncestorLinksRenderer ancestorLinksRenderer,
      AnnouncementsRenderer announcementsRenderer,
      AttachmentsRenderer attachmentsRenderer,
      CommentsRenderer commentsRenderer,
      ContentRenderer contentRenderer,
      FileCabinetRenderer fileCabinetRenderer,
      ListRenderer listRenderer,
      SubpageLinksRenderer subpageLinksRenderer,
      TitleRenderer titleRenderer) {
    this.ancestorLinksRenderer = checkNotNull(ancestorLinksRenderer);
    this.announcementsRenderer = checkNotNull(announcementsRenderer);
    this.attachmentsRenderer = checkNotNull(attachmentsRenderer);
    this.commentsRenderer = checkNotNull(commentsRenderer);
    this.contentRenderer = checkNotNull(contentRenderer);
    this.fileCabinetRenderer = checkNotNull(fileCabinetRenderer);
    this.listRenderer = checkNotNull(listRenderer);
    this.subpageLinksRenderer = checkNotNull(subpageLinksRenderer);
    this.titleRenderer = checkNotNull(titleRenderer);
  }
  
  @Override
  public void exportPage(BaseContentEntry<?> entry, EntryStore entryStore,
      Appendable out, boolean revisionsExported, URL siteURL) throws IOException {
    checkNotNull(entry, "entry");
    checkNotNull(entryStore, "entryStore");
    checkNotNull(out, "out");
    XmlElement body = contentRenderer.renderContent(entry,entryStore, revisionsExported, siteURL);
    body.appendTo(out);
  }
   
}