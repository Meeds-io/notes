/*
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2020 - 2022 Meeds Association contact@meeds.io
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.exoplatform.wiki.notification.Utils;

import org.exoplatform.commons.api.notification.model.ArgumentLiteral;
import org.exoplatform.wiki.model.Page;

import java.util.Set;

public class NotificationsUtils {
  public final static ArgumentLiteral<Page> PAGE = new ArgumentLiteral<Page>(Page.class, "page");

  public final static ArgumentLiteral<String> CONTENT_CHANGE = new ArgumentLiteral<String>(String.class, "content_change");

  public final static String EDIT_WIKI_NOTIFICATION_ID = "EditWikiNotificationPlugin";

  public static final ArgumentLiteral<Set> WATCHERS = new ArgumentLiteral<Set>(Set.class, "watchers");

  public final static ArgumentLiteral<String> WIKI_EDITOR    = new ArgumentLiteral<String>(String.class, "wiki_editor");

  public static final ArgumentLiteral<String> WIKI_PAGE_NAME = new ArgumentLiteral<String>(String.class, "wiki_page_name");

  public static final ArgumentLiteral<String> WIKI_URL = new ArgumentLiteral<String>(String.class, "wiki_url");

}
