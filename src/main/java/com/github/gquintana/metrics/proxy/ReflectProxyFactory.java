package com.github.gquintana.metrics.proxy;

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


import java.lang.reflect.Proxy;

/**
 * Factory of proxies producing proxies based on Java reflection
 */
public class ReflectProxyFactory implements ProxyFactory {
	/**
	 * {@inheritDoc}
	 */
        @Override
	public <T> T newProxy(ProxyHandler<T> proxyHandler, ProxyClass proxyClass) {
		return (T) Proxy.newProxyInstance(proxyClass.getClassLoader(), proxyClass.getInterfaces(), proxyHandler);
	}
}
