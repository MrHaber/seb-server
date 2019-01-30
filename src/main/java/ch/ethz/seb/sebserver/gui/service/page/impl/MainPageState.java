/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 * 
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.service.page.impl;

import javax.servlet.http.HttpSession;

import org.eclipse.rap.rwt.RWT;

import ch.ethz.seb.sebserver.gui.service.page.activity.ActivitySelection;
import ch.ethz.seb.sebserver.gui.service.page.activity.ActivitySelection.Activity;

public final class MainPageState {

    public ActivitySelection activitySelection = Activity.NONE.createSelection();

    private MainPageState() {
    }

    public static MainPageState get() {
        try {
            final HttpSession httpSession = RWT
                    .getUISession()
                    .getHttpSession();

            MainPageState mainPageState = (MainPageState) httpSession.getAttribute(SEBMainPage.ATTR_MAIN_PAGE_STATE);
            if (mainPageState == null) {
                mainPageState = new MainPageState();
                httpSession.setAttribute(SEBMainPage.ATTR_MAIN_PAGE_STATE, mainPageState);
            }

            return mainPageState;
        } catch (final Exception e) {
            SEBMainPage.log.error("Unexpected error while trying to get MainPageState from user-session");
        }

        return null;
    }

    public static void clear() {
        final MainPageState mainPageState = get();
        mainPageState.activitySelection = Activity.NONE.createSelection();
    }
}