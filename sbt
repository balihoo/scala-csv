#!/bin/sh
java -Xms512M -Xmx1536M -Xss1M -XX:+CMSClassUnloadingEnabled -XX:MaxPermSize=384M -Dsbt.ivy.home=${HOME}/.ivy2 -jar `dirname $0`/sbt-launch-0.13.7-M3.jar "$@"
