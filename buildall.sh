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

# This build script takes four arguments: $1=fmaas image tag, 
# $2=CLEAN_BUILD_BUILDER_IMAGE:(Optional): ["yes"|"no"] (Default: no). This will do a clean build of builder image.
# $3=DELETE_BUILDER_IMAGE:(Optional): ["yes"|"no"] (Default: no). This will delete the image after artifacts are released.
# $4=cim image tag (Optional)
set -e
if [ $# -lt 1 ];
then
echo "Too few arguments passed; atleast one required- FMaaS image tag"
exit 1
fi

BUILDER_NAME="fault-service-builder"
BUILDER_VERSION="v0.1"

MICROSERVICE_NAME="fault-service"
MICROSERVICE_VERSION=$1

ARTIFACTS_PATH="./artifacts"
BUILDER_ARG=""

CLEAN_BUILD_BUILDER_IMAGE=$2
DELETE_BUILDER_IMAGE=$3
CIM_VERSION=$4

mkdir -p $ARTIFACTS_PATH/charts
mkdir -p $ARTIFACTS_PATH/images

if [[ -n "$CLEAN_BUILD_BUILDER_IMAGE" ]] && [[ "$CLEAN_BUILD_BUILDER_IMAGE" == "yes" ]]; then
echo -e "\e[1;32;40m[FMAAS-BUILD] Clean Build Builder Image...\e[0m"
BUILDER_ARG="--no-cache"
fi

echo -e "\e[1;32;40m[FMAAS-BUILD] Build: $BUILDER_NAME, Version:$BUILDER_VERSION \e[0m"
docker build --rm $BUILDER_ARG -f ./build/fault-service-builder-dockerfile -t $BUILDER_NAME:$BUILDER_VERSION .

##NANO SEC timestamp
BUILDER_LABEL="fault-service-builder-$(date +%s%9N)"
echo -e "\e[1;32;40m[FMAAS-BUILD] Build MICROSERVICE_NAME:$MICROSERVICE_NAME, Version:$MICROSERVICE_VERSION \e[0m"
docker build --rm --build-arg BUILDER_LABEL=$BUILDER_LABEL -f ./build/fault-service-dockerfile -t $MICROSERVICE_NAME:$MICROSERVICE_VERSION .

echo -e "\e[1;32;40m[FMAAS-BUILD] Releasing artifacts \e[0m"
docker save $MICROSERVICE_NAME:$MICROSERVICE_VERSION | gzip > $ARTIFACTS_PATH/images/$MICROSERVICE_NAME-$MICROSERVICE_VERSION.tar.gz

echo -e "\e[1;32;40m[FMAAS-BUILD] Upating fault-service chart \e[0m"
cp -r ./charts/fault-service $ARTIFACTS_PATH/charts/.
sed -i "s/fmaas_tag/$1/" $ARTIFACTS_PATH/charts/fault-service/values.yaml
#sed -i "s/cim_tag/$4/" $ARTIFACTS_PATH/charts/fault-service/values.yaml
md5sum $ARTIFACTS_PATH/images/*

echo -e "\e[1;32;40m[FMAAS-BUILD] Deleting intermediate and microservice images \e[0m"
docker image prune -f --filter "label=IMAGE-TYPE=$BUILDER_LABEL"
docker rmi -f $MICROSERVICE_NAME:$MICROSERVICE_VERSION

if [[ -n "$DELETE_BUILDER_IMAGE" ]] && [[ "$DELETE_BUILDER_IMAGE" == "yes" ]]; then
echo -e "\e[1;32;40m[FMAAS-BUILD] Deleting builder image \e[0m"
docker rmi -f $BUILDER_NAME:$BUILDER_VERSION
fi
