/*
 * Copyright (c) 2018 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.service.examconfig;

import org.eclipse.rap.rwt.RWT;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationAttribute;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.Orientation;
import ch.ethz.seb.sebserver.gui.service.examconfig.impl.ViewContext;
import ch.ethz.seb.sebserver.gui.widget.WidgetFactory.CustomVariant;

public interface InputFieldBuilder {

    String RES_BUNDLE_KEY_PREFIX = "sebserver.examconfig.attribute.";

    boolean builderFor(
            ConfigurationAttribute attribute,
            Orientation orientation);

    InputField createInputField(
            final Composite parent,
            final ConfigurationAttribute attribute,
            final ViewContext viewContext);

    static Composite createInnerGrid(
            final Composite parent,
            final Orientation orientation) {

        return createInnerGrid(parent, orientation, 1);
    }

    static Composite createInnerGrid(
            final Composite parent,
            final Orientation orientation,
            final int numColumns) {

        final Composite comp = new Composite(parent, SWT.NONE);
        final GridLayout gridLayout = new GridLayout(numColumns, true);
        gridLayout.verticalSpacing = 0;
        gridLayout.marginHeight = 1;
        comp.setLayout(gridLayout);

        final GridData gridData = new GridData(
                SWT.FILL, SWT.FILL,
                true, false,
                (orientation != null) ? orientation.width() : 1,
                (orientation != null) ? orientation.height() : 1);
        comp.setLayoutData(gridData);
        return comp;
    }

    static Label createErrorLabel(final Composite innerGrid) {
        final Label errorLabel = new Label(innerGrid, SWT.NONE);
        errorLabel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
        errorLabel.setVisible(false);
        errorLabel.setData(RWT.CUSTOM_VARIANT, CustomVariant.ERROR.key);
        return errorLabel;
    }

}