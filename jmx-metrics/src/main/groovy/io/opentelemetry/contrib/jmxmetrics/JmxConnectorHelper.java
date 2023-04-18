/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.jmxmetrics;

import java.io.IOException;
import java.net.MalformedURLException;
import java.rmi.NotBoundException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import javax.management.remote.rmi.RMIConnector;
import javax.management.remote.rmi.RMIServer;
import javax.rmi.ssl.SslRMIClientSocketFactory;

public class JmxConnectorHelper {

  private static final Logger logger = Logger.getLogger(JmxConnectorHelper.class.getName());

  private static RMIServer stub = null;
  private static final SslRMIClientSocketFactory sslRMIClientSocketFactory =
      new SslRMIClientSocketFactory();

  private JmxConnectorHelper() {}

  public static boolean validateConfig(final JmxConfig config) throws MalformedURLException {

    if (!config.registrySsl) {
      // ensure the service URL is valid, as it will be used to connect to the
      // MBean server when SSL is not enabled on the RMI registry.
      JMXServiceURL jmxServiceURL = new JMXServiceURL(config.serviceUrl);
    }
    return true;
  }

  public static JMXConnector connect(
      String serviceURL, String hostName, int port, Map<String, Object> env, boolean registrySsl)
      throws IOException {

    // Different connection logic is needed when SSL is enabled on the RMI registry
    if (registrySsl) {
      logger.log(Level.INFO, "Attempting to connect to an SSL-protected RMI registry");

      env.put("jmx.remote.x.check.stub", "true");
      // Check for SSL config on reconnection only
      if (stub == null) {
        getStub(hostName, port);
      }
      JMXConnector jmxConn = new RMIConnector(stub, null);
      jmxConn.connect(env);
      return jmxConn;
    } else {
      JMXServiceURL jmxServiceURL = new JMXServiceURL(serviceURL);
      return JMXConnectorFactory.connect(jmxServiceURL, env);
    }
  }

  private static void getStub(String hostName, int port) throws IOException {
    // Get the reference to the RMI Registry and lookup RMIServer stub
    Registry registry;
    try {
      registry = LocateRegistry.getRegistry(hostName, port, sslRMIClientSocketFactory);
      stub = (RMIServer) registry.lookup("jmxrmi");
    } catch (NotBoundException nbe) {
      throw new IOException(nbe.getMessage());
    }
  }
}
