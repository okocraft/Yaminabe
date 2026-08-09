package net.okocraft.yaminabe.paper.testsupport;

import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * Sets the test server up before any test runs.
 * <p>
 * Registered as a service in {@code META-INF/services} and picked up by the extension auto-detection the test task
 * turns on, so that a test does not have to remember to ask for a server that touching an item already needs.
 */
public final class TestServerExtension implements BeforeAllCallback {

    @Override
    public void beforeAll(ExtensionContext context) {
        TestServer.setUp();
    }
}
