#!/bin/bash
path=`md5 -qs config_com.resestudio.jewelry.jewellegend17`
java -jar ./EncryptTool.jar e default.json ../app/src/main/assets/$path
