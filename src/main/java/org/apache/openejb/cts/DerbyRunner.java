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

public class DerbyRunner {

	private static class DerbyThread extends Thread {
		private static Logger log = Logger.getLogger(DerbyRunner.class);
		private static final int SLEEP_INTERVAL = 60000;
		private NetworkServerControl serverControl;

		public void run() {
			System.out.println("Starting embedded Derby database");
			try {
	            serverControl = new NetworkServerControl("127.0.0.1", "11527");
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
		DerbyThread thread = new DerbyThread();
		thread.setDaemon(true);
		thread.setName("DerbyServerDaemon");
		thread.start();
	}
}
