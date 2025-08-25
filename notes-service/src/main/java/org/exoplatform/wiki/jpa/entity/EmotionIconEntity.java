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
package org.exoplatform.wiki.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

@Entity(name = "WikiEmotionIconEntity")
@Table(name = "WIKI_EMOTION_ICONS")
@NamedQueries({
        @NamedQuery(name = "emotionIcon.getEmotionIconByName", query = "SELECT e FROM WikiEmotionIconEntity e WHERE e.name = :name")
})
public class EmotionIconEntity {
  @Id
  @SequenceGenerator(name="SEQ_WIKI_EMOTION_ICONS_ICON_ID", sequenceName="SEQ_WIKI_EMOTION_ICONS_ICON_ID", allocationSize = 1)
  @GeneratedValue(strategy=GenerationType.AUTO, generator="SEQ_WIKI_EMOTION_ICONS_ICON_ID")
  @Column(name = "EMOTION_ICON_ID")
  private long id;

  @Column(name = "NAME")
  private String name;

  @Column(name = "IMAGE", length = 20971520)
  private byte[] image;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public byte[] getImage() {
    return image;
  }

  public void setImage(byte[] image) {
    this.image = image;
  }
}
