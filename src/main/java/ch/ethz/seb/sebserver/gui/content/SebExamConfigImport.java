/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.content;

import java.io.InputStream;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import ch.ethz.seb.sebserver.gbl.api.API;
import ch.ethz.seb.sebserver.gbl.model.Domain;
import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.Configuration;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationNode;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.gui.content.action.ActionDefinition;
import ch.ethz.seb.sebserver.gui.form.Form;
import ch.ethz.seb.sebserver.gui.form.FormBuilder;
import ch.ethz.seb.sebserver.gui.form.FormHandle;
import ch.ethz.seb.sebserver.gui.service.ResourceService;
import ch.ethz.seb.sebserver.gui.service.i18n.LocTextKey;
import ch.ethz.seb.sebserver.gui.service.page.ModalInputDialogComposer;
import ch.ethz.seb.sebserver.gui.service.page.PageContext;
import ch.ethz.seb.sebserver.gui.service.page.PageService;
import ch.ethz.seb.sebserver.gui.service.page.event.ActionEvent;
import ch.ethz.seb.sebserver.gui.service.page.impl.ModalInputDialog;
import ch.ethz.seb.sebserver.gui.service.page.impl.PageAction;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.RestCall;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.seb.examconfig.ImportExamConfigOnExistingConfig;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.seb.examconfig.ImportNewExamConfig;
import ch.ethz.seb.sebserver.gui.widget.FileUploadSelection;

public final class SebExamConfigImport {

    static Function<PageAction, PageAction> importFunction(
            final PageService pageService,
            final boolean newConfig) {

        return action -> {

            final ModalInputDialog<FormHandle<ConfigurationNode>> dialog =
                    new ModalInputDialog<FormHandle<ConfigurationNode>>(
                            action.pageContext().getParent().getShell(),
                            pageService.getWidgetFactory())
                                    .setDialogWidth(600);

            final ImportFormContext importFormContext = new ImportFormContext(
                    pageService,
                    action.pageContext(),
                    newConfig);

            dialog.open(
                    SebExamConfigPropForm.FORM_IMPORT_TEXT_KEY,
                    (Predicate<FormHandle<ConfigurationNode>>) formHandle -> doImport(
                            pageService,
                            formHandle,
                            newConfig),
                    importFormContext::cancelUpload,
                    importFormContext);

            return action;
        };
    }

    private static final boolean doImport(
            final PageService pageService,
            final FormHandle<ConfigurationNode> formHandle,
            final boolean newConfig) {

        try {
            final Form form = formHandle.getForm();
            final EntityKey entityKey = formHandle.getContext().getEntityKey();
            final Control fieldControl = form.getFieldControl(API.IMPORT_FILE_ATTR_NAME);
            final PageContext context = formHandle.getContext();

            // Ad-hoc field validation
            formHandle.process(name -> true, field -> field.resetError());
            final String fieldValue = form.getFieldValue(Domain.CONFIGURATION_NODE.ATTR_NAME);
            if (StringUtils.isBlank(fieldValue)) {
                form.setFieldError(
                        Domain.CONFIGURATION_NODE.ATTR_NAME,
                        pageService
                                .getI18nSupport()
                                .getText(new LocTextKey("sebserver.form.validation.fieldError.notNull")));
                return false;
            } else if (fieldValue.length() < 3 || fieldValue.length() > 255) {
                form.setFieldError(
                        Domain.CONFIGURATION_NODE.ATTR_NAME,
                        pageService
                                .getI18nSupport()
                                .getText(new LocTextKey("sebserver.form.validation.fieldError.size",
                                        null,
                                        null,
                                        null,
                                        3,
                                        255)));
                return false;
            }

            if (fieldControl != null && fieldControl instanceof FileUploadSelection) {
                final FileUploadSelection fileUpload = (FileUploadSelection) fieldControl;
                final InputStream inputStream = fileUpload.getInputStream();
                if (inputStream != null) {
                    final RestCall<Configuration>.RestCallBuilder restCall = (newConfig)
                            ? pageService.getRestService()
                                    .getBuilder(ImportNewExamConfig.class)
                            : pageService.getRestService()
                                    .getBuilder(ImportExamConfigOnExistingConfig.class);

                    restCall
                            .withHeader(
                                    API.IMPORT_PASSWORD_ATTR_NAME,
                                    form.getFieldValue(API.IMPORT_PASSWORD_ATTR_NAME))
                            .withBody(inputStream);

                    if (newConfig) {
                        restCall
                                .withHeader(
                                        Domain.CONFIGURATION_NODE.ATTR_NAME,
                                        form.getFieldValue(Domain.CONFIGURATION_NODE.ATTR_NAME))
                                .withHeader(
                                        Domain.CONFIGURATION_NODE.ATTR_DESCRIPTION,
                                        form.getFieldValue(Domain.CONFIGURATION_NODE.ATTR_DESCRIPTION))
                                .withHeader(
                                        Domain.CONFIGURATION_NODE.ATTR_TEMPLATE_ID,
                                        form.getFieldValue(Domain.CONFIGURATION_NODE.ATTR_TEMPLATE_ID));
                    } else {
                        restCall.withURIVariable(API.PARAM_MODEL_ID, entityKey.modelId);
                    }

                    final Result<Configuration> configuration = restCall
                            .call();

                    if (!configuration.hasError()) {
                        context.publishInfo(SebExamConfigPropForm.FORM_IMPORT_CONFIRM_TEXT_KEY);
                        if (newConfig) {

                            final PageAction action = pageService.pageActionBuilder(context)
                                    .newAction(ActionDefinition.SEB_EXAM_CONFIG_IMPORT_TO_NEW_CONFIG)
                                    .create();

                            pageService.firePageEvent(
                                    new ActionEvent(action),
                                    action.pageContext());
                        }
                        return true;
                    }
                } else {
                    formHandle.getContext().publishPageMessage(
                            new LocTextKey("sebserver.error.unexpected"),
                            new LocTextKey("Please selecte a valid SEB Exam Configuration File"));
                }
            }

            return false;
        } catch (final Exception e) {
            formHandle.getContext().notifyError(e);
            return true;
        }
    }

    private static final class ImportFormContext implements ModalInputDialogComposer<FormHandle<ConfigurationNode>> {

        private final PageService pageService;
        private final PageContext pageContext;
        private final boolean newConfig;

        private Form form = null;

        protected ImportFormContext(
                final PageService pageService,
                final PageContext pageContext,
                final boolean newConfig) {

            this.pageService = pageService;
            this.pageContext = pageContext;
            this.newConfig = newConfig;
        }

        @Override
        public Supplier<FormHandle<ConfigurationNode>> compose(final Composite parent) {

            final ResourceService resourceService = this.pageService.getResourceService();

            final FormHandle<ConfigurationNode> formHandle = this.pageService.formBuilder(
                    this.pageContext.copyOf(parent), 4)
                    .readonly(false)
                    .addField(FormBuilder.fileUpload(
                            API.IMPORT_FILE_ATTR_NAME,
                            SebExamConfigPropForm.FORM_IMPORT_SELECT_TEXT_KEY,
                            null,
                            API.SEB_FILE_EXTENSION))

                    .addFieldIf(
                            () -> this.newConfig,
                            () -> FormBuilder.text(
                                    Domain.CONFIGURATION_NODE.ATTR_NAME,
                                    SebExamConfigPropForm.FORM_NAME_TEXT_KEY))
                    .addFieldIf(
                            () -> this.newConfig,
                            () -> FormBuilder.text(
                                    Domain.CONFIGURATION_NODE.ATTR_DESCRIPTION,
                                    SebExamConfigPropForm.FORM_DESCRIPTION_TEXT_KEY)
                                    .asArea())
                    .addFieldIf(
                            () -> this.newConfig,
                            () -> FormBuilder.singleSelection(
                                    Domain.CONFIGURATION_NODE.ATTR_TEMPLATE_ID,
                                    SebExamConfigPropForm.FORM_TEMPLATE_TEXT_KEY,
                                    null,
                                    resourceService::getExamConfigTemplateResources))

                    .addField(FormBuilder.text(
                            API.IMPORT_PASSWORD_ATTR_NAME,
                            SebExamConfigPropForm.FORM_IMPORT_PASSWORD_TEXT_KEY,
                            "").asPasswordField())
                    .build();

            this.form = formHandle.getForm();
            return () -> formHandle;
        }

        void cancelUpload() {
            if (this.form != null) {
                final Control fieldControl = this.form.getFieldControl(API.IMPORT_FILE_ATTR_NAME);
                if (fieldControl != null && fieldControl instanceof FileUploadSelection) {
                    ((FileUploadSelection) fieldControl).close();
                }
            }
        }
    }

}
