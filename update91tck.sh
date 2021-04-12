#!/bin/bash
#
# This script will look for a more recent Jakarta EE 9.1 SNAPSHOT TCK
# And download and install it onto your machine, then update the value
# of the <jakartaee91.cts.home> element in ~/.m2/settings.xml
#
# It is safe to run this script repeatedly as a way to check for new TCKs
#

grep -q -m 1 jakartaee91.cts.home ~/.m2/settings.xml || {
    echo "No <jakartaee91.cts.home> variable found in ~/.m2/settings.xml"
    echo "This script requires you to have setup the EE 9.1 TCK at least once manually"
    echo "See the README.adoc for further instructions"
    exit 1
}

grep -q -m 1 jakartaee91.ri.home ~/.m2/settings.xml || {
    echo "No <jakartaee91.ti.home> variable found in ~/.m2/settings.xml"
    echo "This script requires you to have setup the EE 9.1 TCK at least once manually"
    echo "See the README.adoc for further instructions"
    exit 1
}

## Download the jakarta-jakartaeetckinfo.txt from Eclipse and get basic meta data
TCKINFO="$(curl -s https://download.eclipse.org/ee4j/jakartaee-tck/jakartaee9-eftl/staged-910/jakarta-jakartaeetckinfo.txt)"
DATESTAMP="$(echo "$TCKINFO" | grep 'date:' | perl -pe 's,.*date: (\d\d\d\d-\d\d-\d\d) (\d\d):(\d\d).*,$1.$2$3,')"
URL="$(echo "$TCKINFO" | grep 'download.eclipse.org' | perl -pe 's,.*(://download.eclipse.org/[^ ]+\.zip).*,https$1,')"
SHA="$(echo "$TCKINFO" | grep 'SHA256SUM' | perl -pe 's,.*SHA256SUM: ([0-9a-f]+).*,$1,')"
NAME="$(echo "$TCKINFO" | grep 'Name:' | perl -pe 's,.*Name: *jakarta-([^ ]+)\.zip.*,$1,')"

## Look at our existing tck setup to see where TCKs should be installed
OLDTCK="$(grep jakartaee91.cts.home ~/.m2/settings.xml | perl -pe 's,.*home>([^<]+)<.*,$1,')"
TCKDIR="$(dirname "$OLDTCK")"

## Look at our existing tck setup to see where GlassFish should be installed
OLDRI="$(grep jakartaee91.ri.home ~/.m2/settings.xml | perl -pe 's,.*home>([^<]+)<.*,$1,')"
RIDIR="$(dirname "$OLDRI")"

TCK="$NAME-$DATESTAMP"

RI="glassfish-6.0.0"
RIURL="https://download.eclipse.org/ee4j/glassfish/$RI.zip"

echo "Latest TCK
NAME: $NAME
DATE: $DATESTAMP
URL:  $URL
SHA:  $SHA
DIR:  $TCKDIR
"
#https://download.eclipse.org/ee4j/glassfish/glassfish-6.0.0.zip
## Download the TCK if we have not
[ -f "$TCKDIR/$TCK.zip" ] || (
    echo "Downloading $TCK.zip"
    cd "$TCKDIR" &&
    curl "$URL" > "$TCK.zip"
)

echo "Downloaded $TCK.zip"

## Extract the TCK if we have not
[ -d "$TCKDIR/$TCK" ] || (
    echo "Extracting to $TCKDIR/$TCK"
    mkdir "$TCKDIR/$TCK" &&
	cd "$TCKDIR/$TCK" &&
	bsdtar --strip-components=1 -xf "../$TCK.zip"
)

echo "Extracted $TCK"

## Download the RI if we have not                                                                                                                      
[ -f "$RIDIR/$RI.zip" ] || (
    echo "Downloading $RI.zip"
    cd "$RIDIR" &&
    curl "$RIURL" > "$RI.zip"
)

echo "Downloaded $RI.zip"

## Extract the RI if we have not                                                                                                                       
[ -d "$RIDIR/$RI" ] || (
    echo "Extracting to $RIDIR/$RI"
    mkdir "$RIDIR/$RI" &&
        cd "$RIDIR/$RI" &&
        bsdtar --strip-components=1 -xf "../$RI.zip"
)

echo "Extracted $RI"

## Download ant if we have not
[ -f "$TCKDIR/apache-ant-1.10.9-bin.zip" ] || (
    echo "Downloading ant"
    cd "$TCKDIR" &&
	curl -s -O https://archive.apache.org/dist/ant/binaries/apache-ant-1.10.9-bin.zip
)

echo "Downloaded ant"

## Extract ant into TCK if we have not
[ -d "$TCKDIR/$TCK/tools/ant" ] || (
    echo "Extracting ant $TCKDIR/$TCK/tools/ant"
    mkdir -p "$TCKDIR/$TCK/tools/ant" &&
	cd "$TCKDIR/$TCK/tools/ant" &&
	bsdtar --strip-components=1 -xf "../../../apache-ant-1.10.9-bin.zip"
)

echo "Extracted ant"

## Update jakartaee91.cts.home in ~/.m2/settings.xml
perl -i -pe "s,(<jakartaee91.cts.home>)[^<]+<,\$1$TCKDIR/$TCK<," ~/.m2/settings.xml

## Update jakartaee91.ri.home in ~/.m2/settings.xml
perl -i -pe "s,(<jakartaee91.ri.home>)[^<]+<,\$1$RIDIR/$RI/glassfish<," ~/.m2/settings.xml

echo "Updated ~/.m2/settings.xml"

## Add info.txt and sha256 files into the extracted TCK
## so that we have the full details on what we've installed
echo "$TCKINFO" > "$TCKDIR/$TCK/info.txt"
echo "$SHA" > "$TCKDIR/$TCK/sha256"
