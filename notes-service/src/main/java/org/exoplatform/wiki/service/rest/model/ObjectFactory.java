/*
 * Copyright (C) 2003-2021 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 */
package org.exoplatform.wiki.service.rest.model;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.bind.annotation.XmlRegistry;
import javax.xml.namespace.QName;

@XmlRegistry
public class ObjectFactory {

  private final static QName _ObjectSummary_QNAME = new QName("http://www.xwiki.org", "objectSummary");

  /**
   * Create a new ObjectFactory that can be used to create new instances of
   * schema derived classes for package: org.exoplatform.wiki.service.rest.model
   */
  public ObjectFactory() {
  }

  /**
   * Create an instance of {@link Xwiki }
   */
  public Xwiki createXwiki() {
    return new Xwiki();
  }

  /**
   * Create an instance of {@link LinkCollection }
   */
  public LinkCollection createLinkCollection() {
    return new LinkCollection();
  }

  /**
   * Create an instance of {@link Link }
   */
  public Link createLink() {
    return new Link();
  }

  /**
   * Create an instance of {@link Syntaxes }
   */
  public Syntaxes createSyntaxes() {
    return new Syntaxes();
  }

  /**
   * Create an instance of {@link Wikis }
   */
  public Wikis createWikis() {
    return new Wikis();
  }

  /**
   * Create an instance of {@link Wiki }
   */
  public Wiki createWiki() {
    return new Wiki();
  }

  /**
   * Create an instance of {@link Spaces }
   */
  public Spaces createSpaces() {
    return new Spaces();
  }

  /**
   * Create an instance of {@link Space }
   */
  public Space createSpace() {
    return new Space();
  }

  /**
   * Create an instance of {@link Pages }
   */
  public Pages createPages() {
    return new Pages();
  }

  /**
   * Create an instance of {@link PageSummary }
   */
  public PageSummary createPageSummary() {
    return new PageSummary();
  }

  /**
   * Create an instance of {@link Page }
   */
  public Page createPage() {
    return new Page();
  }

  /**
   * Create an instance of {@link Translations }
   */
  public Translations createTranslations() {
    return new Translations();
  }

  /**
   * Create an instance of {@link History }
   */
  public History createHistory() {
    return new History();
  }

  /**
   * Create an instance of {@link HistorySummary }
   */
  public HistorySummary createHistorySummary() {
    return new HistorySummary();
  }

  /**
   * Create an instance of {@link Attachments }
   */
  public Attachments createAttachments() {
    return new Attachments();
  }

  /**
   * Create an instance of {@link Attachment }
   */
  public Attachment createAttachment() {
    return new Attachment();
  }

  /**
   * Create an instance of {@link Comments }
   */
  public Comments createComments() {
    return new Comments();
  }

  /**
   * Create an instance of {@link Comment }
   */
  public Comment createComment() {
    return new Comment();
  }

  /**
   * Create an instance of {@link Property }
   */
  public Property createProperty() {
    return new Property();
  }

  /**
   * Create an instance of {@link Attribute }
   */
  public Attribute createAttribute() {
    return new Attribute();
  }

  /**
   * Create an instance of {@link Class }
   */
  public Class createClass() {
    return new Class();
  }

  /**
   * Create an instance of {@link Classes }
   */
  public Classes createClasses() {
    return new Classes();
  }

  /**
   * Create an instance of {@link Objects }
   */
  public Objects createObjects() {
    return new Objects();
  }

  /**
   * Create an instance of {@link ObjectSummary }
   */
  public ObjectSummary createObjectSummary() {
    return new ObjectSummary();
  }

  /**
   * Create an instance of {@link Object }
   */
  public Object createObject() {
    return new Object();
  }

  /**
   * Create an instance of {@link Translation }
   */
  public Translation createTranslation() {
    return new Translation();
  }

  /**
   * Create an instance of {@link Properties }
   */
  public Properties createProperties() {
    return new Properties();
  }

  /**
   * Create an instance of {@link Tag }
   */
  public Tag createTag() {
    return new Tag();
  }

  /**
   * Create an instance of {@link Tags }
   */
  public Tags createTags() {
    return new Tags();
  }

  /**
   * Create an instance of {@link SearchResult }
   */
  public SearchResult createSearchResult() {
    return new SearchResult();
  }

  /**
   * Create an instance of {@link SearchResults }
   */
  public SearchResults createSearchResults() {
    return new SearchResults();
  }

  /**
   * Create an instance of {@link JAXBElement }{@code <}{@link ObjectSummary
   * }{@code >}}
   */
  @XmlElementDecl(namespace = "http://www.xwiki.org", name = "objectSummary")
  public JAXBElement<ObjectSummary> createObjectSummary(ObjectSummary value) {
    return new JAXBElement<ObjectSummary>(_ObjectSummary_QNAME, ObjectSummary.class, null, value);
  }

}
