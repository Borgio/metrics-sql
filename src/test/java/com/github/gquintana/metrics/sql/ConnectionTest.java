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

import com.codahale.metrics.MetricRegistry;
import io.micrometer.core.instrument.dropwizard.DropwizardMeterRegistry;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
/**
 * Test connection wrapper
 */
public class ConnectionTest {
    private DropwizardMeterRegistry meterRegistry;
    private JdbcProxyFactory proxyFactory;
    @Before
    public void setUp() {
        meterRegistry = MeterRegistryHelper.createDropwizardMeterRegistry();
        proxyFactory = new JdbcProxyFactory(meterRegistry);
    }
    @Test
    public void testConnectionLife() throws SQLException {
        // Act
        Connection connection = proxyFactory.wrapConnection(H2DbUtil.openConnection());
        H2DbUtil.close(connection);
        // Assert
        assertNotNull(connection);
        assertNotNull(meterRegistry.getDropwizardRegistry().getTimers().get("java.sql.Connection"));
        
    }
    @Test
    public void testConnectionStatement() throws SQLException {
        // Act
        Connection connection = proxyFactory.wrapConnection(H2DbUtil.openConnection());
        Statement statement = connection.createStatement();
        PreparedStatement preparedStatement = connection.prepareCall("select 1 from dual");
        H2DbUtil.close(preparedStatement, statement, connection);
        // Assert
        assertTrue(Proxy.isProxyClass(statement.getClass()));
        assertTrue(Proxy.isProxyClass(preparedStatement.getClass()));
    }
    @Test
    public void testConnectionNotTimed() throws SQLException {
        // Act
        Connection connection = proxyFactory.wrapConnection(H2DbUtil.openConnection());
        String dbProduct = connection.getMetaData().getDatabaseProductName();
        // Assert
        assertTrue(dbProduct.toLowerCase().contains("h2"));
    }
        @Test
    public void testResultSetUnwrap() throws SQLException {
        // Act
        Connection connection = proxyFactory.wrapConnection(H2DbUtil.openConnection());
        
        // Assert        
        assertTrue(connection.isWrapperFor(org.h2.jdbc.JdbcConnection.class));
        assertTrue(connection.unwrap(org.h2.jdbc.JdbcConnection.class) instanceof org.h2.jdbc.JdbcConnection);
        
        H2DbUtil.close(connection);
    }

}
