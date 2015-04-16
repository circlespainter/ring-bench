#!/bin/bash

jfrOpts="-XX:+UnlockCommercialFeatures -XX:+FlightRecorder -XX:FlightRecorderOptions=defaultrecording=true,dumponexit=true,dumponexitpath=path"

set -e

[ -z "$workerCount" ] && workerCount=503
[ -z "$ringSize" ] && ringSize=1000000
[ -z "$quasarAgentLocation" ] && quasarAgentLocation=$HOME/.m2/repository/co/paralleluniverse/quasar-core/0.6.3-SNAPSHOT/quasar-core-0.6.3-SNAPSHOT-jdk8.jar
[ -z "$warmupIters" ] && warmupIters=5
[ -z "$iters" ] && iters=10
[ -z "$stat" ] && stat=avg
[ -z "$unit" ] && unit=ms
[ -z "$forks" ] && forks=5
[ -z "$benchRegexp" ] && benchRegexp=".*Benchmark.*"
[ "$enableJfr" != "true" ] && enableJfr=false && jfrOpts=""

if [ "$1" = "-h" -o "$1" = "--help" ]; then
    echo "Available environment parameters (with defaults):"
    echo "    workerCount             ($workerCount)"
    echo "    ringSize                ($ringSize)"
    echo "    quasarAgentLocation     ($quasarAgentLocation)"
    echo "    warmupIters             ($warmupIters)"
    echo "    iters                   ($iters)"
    echo "    stat                    ($stat)"
    echo "    unit                    ($unit)"
    echo "    forks                   ($forks)"
    echo "    benchRegexp             ($benchRegexp)"
    echo "    enableJfr               ($enableJfr)"
    exit 0
fi

error() {
    echo "$@" >&2
}

if [ ! -e "$quasarAgentLocation" ]; then 
    error "Could not locate agent."
    error "Expected path: $quasarAgentLocation"
    exit 1
fi

cmd="$JAVA_HOME/bin/java -jar target/fiber-test.jar\
 -jvmArgsAppend \"$jfrOpts -DworkerCount=$workerCount -DringSize=$ringSize -javaagent:$quasarAgentLocation\"\
 -wi $warmupIters -i $iters -bm $stat -tu $unit -f $forks \"$benchRegexp\""

echo "$cmd"
eval "$cmd"