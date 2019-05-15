#!/usr/bin/env bash

PRODUCT_BUILD_STATUS=$(eval "curl -s https://wso2.org/jenkins/job/products/job/micro-integrator/$buildNumber/api/json | python3 -c \"import sys, json; print(json.load(sys.stdin)['result'])\"")

if [ "${PRODUCT_BUILD_STATUS}" != "SUCCESS" ]
then
    echo "Product build was not successful. Therefore not building the docker image"
    exit
fi

echo "Wait till artifacts are archived in the product build..."
sleep 30

DISTRIBUTION_NAME=$(eval "curl -s https://wso2.org/jenkins/job/products/job/micro-integrator/$buildNumber/api/json | python3 -c \"import sys, json; print(json.load(sys.stdin)['artifacts'][5]['fileName'])\"")

# Cleanup build directory
rm -fr docker-build
rm -fr micro-integrator
rm -f $DISTRIBUTION_NAME

mkdir docker-build

wget --progress=dot:mega https://wso2.org/jenkins/job/products/job/micro-integrator/$buildNumber/artifact/distribution/target/$DISTRIBUTION_NAME
git clone --depth=50 https://github.com/wso2/micro-integrator.git
cp micro-integrator/distribution/src/docker-distribution/filtered/Dockerfile docker-build
unzip -q $DISTRIBUTION_NAME -d docker-build

cd docker-build
DISTRIBUTION_ROOT=${DISTRIBUTION_NAME%.zip}
DISTRIBUTION_VERSION=${DISTRIBUTION_ROOT#wso2mi-}

mv $DISTRIBUTION_ROOT wso2mi

docker build --pull -t wso2/micro-integrator:$DISTRIBUTION_VERSION .
docker push wso2/micro-integrator:$DISTRIBUTION_VERSION

cd ..

rm -r docker-build
rm -r micro-integrator
rm $DISTRIBUTION_NAME

docker image prune -f

