/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package openejb.tck.util

/**
 * Helper to build Ant paths.
 *
 * @version $Revision$ $Date$
 */
class PathBuilder {
    def log

    def ant

    def paths = []

    def directory

    public PathBuilder(source) {
        assert source != null

        this.log = source.log
        this.ant = source.ant
    }

    def appendAll(includes, excludes) {
        assert includes != null
        assert directory != null

        def dir = new File(directory)
        assert dir.isDirectory(): "Not a directory " + dir.getAbsolutePath();

        def p = ant.path() {
            fileset(dir: dir) {
                include(name: includes)
                if (excludes != null) {
                    exclude(name: excludes)
                }
            }
        }

        def s = p.size()
        paths.add(p)
    }

    def appendAll(includes) {
        return appendAll(includes, null)
    }

    def append(includes, excludes) {
        assert includes != null
        assert directory != null

        def dir = new File(directory)
        assert dir.isDirectory(): "Not a directory " + dir.getAbsolutePath();

        def p = ant.path() {
            fileset(dir: dir) {
                include(name: includes)
                if (excludes != null) {
                    exclude(name: excludes)
                }
            }
        }

        def s = p.size()
        if (s == 0) {
            throw new Exception("Expected one file to be included into path; dir=${dir}, includes=${includes}, excludes=${excludes}")
        }
        if (s > 1) {
            throw new Exception("Found more than one file to be included into path; dir=${dir}, includes=${includes}, excludes=${excludes}; found: ${p}")
        }

        paths.add(p)
    }

    def appendDir(d) {
        assert d != null

        def dir = new File(d)
        assert dir.isDirectory(): "Not a directory " + dir.getAbsolutePath();

        def p = ant.path() { ant.pathelement(location: d) }
        paths.add(p)
    }


    def append(includes) {
        return append(includes, null)
    }

    def reference(id) {
        assert id != null

        def p = ant.path(refid: id)

        paths.add(p)
    }

    def getPath(id) {
        assert id != null

        def p = ant.path(id: id)

        paths.each {
            p.add(it)
        }

        return p
    }
}
