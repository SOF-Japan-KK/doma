/*
 * Copyright 2004-2010 the Seasar Foundation and the Others.
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
package org.seasar.doma.jdbc.query;

import static org.seasar.doma.internal.util.AssertionUtil.assertNotNull;

import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;

import org.seasar.doma.internal.expr.ExpressionEvaluator;
import org.seasar.doma.internal.expr.Value;
import org.seasar.doma.internal.jdbc.sql.NodePreparedSqlBuilder;
import org.seasar.doma.internal.jdbc.sql.PreparedSql;
import org.seasar.doma.jdbc.Config;
import org.seasar.doma.jdbc.SqlExecutionSkipCause;
import org.seasar.doma.jdbc.SqlFile;
import org.seasar.doma.jdbc.SqlKind;
import org.seasar.doma.jdbc.entity.EntityType;

/**
 * @author taedium
 * 
 */
public abstract class SqlFileModifyQuery implements ModifyQuery {

    protected final SqlKind kind;

    protected Method method;

    protected Config config;

    protected String sqlFilePath;

    protected final Map<String, Value> parameters = new LinkedHashMap<String, Value>();

    protected String callerClassName;

    protected String callerMethodName;

    protected PreparedSql sql;

    protected int queryTimeout;

    protected boolean optimisticLockCheckRequired;

    protected SqlFileModifyQuery(SqlKind kind) {
        assertNotNull(kind);
        this.kind = kind;
    }

    protected void prepareOptions() {
        if (queryTimeout <= 0) {
            queryTimeout = config.getQueryTimeout();
        }
    }

    protected void prepareSql() {
        SqlFile sqlFile = config.getSqlFileRepository().getSqlFile(method,
                sqlFilePath, config.getDialect());
        ExpressionEvaluator evaluator = new ExpressionEvaluator(parameters,
                config.getDialect().getExpressionFunctions(),
                config.getClassHelper());
        NodePreparedSqlBuilder sqlBuilder = new NodePreparedSqlBuilder(config,
                kind, sqlFile.getPath(), evaluator);
        sql = sqlBuilder.build(sqlFile.getSqlNode());
    }

    public void setMethod(Method method) {
        this.method = method;
    }

    @Override
    public Method getMethod() {
        return method;
    }

    public void setConfig(Config config) {
        this.config = config;
    }

    public void setSqlFilePath(String sqlFilePath) {
        this.sqlFilePath = sqlFilePath;
    }

    public void addParameter(String name, Class<?> type, Object value) {
        assertNotNull(name, type);
        addParameterInternal(name, type, value);
    }

    public void addParameterInternal(String name, Class<?> type, Object value) {
        parameters.put(name, new Value(type, value));
    }

    public void setCallerClassName(String callerClassName) {
        this.callerClassName = callerClassName;
    }

    public void setCallerMethodName(String callerMethodName) {
        this.callerMethodName = callerMethodName;
    }

    public void setQueryTimeout(int queryTimeout) {
        this.queryTimeout = queryTimeout;
    }

    @Override
    public PreparedSql getSql() {
        return sql;
    }

    @Override
    public String getClassName() {
        return callerClassName;
    }

    @Override
    public String getMethodName() {
        return callerMethodName;
    }

    @Override
    public Config getConfig() {
        return config;
    }

    @Override
    public boolean isOptimisticLockCheckRequired() {
        return optimisticLockCheckRequired;
    }

    @Override
    public boolean isExecutable() {
        return true;
    }

    @Override
    public SqlExecutionSkipCause getSqlExecutionSkipCause() {
        return null;
    }

    @Override
    public int getQueryTimeout() {
        return queryTimeout;
    }

    @Override
    public boolean isAutoGeneratedKeysSupported() {
        return false;
    }

    public abstract <E> void setEntityAndEntityType(String name, E entity,
            EntityType<E> entityType);

    @Override
    public String toString() {
        return sql != null ? sql.toString() : null;
    }

}
