package org.exoplatform.wiki.service.search;

import java.util.Calendar;
import java.util.List;
import java.util.Map;

import lombok.Data;
import org.exoplatform.commons.utils.HTMLSanitizer;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.metadata.model.MetadataItem;

@Data
public class SearchResult {
  private static Log log = ExoLogger.getLogger(SearchResult.class);

  protected String wikiType;

  protected String wikiOwner;

  protected Identity wikiOwnerIdentity;

  protected Identity poster;

  protected String pageName;

  protected String attachmentName;

  protected String excerpt;

  protected String title;

  protected SearchResultType type;

  protected String url;

  protected long score;

  protected Calendar updatedDate;

  protected Calendar createdDate;

  protected Map<String, List<MetadataItem>> metadata;

  protected String lang;

  public SearchResult() {
  }

  public SearchResult(String wikiType,
                      String wikiOwner,
                      String pageName,
                      String attachmentName,
                      String excerpt,
                      String title,
                      SearchResultType type,
                      Calendar updatedDate,
                      Calendar createdDate) {
    this.wikiType = wikiType;
    this.wikiOwner = wikiOwner;
    this.pageName = pageName;
    this.attachmentName = attachmentName;
    this.excerpt = excerpt;
    this.title = title;
    this.type = type;
    this.updatedDate = updatedDate;
    this.createdDate = createdDate;
  }

  public SearchResult(String wikiType,
                      Identity poster,
                      Identity wikiOwnerIdentity,
                      String pageName,
                      String attachmentName,
                      String excerpt,
                      String title,
                      SearchResultType type,
                      Calendar updatedDate,
                      Calendar createdDate) {
    this.wikiType = wikiType;
    this.poster = poster;
    this.wikiOwnerIdentity = wikiOwnerIdentity;
    this.pageName = pageName;
    this.attachmentName = attachmentName;
    this.excerpt = excerpt;
    this.title = title;
    this.type = type;
    this.updatedDate = updatedDate;
    this.createdDate = createdDate;
  }

  public String getExcerpt() {
    try {
      return HTMLSanitizer.sanitize(excerpt);
    } catch (Exception e) {

      log.error("Fail to sanitize input [" + excerpt + "], " + e.getMessage(), e);

    }
    return "";
  }
}
