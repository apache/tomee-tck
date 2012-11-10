/**
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.openejb.cts;

import org.apache.derby.drda.NetworkServerControl;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.openejb.util.Log4jPrintWriter;

import java.net.InetAddress;

public class DerbyRunner {

	private static class DerbyThread extends Thread {
		private static Logger log = Logger.getLogger(DerbyRunner.class);
		private static final int SLEEP_INTERVAL = 60000;
		private NetworkServerControl serverControl;
        private int port = NetworkServerControl.DEFAULT_PORTNUMBER;

        public DerbyThread (int derbyPort) {
            port = derbyPort;
        }

		public void run() {
			System.out.println("Starting embedded Derby database on port " + port);
			try {
	            serverControl = new NetworkServerControl(InetAddress.getByAddress(new byte[] {127, 0, 0, 1}), port);
	            serverControl.start(new Log4jPrintWriter("Derby", Level.INFO));
	        } catch (Exception e) {
	        	e.printStackTrace();
	        }
	        
	        while(true) {
	        	try {
					Thread.sleep(SLEEP_INTERVAL);
				} catch (InterruptedException e) {
					break;
				}
	        }
	        
	        System.out.println("Embedded database thread stopping");
		}
		
	}
	
	
	public static void main(String[] args) {
        int port = NetworkServerControl.DEFAULT_PORTNUMBER;
        if (args.length == 1) {
            try {
                port = Integer.parseInt(args[0]);
                //System.setProperty("derby.drda.portNumber", Integer.toString(port));
            } catch (NumberFormatException e) {
                System.out.println("Could not convert port " + args[0] + ". Using the default " + port);
            }
        }
        DerbyThread thread = new DerbyThread(port);
        thread.setDaemon(true);
		thread.setName("DerbyServerDaemon");
		thread.start();
	}
}
