#!/usr/bin/perl
use strict;
use warnings;

my $limit = $ARGV[0];

sub unique {
    my @list = @_;
    my %seen = ();
    return grep { ! $seen{$_} ++ } @list;
}

my %exceptions = ();

local $/ = "Beginning Test:  ";
foreach my $testOutput (<STDIN>) {
    chomp $testOutput;
    
    my ($test) = $testOutput =~ m/(^.*)/;
    my @thrown = unique($testOutput =~ m/(Exception:.*)/g);

    foreach my $t (@thrown) {
	# these two will be in nearly every failed test, just skip them
	next if $t =~ m/Exception: Deployment Failed./;
	next if $t =~ m/Exception: Distribute to one or more targets failed Failed targets: DefaultTarget/;

	# Maybe remove some details so failures can be more easily grouped
	
	$t =~ s,(Could not load WEB-INF/classes/).*(.class),${1}.....${2},;
	$t =~ s,(http://localhost:).*(_vehicle_[^/]+)/.*,$1/.....$2.....,;
	$t =~ s,(404 Not Found for http://localhost:8080/jsf_).*,$1.....,;
	$t =~ s,(Module failed validation.).*,$1.....,;
	$t =~ s,(No provider available for resource-ref 'null' of type 'com.sun.ts.tests.common.connector.whitebox.TSConnectionFactory').*,$1.....,;
	$t =~ s,(Table not found).*,$1.....,;
	$t =~ s,(java.lang.NoClassDefFoundError: Could not fully load class: com.sun.ts.tests.jsf).*,$1.....,;

	push( @{$exceptions{$t}}, $test);
    }
}

my @sortedKeys = sort {$#{$exceptions{$b}} <=> $#{$exceptions{$a}}} keys %exceptions;

my $l = 0;
foreach my $exception (@sortedKeys) {
    my $count = $#{$exceptions{$exception}} +1;
    print "$count\t$exception\n";
    last if ++$l >= $limit;
}

print "\n\n\n";
$l = 0;
foreach my $exception (@sortedKeys) {
    my @tests = @{$exceptions{$exception}};
    
    next if ( $#tests < 5 );

    print "$exception\n";
    my $i = 1;
    foreach my $test (@tests) {
	print " @{[$i++]}\t$test\n";
    }

    print "\n\n\n";

    last if ++$l >= $limit;
}
