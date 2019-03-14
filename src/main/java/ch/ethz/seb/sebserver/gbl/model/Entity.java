/*
 * Copyright (c) 2018 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gbl.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

import ch.ethz.seb.sebserver.gbl.api.API;
import ch.ethz.seb.sebserver.gbl.api.EntityType;

/** Defines generic interface for all types of Entity. */
public interface Entity extends ModelIdAware {

    public static final String FILTER_ATTR_INSTITUTION = API.PARAM_INSTITUTION_ID;
    public static final String FILTER_ATTR_ACTIVE = "active";
    public static final String FILTER_ATTR_NAME = "name";

    /** Get the type of the entity.
     *
     * @return the type of the entity */
    @JsonIgnore
    EntityType entityType();

    /** Get the name of the entity
     *
     * @return the name of the entity */
    @JsonIgnore
    String getName();

    /** Get an unique EntityKey for the entity consisting of the model identifier of the entity
     * and the type of the entity.
     * 
     * @return unique EntityKey for the entity */
    @JsonIgnore
    default EntityKey getEntityKey() {
        final String modelId = getModelId();
        if (modelId == null) {
            return null;
        }
        return new EntityKey(modelId, entityType());
    }

    /** Creates an EntityName instance from a given Entity.
     *
     * @param entity The Entity instance
     * @return EntityName instance created form given Entity */
    public static EntityName toName(final Entity entity) {
        return new EntityName(
                entity.entityType(),
                entity.getModelId(),
                entity.getName());
    }

}
