/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.weblayer.api;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.model.Domain.LMS_SETUP;
import ch.ethz.seb.sebserver.gbl.model.EntityType;
import ch.ethz.seb.sebserver.gbl.model.Page;
import ch.ethz.seb.sebserver.gbl.model.Page.SortOrder;
import ch.ethz.seb.sebserver.gbl.model.exam.Exam;
import ch.ethz.seb.sebserver.gbl.model.exam.QuizData;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.gbl.util.Utils;
import ch.ethz.seb.sebserver.webservice.servicelayer.authorization.AuthorizationGrantService;
import ch.ethz.seb.sebserver.webservice.servicelayer.authorization.PrivilegeType;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.ExamDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.UserActivityLogDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.UserActivityLogDAO.ActivityType;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.LmsAPIService;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.LmsAPITemplate;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.LmsAPITemplate.OrderBy;

@WebServiceProfile
@RestController
@RequestMapping("/${sebserver.webservice.api.admin.endpoint}" + RestAPI.ENDPOINT_QUIZ_IMPORT)
public class QuizImportController {

    private final int defaultPageSize;
    private final int maxPageSize;

    private final LmsAPIService lmsAPIService;
    private final AuthorizationGrantService authorizationGrantService;
    private final UserActivityLogDAO userActivityLogDAO;
    private final ExamDAO examDAO;

    public QuizImportController(
            @Value("${sebserver.webservice.api.pagination.defaultPageSize:10}") final int defaultPageSize,
            @Value("${sebserver.webservice.api.pagination.maxPageSize:500}") final int maxPageSize,
            final LmsAPIService lmsAPIService,
            final AuthorizationGrantService authorizationGrantService,
            final UserActivityLogDAO userActivityLogDAO,
            final ExamDAO examDAO) {

        this.defaultPageSize = defaultPageSize;
        this.maxPageSize = maxPageSize;
        this.lmsAPIService = lmsAPIService;
        this.authorizationGrantService = authorizationGrantService;
        this.userActivityLogDAO = userActivityLogDAO;
        this.examDAO = examDAO;
    }

    @RequestMapping(path = "/search", method = RequestMethod.GET)
    public Page<QuizData> search(
            @RequestParam(name = LMS_SETUP.ATTR_ID, required = true) final Long lmsSetupId,
            @RequestParam(name = QuizData.FILTER_ATTR_NAME, required = false) final String nameLike,
            @RequestParam(name = QuizData.FILTER_ATTR_START_TIME, required = false) final String startTime,
            @RequestParam(name = Page.ATTR_PAGE_NUMBER, required = false) final Integer pageNumber,
            @RequestParam(name = Page.ATTR_PAGE_SIZE, required = false) final Integer pageSize,
            @RequestParam(name = Page.ATTR_SORT_BY, required = false) final String orderBy,
            @RequestParam(name = Page.ATTR_SORT_ORDER, required = false) final String sortOrder) {

        final LmsAPITemplate lmsAPITemplate = this.lmsAPIService
                .createLmsAPITemplate(lmsSetupId)
                .getOrThrow();

        this.authorizationGrantService.checkPrivilege(
                EntityType.EXAM,
                PrivilegeType.READ_ONLY,
                lmsAPITemplate.lmsSetup().institutionId);

        return lmsAPITemplate.getQuizzesPage(
                nameLike,
                Utils.dateTimeStringToTimestamp(startTime, null),
                Result.tryCatch(() -> OrderBy.valueOf(orderBy))
                        .getOrElse(OrderBy.NAME),
                Result.tryCatch(() -> SortOrder.valueOf(sortOrder))
                        .getOrElse(SortOrder.ASCENDING),
                (pageNumber != null)
                        ? pageNumber
                        : 1,
                (pageSize != null)
                        ? (pageSize <= this.maxPageSize)
                                ? pageSize
                                : this.maxPageSize
                        : this.defaultPageSize);
    }

    @RequestMapping(path = "/import", method = RequestMethod.POST)
    public Collection<Exam> importExam(
            @RequestParam(name = LMS_SETUP.ATTR_ID, required = true) final Long lmsSetupId,
            @RequestParam(name = QuizData.QUIZ_ATTR_ID, required = true) final String quizId) {

        this.authorizationGrantService.checkHasAnyPrivilege(
                EntityType.EXAM,
                PrivilegeType.WRITE);

        final LmsAPITemplate lmsAPITemplate = this.lmsAPIService
                .createLmsAPITemplate(lmsSetupId)
                .getOrThrow();

        final Set<String> ids = new HashSet<>(Arrays.asList(
                StringUtils.split(quizId, Constants.LIST_SEPARATOR_CHAR)));

        return lmsAPITemplate.getQuizzes(ids)
                .stream()
                .map(result -> result.flatMap(this.examDAO::importFromQuizData))
                .flatMap(Result::skipOnError)
                .peek(exam -> this.userActivityLogDAO.log(ActivityType.IMPORT, exam))
                .collect(Collectors.toList());
    }

}