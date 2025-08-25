/**
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2020 - 2025 Meeds Association contact@meeds.io
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package org.exoplatform.wiki.jpa.mock;

import java.util.HashMap;
import java.util.Map;

import org.exoplatform.commons.search.index.IndexingService;

public class MockIndexingService implements IndexingService {

  private Map<String, Integer> count = new HashMap<>();

  @Override
  public void init(String s) {
    increaseCount("init");
  }

  @Override
  public void index(String s, String s1) {
    increaseCount("index");
  }

  @Override
  public void reindex(String s, String s1) {
    increaseCount("reindex");
  }

  @Override
  public void unindex(String s, String s1) {
    increaseCount("unindex");
  }

  private synchronized void increaseCount(String name) {
    Integer v = count.get(name);
    if (v == null) {
      v = 0;
    } else {
      v ++;
    }
    count.put(name, v);
  }

  public int getCount(String name) {
    Integer v = count.get(name);
    if (v != null) {
      return v.intValue();
    } else {
      return 0;
    }
  }
}
