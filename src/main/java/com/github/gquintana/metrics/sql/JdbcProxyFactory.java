package com.github.gquintana.metrics.sql;

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


import com.github.gquintana.metrics.proxy.ProxyFactory;
import com.github.gquintana.metrics.proxy.ReflectProxyFactory;
import io.micrometer.core.instrument.MeterRegistry;

import javax.sql.*;
import javax.sql.rowset.*;
import java.sql.*;

/**
 * Factory of {@code JdbcProxyHandler} sub classes, central class of Metrics SQL.
 * It can be used to wrap any JDBC component (connection, statement,
 * result set...). 
 */
public class JdbcProxyFactory {
    /**
     * Timer manager
     */
    private final MetricHelper metricHelper;
    /**
     * Proxy factory
     */
    private final ProxyFactory proxyFactory;

    /**
     * Constructor using default {@link ReflectProxyFactory} and default {@link DefaultMetricNamingStrategy}
     * @param metricRegistry Metric registry to store metrics
     */
    public JdbcProxyFactory(MeterRegistry metricRegistry) {
        this(metricRegistry, new DefaultMetricNamingStrategy());
    }

    /**
     * Constructor
     *
     * @param registry Registry storing metrics
     * @param namingStrategy Naming strategy used to get metrics from SQL
     */
    public JdbcProxyFactory(MeterRegistry registry, MetricNamingStrategy namingStrategy) {
        this(registry, namingStrategy, new ReflectProxyFactory());
    }

    /**
     * Constructor
     *
     * @param registry Registry storing metrics
     * @param namingStrategy Naming strategy used to get metrics from SQL
     * @param proxyFactory AbstractProxyFactory to use for proxy creation
     */
    public JdbcProxyFactory(MeterRegistry registry, MetricNamingStrategy namingStrategy, ProxyFactory proxyFactory) {
        this.metricHelper = new MetricHelper(registry, namingStrategy);
        this.proxyFactory = proxyFactory;
    }

    /**
     * Create a proxy for given JDBC proxy handler
     * @param <T> Proxy type
     * @param proxyHandler Proxy handler
     * @return Proxy
     */
    private <T> T newProxy(JdbcProxyHandler<T> proxyHandler) {
        return proxyFactory.newProxy(proxyHandler, proxyHandler.getProxyClass());
    }
    
    /**
     * Wrap a data source to monitor it.
     *
     * @param wrappedDataSource Data source to wrap
     * @return Wrapped data source
     */
    public DataSource wrapDataSource(DataSource wrappedDataSource) {
        return newProxy(new DataSourceProxyHandler(wrappedDataSource, this));
    }

    /**
     * Wrap a connection to monitor it.
     *
     * @param wrappedConnection Connection to wrap
     * @return Wrapped connection
     */
    public Connection wrapConnection(Connection wrappedConnection) {
        TimeObservation lifeTimerContext = metricHelper.startConnectionLifeTimer();
        return newProxy(new ConnectionProxyHandler(wrappedConnection, this, lifeTimerContext));
    }
    
    /**
     * Wrap a simple statement to monitor it.
     *
     * @param statement Statement to wrap
     * @return Wrapped statement
     */
    public Statement wrapStatement(Statement statement) {
        TimeObservation lifeTimerContext = getMetricHelper().startStatementLifeTimer();
        return newProxy(new StatementProxyHandler(statement, this, lifeTimerContext));
    }

    /**
     * Wrap a prepared statement to monitor it.
     *
     * @param preparedStatement Prepared statement to wrap
     * @param sql SQL
     * @return Wrapped prepared statement
     */
    public PreparedStatement wrapPreparedStatement(PreparedStatement preparedStatement, String sql) {
        Query query = new Query(sql);
        TimeObservation lifeTimerContext = getMetricHelper().startPreparedStatementLifeTimer(query);
        return newProxy(new PreparedStatementProxyHandler(preparedStatement, this, query, lifeTimerContext));
    }

    /**
     * Wrap a callable statement to monitor it.
     *
     * @param callableStatement Prepared statement to wrap
     * @param sql SQL
     * @return Wrapped prepared statement
     */
    public CallableStatement wrapCallableStatement(CallableStatement callableStatement, String sql) {
        Query query = new Query(sql);
        TimeObservation lifeTimerContext = getMetricHelper().startCallableStatementLifeTimer(query);
        return newProxy(new CallableStatementProxyHandler(callableStatement, this, query, lifeTimerContext));
    }

    /**
     * Wrap a result set to monitor it.
     *
     * @param resultSet set to wrap
     * @param sql SQL related to Result set
     * @return Wrapped prepared statement
     */
    public ResultSet wrapResultSet(ResultSet resultSet, String sql) {
        Query query = new Query(sql);
        TimeObservation lifeTimerContext = metricHelper.startResultSetLifeTimer(query);
        return (ResultSet) newProxy(new ResultSetProxyHandler(resultSet, getResultSetType(resultSet), this, query, lifeTimerContext));
    }

    /**
     * Wrap a result set to monitor it.
     *
     * @param resultSet set to wrap
     * @param query SQL query of result set
     * @param lifeTimerContext Started timer
     * @return Wrapped prepared statement
     */
    public ResultSet wrapResultSet(ResultSet resultSet, Query query, TimeObservation lifeTimerContext) {
        return (ResultSet) newProxy(new ResultSetProxyHandler(resultSet, getResultSetType(resultSet), this, query, lifeTimerContext));
    }
    /**
     * Determine the interface implemented by this result set
     *
     * @param resultSet Result set
     */
    private Class<? extends ResultSet> getResultSetType(ResultSet resultSet) {
        Class<? extends ResultSet> resultSetType;
        if (resultSet instanceof RowSet) {
            if (resultSet instanceof CachedRowSet) {
                if (resultSet instanceof WebRowSet) {
                    if (resultSet instanceof FilteredRowSet) {
                        resultSetType = FilteredRowSet.class;
                    } else if (resultSet instanceof JoinRowSet) {
                        resultSetType = JoinRowSet.class;
                    } else {
                        resultSetType = WebRowSet.class;
                    }
                } else {
                    resultSetType = CachedRowSet.class;
                }
            } else if (resultSet instanceof JdbcRowSet) {
                resultSetType = JdbcRowSet.class;
            } else {
                resultSetType = RowSet.class;
            }
        } else {
            resultSetType = ResultSet.class;
        }
        return resultSetType;
    }

    public MetricHelper getMetricHelper() {
        return metricHelper;
    }
}
