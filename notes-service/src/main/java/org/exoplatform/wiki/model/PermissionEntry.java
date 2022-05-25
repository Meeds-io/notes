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

package org.exoplatform.wiki.model;

import java.util.Arrays;

import org.exoplatform.wiki.service.IDType;

public class PermissionEntry {
  private String       id;

  private String       fullName;

  private IDType       idType;

  private Permission[] permissions;

  public PermissionEntry() {
  }

  public PermissionEntry(String id, String fullName, IDType idType, Permission[] permissions) {
    this.id = id;
    this.fullName = fullName;
    this.idType = idType;
    this.permissions = permissions;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getFullName() {
    return fullName;
  }

  public void setFullName(String fullName) {
    this.fullName = fullName;
  }

  public IDType getIdType() {
    return idType;
  }

  public void setIdType(IDType idType) {
    this.idType = idType;
  }

  public Permission[] getPermissions() {
    return permissions;
  }

  public void setPermissions(Permission[] permissions) {
    this.permissions = permissions;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (!(o instanceof PermissionEntry))
      return false;

    PermissionEntry that = (PermissionEntry) o;

    if (fullName != null ? !fullName.equals(that.fullName) : that.fullName != null)
      return false;
    if (id != null ? !id.equals(that.id) : that.id != null)
      return false;
    if (idType != that.idType)
      return false;
      return Arrays.equals(permissions, that.permissions);
  }

  @Override
  public int hashCode() {
    int result = id != null ? id.hashCode() : 0;
    result = 31 * result + (fullName != null ? fullName.hashCode() : 0);
    result = 31 * result + (idType != null ? idType.hashCode() : 0);
    result = 31 * result + (permissions != null ? Arrays.hashCode(permissions) : 0);
    return result;
  }
}
