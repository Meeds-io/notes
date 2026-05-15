/**
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2020 - 2026 Meeds Association contact@meeds.io
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
package org.exoplatform.wiki.tree.utils;

import static org.junit.Assert.assertEquals;
import static org.springframework.test.util.AssertionErrors.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.exoplatform.wiki.tree.JsonNodeData;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import java.lang.reflect.Method;

@RunWith(MockitoJUnitRunner.class)
public class TreeUtilsTest {

  @Test
  public void testSortNodesNatural() throws Exception {
    List<JsonNodeData> nodes = new ArrayList<>();

    nodes.add(createNode("10", 0));
    nodes.add(createNode("1", 0));
    nodes.add(createNode("2", 0));
    nodes.add(createNode("9", 0));

    Method sortNodesMethod = TreeUtils.class.getDeclaredMethod("sortNodes", List.class, Locale.class);
    sortNodesMethod.setAccessible(true);
    sortNodesMethod.invoke(null, nodes, Locale.ENGLISH);

    assertEquals("1", nodes.get(0).getName());
    assertEquals("2", nodes.get(1).getName());
    assertEquals("9", nodes.get(2).getName());
    assertEquals("10", nodes.get(3).getName());
  }

  @Test
  public void testSortNodesManualPosition() throws Exception {
    List<JsonNodeData> nodes = new ArrayList<>();

    nodes.add(createNode("Note 1", 2));
    nodes.add(createNode("Note 2", 1));
    nodes.add(createNode("Note 10", 0));

    Method sortNodesMethod = TreeUtils.class.getDeclaredMethod("sortNodes", List.class, Locale.class);
    sortNodesMethod.setAccessible(true);
    sortNodesMethod.invoke(null, nodes, Locale.ENGLISH);

    assertEquals("Note 10", nodes.get(0).getName());
    assertEquals("Note 2", nodes.get(1).getName());
    assertEquals("Note 1", nodes.get(2).getName());
  }

  @Test
  public void testSortNodesMixed() throws Exception {
    List<JsonNodeData> nodes = new ArrayList<>();

    nodes.add(createNode("B", 1));
    nodes.add(createNode("A", 1));
    nodes.add(createNode("C", 0));

    Method sortNodesMethod = TreeUtils.class.getDeclaredMethod("sortNodes", List.class, Locale.class);
    sortNodesMethod.setAccessible(true);
    sortNodesMethod.invoke(null, nodes, Locale.ENGLISH);

    assertEquals("C", nodes.get(0).getName());
    assertEquals("A", nodes.get(1).getName());
    assertEquals("B", nodes.get(2).getName());
  }

  @Test
  public void testSortNodesAccented() throws Exception {
    List<JsonNodeData> nodes = new ArrayList<>();

    nodes.add(createNode("é", 0));
    nodes.add(createNode("a", 0));
    nodes.add(createNode("b", 0));
    nodes.add(createNode("z", 0));

    Method sortNodesMethod = TreeUtils.class.getDeclaredMethod("sortNodes", List.class, Locale.class);
    sortNodesMethod.setAccessible(true);
    sortNodesMethod.invoke(null, nodes, Locale.FRENCH);

    int indexE = -1;
    int indexZ = -1;
    for (int i = 0; i < nodes.size(); i++) {
      if (nodes.get(i).getName().equals("é"))
        indexE = i;
      if (nodes.get(i).getName().equals("z"))
        indexZ = i;
    }
    assertTrue("'é' should be before 'z'", indexE < indexZ);
  }

  private JsonNodeData createNode(String name, Integer position) {
    JsonNodeData node = new JsonNodeData();
    node.setName(name);
    node.setPosition(position);
    return node;
  }
}
