#!/usr/bin/perl
use strict;
use warnings;

# Just pipe some build output to this script and it will update the status.log

open LOG, ">", "status.log" or die "Can't open file $!\n";

while (<STDIN>) {
    print $_;
    print LOG if s/ *([^ ]+) - .*(PASSED|FAILED|ERROR).*/$2 $1/;
    print LOG if s/^ *((Passed|Failed|Errors):.*)/$1/;
}
close LOG;
