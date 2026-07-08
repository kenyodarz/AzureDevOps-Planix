#!/bin/sh
echo "Iniciando provisionamiento de recursos locales en Floci..."
awslocal s3 mb s3://backlog-reports
awslocal s3 mb s3://test
echo "Buckets backlog-reports y test creados con éxito en Floci!"

