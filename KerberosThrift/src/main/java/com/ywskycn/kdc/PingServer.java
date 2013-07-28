/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.ywskycn.kdc;

import org.apache.hadoop.security.SaslRpcServer;
import org.apache.thrift.TException;
import org.apache.thrift.server.TServer;
import org.apache.thrift.server.TThreadPoolServer;
import org.apache.thrift.transport.TSaslServerTransport;
import org.apache.thrift.transport.TServerSocket;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.AppConfigurationEntry;
import javax.security.auth.login.Configuration;
import javax.security.sasl.AuthorizeCallback;
import javax.security.sasl.Sasl;
import java.util.HashMap;
import java.util.Map;

public class PingServer {
  private TServer server;
  public static int port = 12345;
  public static final String principalName = "host";
  public static final String principalServer = "localhost";
  private final String keytabFilePath = "/tmp/host.keytab";
  private final String realm = "EXAMPLE.COM";

  public void start() throws Exception {
    // setup configuration
    Map<String, String> saslProperties = new HashMap<String, String>();
    saslProperties.put(Sasl.QOP, "auth-conf");
    System.setProperty("javax.security.auth.useSubjectCredsOnly", "false");
    System.setProperty("sun.security.krb5.debug", "true");
    System.setProperty("java.security.krb5.realm", realm);
    System.setProperty("java.security.krb5.kdc", "localhost");

    Configuration.setConfiguration(new Configuration() {
      @Override
      public AppConfigurationEntry[] getAppConfigurationEntry(String name) {
        final Map<String, Object> options = new HashMap<String, Object>();
        options.put("useKeyTab", "true");
        options.put("debug", "true");
        options.put("keyTab", keytabFilePath);
        options.put("refreshKrb5Config", "true");
        options.put("principal", principalName + "/"
                + principalServer + "@" + realm);
        options.put("useTicketCache", "true");
        options.put("storeKey", "true");
        options.put("storePass", "true");

        return new AppConfigurationEntry[]{
                new AppConfigurationEntry(
                        "com.sun.security.auth.module.Krb5LoginModule",
                        AppConfigurationEntry.LoginModuleControlFlag.REQUIRED,
                        options)};
      }
    });

    // configure server and start
    TServerSocket serverTransport = new TServerSocket(port);
    PingService.Processor<PingService.Iface> process =
            new PingService.Processor<PingService.Iface>(
                    new PingServiceAction());
    TSaslServerTransport.Factory saslTransportFactory =
            new TSaslServerTransport.Factory();
    saslTransportFactory.addServerDefinition(
            "GSSAPI", principalName, principalServer, saslProperties,
            new SaslRpcServer.SaslGssCallbackHandler()); // taken from Hadoop

    server = new TThreadPoolServer(
            new TThreadPoolServer.Args(serverTransport)
              .transportFactory(saslTransportFactory)
              .processor(process));
    server.serve();
  }


  public static void main(String args[]) throws Exception {
    PingServer ps = new PingServer();
    ps.start();
  }

  /**
   * action when server receives the ping msg
   */
  private class PingServiceAction implements PingService.Iface {
    @Override
    public String ping(int id) throws TException {
      System.out.println("Server received ping with id " + id);
      return "SUCCESS";
    }
  }
}
