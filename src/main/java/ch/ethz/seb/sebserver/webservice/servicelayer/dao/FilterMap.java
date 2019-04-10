/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.dao;

import org.joda.time.DateTime;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import ch.ethz.seb.sebserver.gbl.api.POSTMapper;
import ch.ethz.seb.sebserver.gbl.model.Entity;
import ch.ethz.seb.sebserver.gbl.model.exam.Exam;
import ch.ethz.seb.sebserver.gbl.model.exam.Indicator;
import ch.ethz.seb.sebserver.gbl.model.exam.QuizData;
import ch.ethz.seb.sebserver.gbl.model.institution.LmsSetup;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.Orientation;
import ch.ethz.seb.sebserver.gbl.model.user.UserInfo;
import ch.ethz.seb.sebserver.gbl.util.Utils;

/** A Map containing various filter criteria from a certain API request.
 * This is used as a data object that can be used to collect API request parameter
 * data on one side and supply filter criteria based access to concrete Entity filtering
 * on the other side.
 *
 * All text based filter criteria are used as SQL wildcard's */
public class FilterMap extends POSTMapper {

    public FilterMap() {
        super(new LinkedMultiValueMap<>());
    }

    public FilterMap(final MultiValueMap<String, String> params) {
        super(params);
    }

    public Integer getActiveAsInt() {
        return getBooleanAsInteger(UserInfo.FILTER_ATTR_ACTIVE);
    }

    public Long getInstitutionId() {
        return getLong(UserInfo.FILTER_ATTR_INSTITUTION);
    }

    public String getName() {
        return getSQLWildcard(Entity.FILTER_ATTR_NAME);
    }

    public String getQuizName() {
        return getString(Entity.FILTER_ATTR_NAME);
    }

    public String getUserUsername() {
        return getSQLWildcard(UserInfo.FILTER_ATTR_USER_NAME);
    }

    public String getUserEmail() {
        return getSQLWildcard(UserInfo.FILTER_ATTR_EMAIL);
    }

    public String getUserLanguage() {
        return getString(UserInfo.FILTER_ATTR_LANGUAGE);
    }

    public String getUserRole() {
        return getString(UserInfo.FILTER_ATTR_ROLE);
    }

    public String getLmsSetupType() {
        return getString(LmsSetup.FILTER_ATTR_LMS_TYPE);
    }

    public DateTime getQuizFromTime() {
        return Utils.toDateTime(getString(QuizData.FILTER_ATTR_START_TIME));
    }

    public DateTime getExamFromTime() {
        return Utils.toDateTime(getString(QuizData.FILTER_ATTR_START_TIME));
    }

    public DateTime getSebClientConfigFromTime() {
        return getQuizFromTime();
    }

    public Long getLmsSetupId() {
        return getLong(LmsSetup.FILTER_ATTR_LMS_SETUP);
    }

    public String getExamType() {
        return getString(Exam.FILTER_ATTR_TYPE);
    }

    public Long getIndicatorExamId() {
        return getLong(Indicator.FILTER_ATTR_EXAM);
    }

    public String getIndicatorName() {
        return getSQLWildcard(Indicator.FILTER_ATTR_NAME);
    }

    public String getOrientationTemplate() {
        return getSQLWildcard(Orientation.FILTER_ATTR_TEMPLATE);
    }

    public String getOrientationView() {
        return getSQLWildcard(Orientation.FILTER_ATTR_VIEW);
    }

    public String getOrientationGroup() {
        return getSQLWildcard(Orientation.FILTER_ATTR_GROUP);
    }

    public String getSQLWildcard(final String name) {
        return toSQLWildcard(this.params.getFirst(name));
    }

    public static String toSQLWildcard(final String text) {
        return (text == null) ? null : "%" + text + "%";
    }

}
