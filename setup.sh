#!/bin/bash
#
# This script will download and setup the Jakarta EE 9.1 TCK along
# with all requirements such as glassfish and ant.  A profile will
# be added to ~/.m2/settings.xml
#
# It is safe to run this script repeatedly
#

TCKDIR="${1?Specify the directory where you would like the TCK to be downloaded and setup}"


TCK_URL="https://download.eclipse.org/jakartaee/platform/10/jakarta-jakartaeetck-10.0.1.zip"
RI_URL="https://download.eclipse.org/ee4j/glassfish/glassfish-7.0.0-M8.zip"
ANT_URL="https://archive.apache.org/dist/ant/binaries/apache-ant-1.10.9-bin.zip"

####################################################################

cd "$TCKDIR" || exit 1

TCK=$(basename $TCK_URL| perl -pe 's,.zip$,,')
RI=$(basename $RI_URL| perl -pe 's,.zip$,,')
ANT=$(basename $ANT_URL| perl -pe 's,.zip$,,')

[ -f "$TCK.zip" ] || curl -LO $TCK_URL
[ -f "$RI.zip" ] || curl -LO $RI_URL
[ -f "$ANT.zip" ] || curl -LO $ANT_URL

(mkdir "$TCK" && cd "$TCK" && bsdtar --strip-components=1 -xf "../$TCK.zip")
(mkdir "$RI" && cd "$RI" && bsdtar --strip-components=1 -xf "../$RI.zip")
(mkdir -p "$TCKDIR/$TCK/tools/ant" && cd "$TCKDIR/$TCK/tools/ant" && bsdtar --strip-components=1 -xf "../../../$ANT.zip")

M2=~/.m2/settings.xml

# If there is no ~/.m2/settings.xml create it
[ -f "$M2" ] || echo '<settings>
</settings>' > $M2

# If there is no profiles section, add it
grep -q "<profiles>" $M2 || perl -i -pe 's,(<settings>),$1
  <profiles>
  </profiles>
,' $M2

# If there is no profiles section, add it
grep -q "<id>$TCK</id>" $M2 || perl -i -pe "s,(<profiles>),\$1
    <profile>
      <id>$TCK</id>
      <activation>
        <activeByDefault>true</activeByDefault>
      </activation>
      <properties>
        <jakartaee10.cts.home></jakartaee10.cts.home>
        <jakartaee10.ri.home></jakartaee10.ri.home>
      </properties>
    </profile>
," $M2

## Update paths in ~/.m2/settings.xml
perl -i -pe "s,(<jakartaee10.cts.home>)[^<]*,\${1}$TCKDIR/$TCK," $M2
perl -i -pe "s,(<jakartaee10.ri.home>)[^<]*,\${1}$TCKDIR/$RI/glassfish," $M2


################################################
#
# create a pom.xml for the tck so it can be
# easily opened in IDEs that support Maven
#
################################################
(cd "$TCK"



cat > pom.xml <<EOF
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
	 xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/maven-v4_0_0.xsd">

  <modelVersion>4.0.0</modelVersion>

  <groupId>jakartaee-tck</groupId>
  <artifactId>jakartaee-tck</artifactId>
  <version>10.0.1</version>
  
  <dependencies>
    <dependency>
      <groupId>org.apache.tomee</groupId>
      <artifactId>jakartaee-api</artifactId>
      <version>10.0.0-SNAPSHOT</version>
    </dependency>
$(
for n in lib/*.jar; do

    artifact="$(basename "$n" | perl -pe 's,(-[0-9.]+)?.jar$,,')"
    
    echo "    <dependency>
      <groupId>jakartaee-tck</groupId>
      <artifactId>$artifact</artifactId>
      <version>1.0</version>
      <scope>system</scope>
      <systemPath>\${project.basedir}/$n</systemPath>
    </dependency>"
done
)
  </dependencies>

  <build>
    <sourceDirectory>src</sourceDirectory>
    <plugins>
      <plugin>    
        <artifactId>maven-compiler-plugin</artifactId>
        <configuration>
          <source>1.8</source>
          <target>1.8</target>
        </configuration>
      </plugin>
    </plugins>
  </build>
</project>
EOF

)
