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

package com.google.sites.liberation;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.sites.liberation.renderers.PageRenderer;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

/**
 * This class can be used to export a single page in a Site as
 * a String of XHTML. 
 * 
 * @author bsimon@google.com (Benjamin Simon)
 */
public final class PageExporter {

  PageRenderer renderer;
	
  /**
   * Constructs a new PageExporter from the given PageRenderer.
   */
  public PageExporter(PageRenderer renderer) {
    this.renderer = checkNotNull(renderer);
  }
  
  /**
   * Exports this entry's page as XHTML to the given file name.
   */
  public void export(String fileName) throws IOException {
    XmlElement html = new XmlElement("html");
    XmlElement body = new XmlElement("body");
    XmlElement parentLinks = renderer.renderParentLinks();
    if (parentLinks != null) {
      body.addElement(parentLinks);
    }
    XmlElement title = renderer.renderTitle();
    if (title != null) {
      body.addElement(title);
    }
    XmlElement mainHtml = renderer.renderMainHtml();
    if (mainHtml != null) {
      body.addElement(mainHtml);
    }
    XmlElement specialContent = renderer.renderSpecialContent();
    if(specialContent != null) {
      body.addElement(specialContent);
    }
    XmlElement subpageLinks = renderer.renderSubpageLinks();
    if (subpageLinks != null) {
      body.addElement(subpageLinks);
    }
    XmlElement attachments = renderer.renderAttachments();
    if (attachments != null) {
      body.addElement(attachments);
    }
    XmlElement comments = renderer.renderComments();
    if (comments != null) {
      body.addElement(comments);
    }
    html.addElement(body);
    BufferedWriter out = new BufferedWriter(new FileWriter(fileName));
    html.appendTo(out);
    out.close();
  }
}