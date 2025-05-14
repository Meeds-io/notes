/**
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2020 - 2024 Meeds Association contact@meeds.io
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
package io.meeds.notes.plugin;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import org.exoplatform.commons.exception.ObjectNotFoundException;
import org.exoplatform.portal.config.UserACL;
import org.exoplatform.services.security.Identity;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceService;
import org.exoplatform.wiki.model.Page;
import org.exoplatform.wiki.model.WikiType;
import org.exoplatform.wiki.service.NoteService;

import io.meeds.portal.permlink.model.PermanentLinkObject;
import io.meeds.portal.permlink.plugin.PermanentLinkPlugin;
import io.meeds.portal.permlink.service.PermanentLinkService;

import jakarta.annotation.PostConstruct;
import lombok.SneakyThrows;

@Component
public class NotePermanentLinkPlugin implements PermanentLinkPlugin {

  public static final String   OBJECT_TYPE = "notes";

  public static final String   URL_FORMAT  = "/portal/s/%s/notes/%s";

  @Autowired
  private NoteService          noteService;

  @Autowired
  private UserACL              userAcl;

  @Autowired
  private SpaceService         spaceService;

  @Autowired
  private PermanentLinkService permanentLinkService;

  @PostConstruct
  public void init() {
    permanentLinkService.addPlugin(this);
  }

  @Override
  public String getObjectType() {
    return OBJECT_TYPE;
  }

  @Override
  @SneakyThrows
  public boolean canAccess(PermanentLinkObject object, Identity identity) throws ObjectNotFoundException {
    return userAcl.hasAccessPermission(OBJECT_TYPE,
                                       object.getObjectId(),
                                       identity);
  }

  @Override
  @SneakyThrows
  public String getDirectAccessUrl(PermanentLinkObject object) throws ObjectNotFoundException {
    Page note = noteService.getNoteById(object.getObjectId());
    if (note == null) {
      throw new ObjectNotFoundException(String.format("Note with id %s not found", object.getObjectId()));
    } else if (!StringUtils.equalsIgnoreCase(WikiType.GROUP.name(), note.getWikiType())) {
      throw new ObjectNotFoundException(String.format("Note with id %s isn't of type 'group'", object.getObjectId()));
    }
    Space space = spaceService.getSpaceByGroupId(note.getWikiOwner());
    if (space == null) {
      throw new ObjectNotFoundException(String.format("Note with id %s associated space wasn't found", object.getObjectId()));
    }
    return String.format(URL_FORMAT, space.getId(), object.getObjectId());
  }

}
