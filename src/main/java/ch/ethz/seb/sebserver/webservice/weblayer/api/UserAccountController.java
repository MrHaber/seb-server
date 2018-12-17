/*
 * Copyright (c) 2018 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.weblayer.api;

import java.security.Principal;
import java.util.Collection;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import ch.ethz.seb.sebserver.gbl.model.EntityType;
import ch.ethz.seb.sebserver.gbl.model.user.UserFilter;
import ch.ethz.seb.sebserver.gbl.model.user.UserInfo;
import ch.ethz.seb.sebserver.gbl.model.user.UserMod;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.webservice.servicelayer.authorization.AuthorizationGrantService;
import ch.ethz.seb.sebserver.webservice.servicelayer.authorization.PrivilegeType;
import ch.ethz.seb.sebserver.webservice.servicelayer.authorization.SEBServerUser;
import ch.ethz.seb.sebserver.webservice.servicelayer.authorization.UserService;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.UserActivityLogDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.UserActivityLogDAO.ActivityType;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.UserDAO;

@WebServiceProfile
@RestController
@RequestMapping("/${sebserver.webservice.api.admin.endpoint}" + RestAPI.ENDPOINT_USER_ACCOUNT)
public class UserAccountController {

    private final UserDAO userDao;
    private final AuthorizationGrantService authorizationGrantService;
    private final UserService userService;
    private final UserActivityLogDAO userActivityLogDAO;

    public UserAccountController(
            final UserDAO userDao,
            final AuthorizationGrantService authorizationGrantService,
            final UserService userService,
            final UserActivityLogDAO userActivityLogDAO) {

        this.userDao = userDao;
        this.authorizationGrantService = authorizationGrantService;
        this.userService = userService;
        this.userActivityLogDAO = userActivityLogDAO;
    }

    @RequestMapping(method = RequestMethod.GET)
    public Collection<UserInfo> getAll(
            //@RequestParam(required = false) final UserFilter filter,
            @RequestBody(required = false) final UserFilter userFilter,
            final Principal principal) {

        // fist check if current user has any privileges for this action
        this.authorizationGrantService.checkHasAnyPrivilege(
                EntityType.USER,
                PrivilegeType.READ_ONLY);

        if (this.authorizationGrantService.hasBasePrivilege(
                EntityType.USER,
                PrivilegeType.READ_ONLY,
                principal)) {

            return (userFilter != null)
                    ? this.userDao.all(userFilter).getOrThrow()
                    : this.userDao.allActive().getOrThrow();

        } else {

            final Predicate<UserInfo> grantFilter = this.authorizationGrantService.getGrantFilter(
                    EntityType.USER,
                    PrivilegeType.READ_ONLY,
                    principal);

            if (userFilter == null) {

                return this.userDao
                        .all(userInfo -> userInfo.active && grantFilter.test(userInfo))
                        .getOrThrow();

            } else {

                return this.userDao
                        .all(userFilter)
                        .getOrThrow()
                        .stream()
                        .filter(grantFilter)
                        .collect(Collectors.toList());
            }
        }
    }

    @RequestMapping(value = "/me", method = RequestMethod.GET)
    public UserInfo loggedInUser(final Authentication auth) {
        return this.userService
                .getCurrentUser()
                .getUserInfo();
    }

    @RequestMapping(value = "/{userUUID}", method = RequestMethod.GET)
    public UserInfo accountInfo(@PathVariable final String userUUID, final Principal principal) {
        return this.userDao
                .byUuid(userUUID)
                .flatMap(userInfo -> this.authorizationGrantService.checkGrantOnEntity(
                        userInfo,
                        PrivilegeType.READ_ONLY))
                .getOrThrow();

    }

    @RequestMapping(value = "/create", method = RequestMethod.PUT)
    public UserInfo createUser(
            @RequestBody final UserMod userData,
            final Principal principal) {

        return _saveUser(userData, principal, PrivilegeType.WRITE);
    }

    @RequestMapping(value = "/save", method = RequestMethod.POST)
    public UserInfo saveUser(
            @RequestBody final UserMod userData,
            final Principal principal) {

        return _saveUser(userData, principal, PrivilegeType.MODIFY);
    }

    private UserInfo _saveUser(
            final UserMod userData,
            final Principal principal,
            final PrivilegeType grantType) {

        // fist check if current user has any privileges for this action
        this.authorizationGrantService.checkHasAnyPrivilege(
                EntityType.USER,
                grantType);

        final SEBServerUser admin = this.userService.extractFromPrincipal(principal);
        final ActivityType actionType = (userData.getUserInfo().uuid == null)
                ? ActivityType.CREATE
                : ActivityType.MODIFY;

        return this.userDao
                .save(admin, userData)
                .flatMap(userInfo -> this.userActivityLogDAO.logUserActivity(
                        admin,
                        actionType,
                        userInfo))
                .getOrThrow();
    }

}