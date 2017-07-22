#!/bin/bash

THEME_FILE=Download/jquery-ui-1.12.1.custom.zip
THEME_ORG=tmp
THEME_OUT_DIR=META-INF/resources/primefaces-arachne
THEME_OUT_IMAGES=$THEME_OUT_DIR/images

mkdir -v tmp
unzip $THEME_FILE -d tmp
mkdir -pv $THEME_OUT_IMAGES
THEME_CSS=$( find -name jquery-ui-*.custom.css )

if [ -z "$THEME_CSS" ]; then
    echo There\'s no theme.css
    exit 1
fi

echo Editing $THEME_CSS...
cat $THEME_CSS | sed -e 's/url("images\(.*\)\.png/url("#{resource['\''primefaces-arachne:images\1.png'\'']}/g' > $THEME_OUT_DIR/theme.css
find $THEME_ORG -name *png -exec cp -v {} $THEME_OUT_IMAGES \;

echo Creating jar...
jar -cvf arachne-theme.jar META-INF


# ORG: url("#{resource['primefaces-afterdark:images/ui-icons-light.png']}");
#      url("#{resource['primefaces-mytheme:images/ui-icons_eeeeee_256x240.png']}");
#      url("#{resource[primefaces-mytheme:images1.png]}");
