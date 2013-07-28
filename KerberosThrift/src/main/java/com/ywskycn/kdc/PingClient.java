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

import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TSaslClientTransport;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;

import javax.security.auth.login.AppConfigurationEntry;
import javax.security.auth.login.Configuration;
import javax.security.sasl.Sasl;
import java.util.HashMap;
import java.util.Map;

public class PingClient {
  private final String principalName = "foo";
  private final String keytabFilePath = "/tmp/foo.keytab";
  private final String realm = "EXAMPLE.COM";

  public void pingServer() throws Exception {
    // configuration
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
        options.put("keyTab", keytabFilePath);
        options.put("useTicketCache", "true");
        options.put("principal", principalName + "@" + realm);
        options.put("refreshKrb5Config", "true");
        options.put("debug", "true");

        return new AppConfigurationEntry[]{
                new AppConfigurationEntry(
                        "com.sun.security.auth.module.Krb5LoginModule",
                        AppConfigurationEntry.LoginModuleControlFlag.REQUIRED,
                        options)};
      }
    });

    TTransport transport = new TSocket("localhost", PingServer.port);
    TTransport saslTransport = new TSaslClientTransport(
            "GSSAPI", null, PingServer.principalName, PingServer.principalServer,
            saslProperties, null, transport);
    TProtocol protocol = new TBinaryProtocol(saslTransport);
    PingService.Client client = new PingService.Client(protocol);
    saslTransport.open();
    System.out.println("Client received message from server " + client.ping(123));
    transport.close();
  }

  public static void main(String args[]) throws Exception {
    PingClient client = new PingClient();
    client.pingServer();
  }
}
