#!/bin/bash
# Copyright 2020 Mavenir
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

echo "======== Starting script ========"
ctrlen=`cat /proc/self/cgroup | head -1 | cut -d '/' -f 5 | wc -c`
if [ "$ctrlen" == "65" ];
then
  export K8S_CONTAINER_ID=`cat /proc/self/cgroup | head -1 | cut -d '/' -f 5`
else
  export K8S_CONTAINER_ID=`cat /proc/self/cgroup | head -1 | cut -d '/' -f 4`
fi
echo "CID $K8S_CONTAINER_ID"


#while ! nc -w 1 localhost 4222 2>/dev/null
#do
#  echo "======== Waiting for oam-sidecar ======== "
#  sleep 1
#done

echo "$JAVA_OPTIONS"
echo "======== Starting FaultService ========"
java -XX:+PrintFlagsFinal $JAVA_OPTIONS -jar /opt/fmaas/fmaas-v0.1.jar

echo "======== Exiting container ======="

