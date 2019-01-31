/*
 * Copyright (c) 2018 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.service.page.event;

import ch.ethz.seb.sebserver.gui.service.page.PageEventListener;

public interface ActivitySelectionListener extends PageEventListener<ActivitySelectionEvent> {

    @Override
    default boolean match(final Class<? extends PageEvent> eventType) {
        return eventType == ActivitySelectionEvent.class;
    }

}