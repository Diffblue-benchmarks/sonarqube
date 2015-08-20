/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package org.sonar.server.permission;

import org.sonar.api.security.DefaultGroups;
import org.sonar.db.DbClient;
import org.sonar.db.permission.PermissionTemplateDao;
import org.sonar.db.permission.PermissionTemplateDto;
import org.sonar.db.user.GroupDto;
import org.sonar.db.user.UserDao;
import org.sonar.db.user.UserDto;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.user.UserSession;

import static org.sonar.server.permission.PermissionPrivilegeChecker.checkGlobalAdminUser;
import static org.sonar.server.permission.PermissionValueValidator.validateProjectPermission;

/**
 * @deprecated since 5.2 can be removed when Ruby doesn't rely on PermissionTemplateService
 */
@Deprecated
abstract class PermissionTemplateUpdater {

  private final String templateKey;
  private final String permission;
  private final String updatedReference;
  private final PermissionTemplateDao permissionTemplateDao;
  private final UserDao userDao;
  private final UserSession userSession;

  PermissionTemplateUpdater(DbClient dbClient, UserSession userSession, String templateKey, String permission, String updatedReference) {
    this.userSession = userSession;
    this.permissionTemplateDao = dbClient.permissionTemplateDao();
    this.userDao = dbClient.userDao();
    this.templateKey = templateKey;
    this.permission = permission;
    this.updatedReference = updatedReference;
  }

  void executeUpdate() {
    checkGlobalAdminUser(userSession);
    Long templateId = getTemplateId(templateKey);
    validateProjectPermission(permission);
    doExecute(templateId, permission);
  }

  abstract void doExecute(Long templateId, String permission);

  Long getUserId() {
    UserDto userDto = userDao.selectActiveUserByLogin(updatedReference);
    if (userDto == null) {
      throw new BadRequestException("Unknown user: " + updatedReference);
    }
    return userDto.getId();
  }

  Long getGroupId() {
    if (DefaultGroups.isAnyone(updatedReference)) {
      return null;
    }
    GroupDto groupDto = userDao.selectGroupByName(updatedReference);
    if (groupDto == null) {
      throw new BadRequestException("Unknown group: " + updatedReference);
    }
    return groupDto.getId();
  }

  private Long getTemplateId(String key) {
    PermissionTemplateDto permissionTemplateDto = permissionTemplateDao.selectTemplateByKey(key);
    if (permissionTemplateDto == null) {
      throw new BadRequestException("Unknown template: " + key);
    }
    return permissionTemplateDto.getId();
  }
}
