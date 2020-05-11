#!/bin/bash
#Sample Usage: deleteUpdateSiteFromBintray.sh version
API=https://api.bintray.com
CURRENT_PATH=$( cd "$(dirname "${BASH_SOURCE[0]}")" ; pwd -P )
BINTRAY_OWNER=opencaesar
BINTRAY_REPO=rcp
PCK_NAME=oml-rosetta
PCK_VERSION=$1
TARGET_PATH=oml-rosetta/releases
PATH_TO_REPOSITORY=${CURRENT_PATH}/target/products

function main() {
	if [ ! -d $PCK_VERSION ]; then
		undeploy_rcp
	else
		echo "Usage: ./undeploy.sh <version>"
	fi
}

function undeploy_rcp() {
	echo "${BINTRAY_USER}"
	echo "${BINTRAY_API_KEY}"
	echo "${BINTRAY_OWNER}"
	echo "${BINTRAY_REPO}"
	echo "${PCK_NAME}"
	echo "${PCK_VERSION}"
	echo "${PATH_TO_REPOSITORY}"
	
	if [ ! -z "$PATH_TO_REPOSITORY" ]; then
	   cd $PATH_TO_REPOSITORY
	   if [ $? -ne 0 ]; then
	     #directory does not exist
	     echo $PATH_TO_REPOSITORY " does not exist"
	     exit 1
	   fi
	fi
	
	
	FILES=./*.zip
	
	echo "Processing features dir $FILES file..."
	for f in $FILES;
	do
	  echo "Deleting file $f ..."
	  curl -X DELETE -u ${BINTRAY_USER}:${BINTRAY_API_KEY} https://api.bintray.com/content/${BINTRAY_OWNER}/${BINTRAY_REPO}/${TARGET_PATH}/${PCK_VERSION}/$f
	  echo ""
	done

	echo "Deleting version: ${PCK_VERSION}"
	curl -X DELETE -u ${BINTRAY_USER}:${BINTRAY_API_KEY} https://api.bintray.com/packages/${BINTRAY_OWNER}/${BINTRAY_REPO}/${PCK_NAME}/versions/${PCK_VERSION}
}

main "$@"