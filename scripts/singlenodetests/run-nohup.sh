#!/bin/bash
#if [ -f LNSlogfile ]; then
#mv --backup=numbered LNSlogfile LNSlogfile.save
#fi
#if [ -f NSlogfile ]; then
#mv --backup=numbered NSlogfile NSlogfile.save
#fi
nohup java -ea -DgigapaxosConfig=../../conf/gigapaxos.gnsApp.properties -cp ../../dist/GNS.jar edu.umass.cs.gns.gnsApp.AppReconfigurableNode -test -configFile ns.properties > NSlogfile 2>&1 &
nohup java -ea -cp ../../dist/GNS.jar edu.umass.cs.gns.localnameserver.LocalNameServer -nsfile ../../conf/single-server-info -configFile lns.properties > LNSlogfile 2>&1 &