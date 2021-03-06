/*
 * Copyright (C) 2003-2010 eXo Platform SAS.
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
package org.exoplatform.wiki.resolver;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.StringTokenizer;

import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.wiki.utils.WikiNameValidator;

public class TitleResolver {
  
  private static final Log      log               = ExoLogger.getLogger(TitleResolver.class);
  private static final String[] INVALID_CHARACTERS = WikiNameValidator.INVALID_CHARACTERS.split(" ");
  
  public static String getId(String title, boolean isEncoded) {
    if (title == null) {
      return null;
    }
    String id = title;
    if (isEncoded) {
      try {
        id = URLDecoder.decode(title, "UTF-8");
      } catch (UnsupportedEncodingException e1) {
        if (log.isWarnEnabled()) 
          log.warn(String.format("Getting Page Id from %s failed because of UnspportedEncodingException. Using page title(%s) instead (Not recommended. Fix it if possible!!!)", title), e1);
      }
    }
    for (String specialChar : INVALID_CHARACTERS) {
      id = replaceSpecialCharacter(specialChar, id);
    }
    return replaceSpecialCharacter(" ", id);
  }

  private static String replaceSpecialCharacter(String specialChar, String id) {
    StringTokenizer st = new StringTokenizer(id, specialChar, false);
    StringBuilder sb = new StringBuilder();
    if (st.hasMoreElements()) {
      sb.append(st.nextElement());
    }
    while (st.hasMoreElements())
      sb.append("_").append(st.nextElement());
    return sb.toString();
  }

}
