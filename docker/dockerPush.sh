#!/bin/bash
cd docker
echo $DOCKER_PASS | docker login docker.pkg.github.com -u $DOCKER_USER --password-stdin
export TAG=`if [ "$TRAVIS_BRANCH" == "master" ]; then
  echo "latest";
else
  echo $TRAVIS_BRANCH;
fi`
export IMAGE_NAME=docker.pkg.github.com/redasurc/tsmanager/ts-manager
export IMAGE_VERSION=`../gradlew properties | grep ^version: | sed -e 's/version: //g'`
docker build -t $IMAGE_NAME:$TRAVIS_COMMIT .
docker tag $IMAGE_NAME:$TRAVIS_COMMIT $IMAGE_NAME:$TAG
echo "$IMAGE_NAME - $TAG - $IMAGE_VERSION"
if [ "$TRAVIS_BRANCH" == "master" ]; then
  docker tag $IMAGE_NAME:$TRAVIS_COMMIT $IMAGE_NAME:$IMAGE_VERSION;
fi
docker push $IMAGE_NAME
ls