/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.openejb.tck.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @version $Rev$ $Date$
 */
public class GeneratePackages {

    public static void main(final String[] args) throws Exception {
        new GeneratePackages().main();
    }

    public void main() throws Exception {

        final File file = new File(System.getProperty("user.dir"), "webprofile.packages");
        final Node root = new Node(null, "");

        final BufferedReader reader = new BufferedReader(new FileReader(file));

        String line = null;

        while ((line = reader.readLine()) != null) {
            Node parent = root;

            final String[] packages = line.split("\\.");
            for (final String p : packages) {
                parent = parent.child(p);
            }
        }

        final String pad = "";
        for (final Node node : root.children.values()) {
            print(node, pad);
        }
    }

    private void print(final Node node, String pad) {
        if (node.children.size() == 0) {
            System.out.println(pad + node.name);
        } else {
            pad += node.name + ".";
            for (final Node n : node.children.values()) {
                print(n, pad);
            }
        }
    }

    class Node {
        final String name;
        final Node parent;
        final Map<String, Node> children = new LinkedHashMap<String, Node>();

        Node(final Node parent, final String name) {
            this.parent = parent;
            this.name = name;
        }

        public Node child(final String name) {

            Node child = children.get(name);

            if (child == null) {
                child = new Node(this, name);
                children.put(name, child);
            }

            return child;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            final Node node = (Node) o;

            if (name != null ? !name.equals(node.name) : node.name != null) return false;
            if (parent != null ? !parent.equals(node.parent) : node.parent != null) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = name != null ? name.hashCode() : 0;
            result = 31 * result + (parent != null ? parent.hashCode() : 0);
            return result;
        }
    }
}
