package net.gquintana.metrics.sql;

/*
 * #%L
 * Metrics SQL
 * %%
 * Copyright (C) 2014 Open-Source
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import javax.sql.PooledConnection;
import net.gquintana.metrics.util.DefaultMetricRegistryHolder;
import net.gquintana.metrics.util.MetricRegistryHolder;

/**
 *
 */
public class DefaultMetricNamingStrategy implements MetricNamingStrategy {
    private final MetricRegistryHolder metricRegistryHolder;

    public DefaultMetricNamingStrategy(MetricRegistryHolder metricRegistryHolder) {
        this.metricRegistryHolder = metricRegistryHolder;
    }

    public DefaultMetricNamingStrategy(MetricRegistry metricRegistry) {
        this.metricRegistryHolder = new DefaultMetricRegistryHolder(metricRegistry);
    }
    private Timer.Context startTimer(Class<?> clazz, String ... name) {
        return metricRegistryHolder.getMetricRegistry().timer(MetricRegistry.name(clazz, name)).time();
    }
    public Timer.Context startPooledConnectionTimer(String connectionFactoryName) {
        return startTimer(PooledConnection.class, connectionFactoryName);
    }

    public Timer.Context startConnectionTimer(String connectionFactoryName) {
        return startTimer(Connection.class, connectionFactoryName);
    }

    public Timer.Context startStatementTimer(String connectionFactoryName) {
        return startTimer(Statement.class, connectionFactoryName);
    }
    protected String getSqlId(String sql) {
        return "["+sql.toLowerCase()+"]";
    }
    protected StatementTimerContext startStatementTimer(Class<? extends Statement> clazz, String connectionFactoryName, String sql, String sqlId) {
        final String lSqlId = sqlId == null ? getSqlId(sql) : sqlId;
        final Timer.Context timerContext = startTimer(clazz, connectionFactoryName, lSqlId);
        return new StatementTimerContext(timerContext, sql, lSqlId);
    }

    protected StatementTimerContext startStatementExecuteTimer(Class<? extends Statement> clazz, String connectionFactoryName, String sql, String sqlId) {
        final String lSqlId = sqlId == null ? getSqlId(sql) : sqlId;
        final Timer.Context timerContext = startTimer(clazz, connectionFactoryName, lSqlId, "exec");
        return new StatementTimerContext(timerContext, sql, lSqlId);
    }

    public StatementTimerContext startStatementExecuteTimer(String connectionFactoryName, String sql) {
        return startStatementExecuteTimer(Statement.class, connectionFactoryName, sql, null);
    }


    public StatementTimerContext startPreparedStatementTimer(String connectionFactoryName, String sql, String sqlId) {
        return startStatementTimer(PreparedStatement.class, connectionFactoryName, sql, sqlId);
    }

    public StatementTimerContext startPreparedStatementExecuteTimer(String connectionFactoryName, String sql, String sqlId) {
        return startStatementExecuteTimer(PreparedStatement.class, connectionFactoryName, sql, sqlId);
    }

    public StatementTimerContext startCallableStatementTimer(String connectionFactoryName, String sql, String sqlId) {
        return startStatementTimer(CallableStatement.class, connectionFactoryName, sql, sqlId);
    }

    public StatementTimerContext startCallableStatementExecuteTimer(String connectionFactoryName, String sql, String sqlId) {
        return startStatementExecuteTimer(CallableStatement.class, connectionFactoryName, sql, sqlId);
    }

    public Timer.Context startResultSetTimer(String connectionFactoryName, String sql, String sqlId) {
        return startTimer(ResultSet.class, connectionFactoryName, sqlId);
    }
}
