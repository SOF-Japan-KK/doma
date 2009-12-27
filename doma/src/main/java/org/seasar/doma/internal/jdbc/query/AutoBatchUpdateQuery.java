/*
 * Copyright 2004-2009 the Seasar Foundation and the Others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package org.seasar.doma.internal.jdbc.query;

import static org.seasar.doma.internal.util.AssertionUtil.*;

import java.util.Iterator;

import org.seasar.doma.internal.jdbc.entity.EntityPropertyType;
import org.seasar.doma.internal.jdbc.entity.EntityType;
import org.seasar.doma.internal.jdbc.sql.PreparedSql;
import org.seasar.doma.internal.jdbc.sql.PreparedSqlBuilder;
import org.seasar.doma.jdbc.SqlKind;

/**
 * @author taedium
 * 
 */
public class AutoBatchUpdateQuery<E> extends AutoBatchModifyQuery<E> implements
        BatchUpdateQuery {

    protected boolean versionIncluded;

    protected boolean optimisticLockExceptionSuppressed;

    public AutoBatchUpdateQuery(EntityType<E> entityType) {
        super(entityType);
    }

    public void prepare() {
        assertNotNull(config, callerClassName, callerMethodName, entities, sqls);
        Iterator<E> it = entities.iterator();
        if (it.hasNext()) {
            executable = true;
            executionSkipCause = null;
            currentEntity = it.next();
            entityType.preUpdate(currentEntity);
            prepareTableAndColumnNames();
            prepareIdAndVersionProperties();
            validateIdExistent();
            prepareOptions();
            prepareOptimisticLock();
            prepareTargetProperties();
            prepareSql();
        } else {
            return;
        }
        while (it.hasNext()) {
            currentEntity = it.next();
            entityType.preUpdate(currentEntity);
            prepareSql();
        }
        assertEquals(entities.size(), sqls.size());
    }

    protected void prepareOptimisticLock() {
        if (versionPropertyType != null && !versionIncluded) {
            if (!optimisticLockExceptionSuppressed) {
                optimisticLockCheckRequired = true;
            }
        }
    }

    protected void prepareTargetProperties() {
        for (EntityPropertyType<E, ?> p : entityType.getEntityPropertyTypes()) {
            if (!p.isUpdatable()) {
                continue;
            }
            if (p.isId()) {
                continue;
            }
            if (p.isVersion()) {
                targetProperties.add(p);
                continue;
            }
            if (!isTargetPropertyName(p.getName())) {
                continue;
            }
            targetProperties.add(p);
        }
    }

    protected void prepareSql() {
        PreparedSqlBuilder builder = new PreparedSqlBuilder(config,
                SqlKind.BATCH_UPDATE);
        builder.appendSql("update ");
        builder.appendSql(tableName);
        builder.appendSql(" set ");
        for (EntityPropertyType<E, ?> p : targetProperties) {
            builder.appendSql(columnNameMap.get(p.getName()));
            builder.appendSql(" = ");
            builder.appendWrapper(p.getWrapper(currentEntity));
            if (p.isVersion() && !versionIncluded) {
                builder.appendSql(" + 1");
            }
            builder.appendSql(", ");
        }
        builder.cutBackSql(2);
        if (idProperties.size() > 0) {
            builder.appendSql(" where ");
            for (EntityPropertyType<E, ?> p : idProperties) {
                builder.appendSql(columnNameMap.get(p.getName()));
                builder.appendSql(" = ");
                builder.appendWrapper(p.getWrapper(currentEntity));
                builder.appendSql(" and ");
            }
            builder.cutBackSql(5);
        }
        if (versionPropertyType != null && !versionIncluded) {
            if (idProperties.size() == 0) {
                builder.appendSql(" where ");
            } else {
                builder.appendSql(" and ");
            }
            builder.appendSql(columnNameMap.get(versionPropertyType.getName()));
            builder.appendSql(" = ");
            builder
                    .appendWrapper(versionPropertyType
                            .getWrapper(currentEntity));
        }
        PreparedSql sql = builder.build();
        sqls.add(sql);
    }

    @Override
    public void incrementVersions() {
        if (versionIncluded) {
            return;
        }
        for (E entity : entities) {
            if (versionPropertyType != null) {
                versionPropertyType.increment(entity);
            }
        }
    }

    public void setVersionIncluded(boolean versionIncluded) {
        this.versionIncluded = versionIncluded;
    }

    public void setOptimisticLockExceptionSuppressed(
            boolean optimisticLockExceptionSuppressed) {
        this.optimisticLockExceptionSuppressed = optimisticLockExceptionSuppressed;
    }

}
