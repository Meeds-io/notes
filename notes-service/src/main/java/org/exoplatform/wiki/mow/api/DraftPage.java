package org.exoplatform.wiki.mow.api;

import lombok.Data;
import org.exoplatform.commons.diff.DiffResult;

@Data
public class DraftPage extends Page {
  private String targetPageId;

  private String targetPageRevision;

  private boolean newPage;

  private DiffResult changes;

  @Override
  public boolean isDraftPage() {
    return true;
  }
}
