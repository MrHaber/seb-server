/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.content;

import java.util.function.Function;

import org.eclipse.swt.widgets.Composite;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.model.Domain;
import ch.ethz.seb.sebserver.gbl.model.Entity;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationNode;
import ch.ethz.seb.sebserver.gbl.model.user.UserRole;
import ch.ethz.seb.sebserver.gbl.profile.GuiProfile;
import ch.ethz.seb.sebserver.gui.content.action.ActionDefinition;
import ch.ethz.seb.sebserver.gui.service.ResourceService;
import ch.ethz.seb.sebserver.gui.service.i18n.LocTextKey;
import ch.ethz.seb.sebserver.gui.service.page.PageContext;
import ch.ethz.seb.sebserver.gui.service.page.PageService;
import ch.ethz.seb.sebserver.gui.service.page.PageService.PageActionBuilder;
import ch.ethz.seb.sebserver.gui.service.page.TemplateComposer;
import ch.ethz.seb.sebserver.gui.service.page.impl.PageAction;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.RestService;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.seb.examconfig.GetExamConfigNodes;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.auth.CurrentUser;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.auth.CurrentUser.GrantCheck;
import ch.ethz.seb.sebserver.gui.table.ColumnDefinition;
import ch.ethz.seb.sebserver.gui.table.ColumnDefinition.TableFilterAttribute;
import ch.ethz.seb.sebserver.gui.table.EntityTable;
import ch.ethz.seb.sebserver.gui.table.TableFilter.CriteriaType;

@Lazy
@Component
@GuiProfile
public class SebExamConfigList implements TemplateComposer {

    private static final LocTextKey EMPTY_LIST_TEXT_KEY =
            new LocTextKey("sebserver.examconfig.list.empty");
    private static final LocTextKey TITLE_TEXT_KEY =
            new LocTextKey("sebserver.examconfig.list.title");
    private static final LocTextKey INSTITUTION_TEXT_KEY =
            new LocTextKey("sebserver.examconfig.list.column.institution");
    private static final LocTextKey NAME_TEXT_KEY =
            new LocTextKey("sebserver.examconfig.list.column.name");
    private static final LocTextKey DESCRIPTION_TEXT_KEY =
            new LocTextKey("sebserver.examconfig.list.column.description");
    private static final LocTextKey ACTIVE_TEXT_KEY =
            new LocTextKey("sebserver.examconfig.list.column.active");
    private static final LocTextKey EMPTY_SELECTION_TEXT_KEY =
            new LocTextKey("sebserver.examconfig.info.pleaseSelect");

    private final TableFilterAttribute institutionFilter;
    private final TableFilterAttribute nameFilter =
            new TableFilterAttribute(CriteriaType.TEXT, Entity.FILTER_ATTR_NAME);

    private final PageService pageService;
    private final RestService restService;
    private final CurrentUser currentUser;
    private final ResourceService resourceService;
    private final int pageSize;

    protected SebExamConfigList(
            final PageService pageService,
            final RestService restService,
            final CurrentUser currentUser,
            @Value("${sebserver.gui.list.page.size}") final Integer pageSize) {

        this.pageService = pageService;
        this.restService = restService;
        this.currentUser = currentUser;
        this.resourceService = pageService.getResourceService();
        this.pageSize = pageSize;

        this.institutionFilter = new TableFilterAttribute(
                CriteriaType.SINGLE_SELECTION,
                Entity.FILTER_ATTR_INSTITUTION,
                this.resourceService::institutionResource);
    }

    @Override
    public void compose(final PageContext pageContext) {

        final Composite content = this.pageService.getWidgetFactory().defaultPageLayout(
                pageContext.getParent(),
                TITLE_TEXT_KEY);

        final boolean isSEBAdmin = this.currentUser.get().hasRole(UserRole.SEB_SERVER_ADMIN);
        final PageActionBuilder pageActionBuilder =
                this.pageService.pageActionBuilder(pageContext.clearEntityKeys());

        // table
        final EntityTable<ConfigurationNode> table =
                this.pageService.entityTableBuilder(this.restService.getRestCall(GetExamConfigNodes.class))
                        .withEmptyMessage(EMPTY_LIST_TEXT_KEY)
                        .withPaging(this.pageSize)
                        .withColumnIf(
                                () -> isSEBAdmin,
                                () -> new ColumnDefinition<>(
                                        Domain.LMS_SETUP.ATTR_INSTITUTION_ID,
                                        INSTITUTION_TEXT_KEY,
                                        examConfigInstitutionNameFunction(this.resourceService),
                                        this.institutionFilter,
                                        false))
                        .withColumn(new ColumnDefinition<>(
                                Domain.CONFIGURATION_NODE.ATTR_NAME,
                                NAME_TEXT_KEY,
                                entity -> entity.name,
                                this.nameFilter,
                                true))
                        .withColumn(new ColumnDefinition<>(
                                Domain.CONFIGURATION_NODE.ATTR_DESCRIPTION,
                                DESCRIPTION_TEXT_KEY,
                                entity -> entity.description,
                                this.nameFilter,
                                true))
                        .withColumn(new ColumnDefinition<>(
                                Domain.CONFIGURATION_NODE.ATTR_ACTIVE,
                                ACTIVE_TEXT_KEY,
                                entity -> entity.active,
                                true))
                        .withDefaultAction(pageActionBuilder
                                .newAction(ActionDefinition.SEB_EXAM_CONFIG_VIEW_FROM_LIST)
                                .create())
                        .compose(content);

        final GrantCheck examConfigGrant = this.currentUser.grantCheck(EntityType.CONFIGURATION_NODE);

        pageActionBuilder

                .newAction(ActionDefinition.SEB_EXAM_CONFIG_NEW)
                .publishIf(examConfigGrant::iw)

                .newAction(ActionDefinition.SEB_EXAM_CONFIG_VIEW_FROM_LIST)
                .withSelect(table::getSelection, PageAction::applySingleSelection, EMPTY_SELECTION_TEXT_KEY)
                .publishIf(() -> table.hasAnyContent())

                .newAction(ActionDefinition.SEB_EXAM_CONFIG_MODIFY_FROM_LIST)
                .withSelect(table::getSelection, PageAction::applySingleSelection, EMPTY_SELECTION_TEXT_KEY)
                .publishIf(() -> examConfigGrant.im() && table.hasAnyContent())

                .newAction(ActionDefinition.SEB_EXAM_CONFIG_MODIFY_PROPERTIES_FROM_LIST)
                .withSelect(table::getSelection, PageAction::applySingleSelection, EMPTY_SELECTION_TEXT_KEY)
                .publishIf(() -> examConfigGrant.im() && table.hasAnyContent());
    }

    private static Function<ConfigurationNode, String> examConfigInstitutionNameFunction(
            final ResourceService resourceService) {

        return config -> resourceService.getInstitutionNameFunction()
                .apply(String.valueOf(config.institutionId));
    }

}