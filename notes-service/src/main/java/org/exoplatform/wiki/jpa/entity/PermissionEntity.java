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

package org.exoplatform.wiki.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

import org.exoplatform.commons.api.persistence.ExoEntity;
import org.exoplatform.wiki.model.PermissionType;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          exo@exoplatform.com
 * Jun 26, 2015  
 */
@Embeddable
@ExoEntity
public class PermissionEntity {
  /**
   * User or Group
   */
  @Column(name = "WIKI_IDENTITY")
  private String identity;

  @Column(name = "IDENTITY_TYPE")
  private String identityType;

  @Column(name="PERMISSION")
  @Enumerated(EnumType.STRING)
  private PermissionType permissionType;


  public PermissionEntity() {
    //Default constructor
  }

  public PermissionEntity(String identity, String identityType, PermissionType permissionType) {
    this.identity = identity;
    this.identityType = identityType;
    this.permissionType = permissionType;
  }

  public String getIdentity() {
    return identity;
  }

  public void setIdentity(String user) {
    this.identity = user;
  }

  public String getIdentityType() {
    return identityType;
  }

  public void setIdentityType(String identityType) {
    this.identityType = identityType;
  }

  public PermissionType getPermissionType() {
    return permissionType;
  }

  public void setPermissionType(PermissionType permissionType) {
    this.permissionType = permissionType;
  }
}
