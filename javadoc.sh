#!/bin/bash

git config --local user.email "action@github.com"
git config --local user.name "GitHub Actions"
git fetch origin gh-pages

git switch -f gh-pages

version=$(cat version.txt)

for dir in ./*
do
  if [ "$dir" == "./generated" ]; then
    continue
  fi

  rm -rf "$dir"
done

cp -Rfv ./generated/* ./
rm -rf ./generated

echo "nms.teaminceptus.us" > CNAME

git add .
git commit -m "Update JavaDocs ($1)"
git push -f origin gh-pages