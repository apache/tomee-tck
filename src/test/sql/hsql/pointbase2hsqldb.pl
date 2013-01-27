#!/usr/bin/perl -w

use strict;
use warnings;

while (<>) {

    unless (m/PRIMARY KEY NOT NULL/) {
	print $_;
	next;
    }

# CREATE TABLE CUSTOMER_TABLE ( ID VARCHAR(255)   PRIMARY KEY NOT NULL, NAME VARCHAR(255) , COUNTRY VARCHAR(255), CODE VARCHAR(255), FK5_FOR_CUSTOMER_TABLE VARCHAR(255) , FK6_FOR_CUSTOMER_TABLE VARCHAR(255));

    my ($table, $paramString) = m/CREATE +TABLE +([^ ]+) *\( *(.*) *\) *;/;
    print $table . "\n";
    my @params = split(" *, *", $paramString);

    my $constraint = "constraint PK_${table} PRIMARY KEY";

    foreach (@params) {
	if (s/ PRIMARY KEY (NOT NULL)/$1/) {
	    my @t = split(/ +/, $_);
	    $constraint .= "($t[0])";
	}
    }

    push @params, $constraint;

    print "CREATE TABLE $table (";
    print join(", ", @params);
    print ");\n";

#    foreach (@params) {
#	print " -  $_\n";
#    }
}
