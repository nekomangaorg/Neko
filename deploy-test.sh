mkdir $HOME/buildApk/
mkdir $HOME/android/

cp -R app/build/outputs/apk/app-debug.apk $HOME/android/
cd $HOME
git config --global user.email "<>"
git config --global user.name "CE" 

git clone --quiet --branch=master  https://CarlosEsco:$GITHUB_API_KEY@github.com/MangaDex/MangaDex-Debug  master > /dev/null
cd master cp -Rf $HOME/android/* .

git add -f .
git remote rm origin
git remote add origin https://CarlosEsco:$GITHUB_API_KEY@github.com/CarlosEsco/MangaDex.git
git add -f .
git commit -m "Travis build $TRAVIS_BUILD_NUMBER pushed [skip ci]"
git push -fq origin master > /dev/null
echo "Done"
