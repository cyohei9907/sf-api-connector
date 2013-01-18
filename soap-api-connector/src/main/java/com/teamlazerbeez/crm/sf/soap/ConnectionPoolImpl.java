/*
 * Copyright © 2010. Team Lazer Beez (http://teamlazerbeez.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.teamlazerbeez.crm.sf.soap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.ThreadSafe;
import java.util.HashMap;
import java.util.Map;

/*
* silence complaints about the static logger
*/
@SuppressWarnings("AccessToStaticFieldLockedOnInstance")
@ThreadSafe
public final class ConnectionPoolImpl<T> implements ConnectionPool<T> {

    private static final Logger logger = LoggerFactory.getLogger(ConnectionPoolImpl.class);

    private static final BundleFactory NORMAL_BUNDLE_FACTORY = new NormalBundleFactory();

    private static final BundleFactory SANDBOX_BUNDLE_FACTORY = new SandboxBundleFactory();

    private final Map<T, ConnectionBundleImpl> orgIdToBundleMap = new HashMap<T, ConnectionBundleImpl>();

    private final Map<T, ConnectionBundleImpl> orgIdToSandboxBundleMap = new HashMap<T, ConnectionBundleImpl>();

    private final BindingRepository bindingRepository;

    public ConnectionPoolImpl(@Nonnull String partnerKey) {
        this.bindingRepository = new BindingRepository(partnerKey);
    }

    @Override
    public synchronized ConnectionBundle getConnectionBundle(T orgIdentifier) {
        return getConnectionBundleImpl(this.orgIdToBundleMap, orgIdentifier);
    }

    @Override
    public synchronized ConnectionBundle getSandboxConnectionBundle(@Nonnull T orgIdentifier) {
        return getConnectionBundleImpl(this.orgIdToSandboxBundleMap, orgIdentifier);
    }

    private ConnectionBundle getConnectionBundleImpl(Map<T, ConnectionBundleImpl> bundles, T orgId) {
        ConnectionBundle cp = bundles.get(orgId);

        if (cp == null) {
            throw new IllegalStateException(
                    "The ConnectionBundle for org id <" + orgId + "> has not been configured yet!");
        }

        return cp;
    }

    @Override
    public synchronized void configureOrg(T orgId, String username, String password,
            int maxConcurrentApiCalls) {
        configureOrgImpl(orgId, username, password, maxConcurrentApiCalls, this.orgIdToBundleMap,
                NORMAL_BUNDLE_FACTORY);
    }

    @Override
    public synchronized void configureSandboxOrg(T orgId, String username, String password,
            int maxConcurrentApiCalls) {
        configureOrgImpl(orgId, username, password, maxConcurrentApiCalls, this.orgIdToSandboxBundleMap,
                SANDBOX_BUNDLE_FACTORY);
    }

    private void configureOrgImpl(T orgId, String username, String password, int maxConcurrentApiCalls,
            Map<T, ConnectionBundleImpl> bundles, BundleFactory bundleFactory) {
        ConnectionBundleImpl cp = bundles.get(orgId);

        if (cp == null) {
            logger.debug("Initial configuration for org " + orgId);

            bundles.put(orgId,
                    bundleFactory.getBundle(this.bindingRepository, username, password, maxConcurrentApiCalls));
        } else {
            logger.debug("Updating existing configuration for org " + orgId);

            cp.updateCredentials(username, password, maxConcurrentApiCalls);
        }
    }

    @ThreadSafe
    private static interface BundleFactory {
        @Nonnull
        ConnectionBundleImpl getBundle(@Nonnull BindingRepository bindingRepository, @Nonnull String username,
                @Nonnull String password, int maxConcurrentApiCalls);
    }

    @Immutable
    private static class NormalBundleFactory implements BundleFactory {
        @Override
        public ConnectionBundleImpl getBundle(@Nonnull BindingRepository bindingRepository, @Nonnull String username,
                @Nonnull String password, int maxConcurrentApiCalls) {
            return ConnectionBundleImpl.getNew(bindingRepository, username, password, maxConcurrentApiCalls);
        }
    }

    @Immutable
    private static class SandboxBundleFactory implements BundleFactory {
        @Override
        public ConnectionBundleImpl getBundle(@Nonnull BindingRepository bindingRepository, @Nonnull String username,
                @Nonnull String password, int maxConcurrentApiCalls) {
            return ConnectionBundleImpl.getNewForSandbox(bindingRepository, username, password, maxConcurrentApiCalls);
        }
    }
}
