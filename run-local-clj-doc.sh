#!/bin/bash
docker run --rm --publish 8000:8000 --volume ./.cljdoc-preview:/app/data --platform linux/amd64 cljdoc/cljdoc