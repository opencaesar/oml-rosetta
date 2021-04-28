#!/bin/bash

CURRENT_PATH=$( cd "$(dirname "${BASH_SOURCE[0]}")" ; pwd -P )
PCK_VERSION=$(cat ${CURRENT_PATH}/../version.txt)
PATH_TO_REPOSITORY=${CURRENT_PATH}/target/products

function main() {
	deploy_rcp
}

function deploy_rcp() {
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
	  pre="${f%%-*}"
	  post="${f##*-}"
	  file="${pre}-${PCK_VERSION}-${post}"
	  if [ $f != $file ]; then
   	     echo "Renaming $f to $file"
	     mv $f $file
	  fi
	done
}


main "$@"