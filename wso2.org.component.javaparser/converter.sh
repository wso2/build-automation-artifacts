converted_repo(){
	directory_string=$(find . -type d | grep "internal")
	pom_string=$(find . -type f | grep "pom.xml")
        touch component.xml
	FILE_NAME=component.xml
	truncate -s 0 $FILE_NAME
	echo -n "<?xml version=" >> $FILE_NAME
	echo -n '"1.0"' >> $FILE_NAME
	echo -n " encoding=" >> $FILE_NAME
	echo -n  '"UTF-8"' >> $FILE_NAME
	echo -n  "?>" >> $FILE_NAME
	echo "" >> $FILE_NAME
	echo "<Components>" >> $FILE_NAME
	echo -n "<componentPath name=" >> $FILE_NAME
	echo -n '"pathNames"' >> $FILE_NAME
	echo -n ">" >> $FILE_NAME
	echo "" >> $FILE_NAME
	var1=1
	for directory_name in $directory_string
	do
	    dn="";
	    echo "" >> $FILE_NAME
	    echo -n "<Path>" >> $FILE_NAME
	    dn=$( echo $directory_name | cut -c2-)
	    FULL_PATH=$PWD$dn
	    echo -n $FULL_PATH >> $FILE_NAME 
	    echo -n "</Path>" >> $FILE_NAME
	    echo "" >> $FILE_NAME
	    var1=$((var1+1))
	done
	var2=1
	for pom_name in $pom_string
	do
	    dn="";
	    echo "" >> $FILE_NAME
	    echo -n "<POMPath>" >> $FILE_NAME
	    dn=$( echo $pom_name | cut -c2-)
	    FULL_PATH=$PWD$dn
	    echo -n $FULL_PATH >> $FILE_NAME 
	    echo -n "</POMPath>" >> $FILE_NAME
	    echo "" >> $FILE_NAME
	    var2=$((var2+1))
	done
	echo -n "" >> $FILE_NAME
	echo "</componentPath>" >> $FILE_NAME
	echo "</Components>" >> $FILE_NAME
	COMPONENT_PATH=$PWD"/"$FILE_NAME

java -cp wso2.org.component.javaparser-1.0.0-SNAPSHOT-jar-with-dependencies.jar component.javaparser.App $COMPONENT_PATH
	#rm  $FILE_NAME wso2.org.component.javaparser-1.0.0-SNAPSHOT-jar-with-dependencies.jar
}


validate_result(){
target_string=$(find . -type d | grep "OLD_XML")
touch target.xml
TARGET_FILE=target.xml
truncate -s 0 $TARGET_FILE
echo -n "<?xml version=" >> $TARGET_FILE
	echo -n '"1.0"' >> $TARGET_FILE
	echo -n " encoding=" >> $TARGET_FILE
	echo -n  '"UTF-8"' >> $TARGET_FILE
	echo -n  "?>" >> $TARGET_FILE
	echo "" >> $TARGET_FILE
	echo "<Targets>" >> $TARGET_FILE
	echo -n "<targetPath name=" >> $TARGET_FILE
	echo -n '"targetNames"' >> $TARGET_FILE
	echo -n ">" >> $TARGET_FILE
	echo "" >> $TARGET_FILE
	var1=1
	for target_name in $target_string
	do
	    dn="";
	    echo "" >> $TARGET_FILE
	    echo -n "<Path>" >> $TARGET_FILE
	    dn=$( echo $target_name | cut -c2-)
	    FULL_PATH=$PWD$dn
	    echo -n $FULL_PATH >> $TARGET_FILE
	    echo -n "</Path>" >> $TARGET_FILE
	    echo "" >> $TARGET_FILE
	    var1=$((var1+1))
	done
	echo -n "" >> $TARGET_FILE
	echo "</targetPath>" >> $TARGET_FILE
	echo "</Targets>" >> $TARGET_FILE
	TARGET_PATH=$PWD"/"$TARGET_FILE
java -cp wso2.org.component.javaparser-1.0.0-SNAPSHOT-jar-with-dependencies.jar component.javaparser.App $TARGET_PATH
}

find_oldjar(){
tmp=/tmp
PARENT_DIR=$PWD$tmp
echo $PARENT_DIR
cd old
for JAR_FILE in $(find *.jar)
do
 jar xf $JAR_FILE OSGI-INF
 cd OSGI-INF
 files=*.xml
 if [ ${#files[@]} -gt 0 ]; 
 then
        mkdir $JAR_FILE
	mv *.xml $JAR_FILE
 fi
 cd ..
done
mv OSGI-INF OLD_XML
mv OLD_XML $PARENT_DIR
cd ..
}


find_newjar(){
tmp=/tmp
echo $tmp
PARENT_DIR=$PWD$tmp
OLD_JAR=/OLD_XML/
cd new
for JAR_FILES in $(find *.jar)
do
 jar xf $JAR_FILES OSGI-INF
 cd OSGI-INF
 mv *.xml $PARENT_DIR$OLD_JAR$JAR_FILES
 cd ..
done
cd ..
}

create_dir(){
mkdir tmp
mkdir old
mkdir new
}

remove_dir(){
rm -r new
rm -r old
rm -r tmp
}

remove_files(){
rm target.xml
rm component.xml
}

commit(){
echo "commit to git"
}


build_and_convert() {

REPO=$1

git clone --depth 1 https://github.com/$REPO.git


JAVA_PARSER=wso2.org.component.javaparser-1.0.0-SNAPSHOT-jar-with-dependencies.jar

LOCAL_REPO=$(ls -td -- */ | head -n 1)



repo_result=$(find . -name repo_result.txt)
if [ "$repo_result" != "./repo_result.txt" ]; then
    touch repo_result.txt
    repo_result=repo_result.txt
fi



cp $JAVA_PARSER $LOCAL_REPO
cd $LOCAL_REPO


echo "*** Building to find initial scr components.xml"
mvn clean install -Dmaven.test.skip=true
create_dir
cp `find components/ -name "*.jar" | xargs grep OSGI-INF` old
find_oldjar
mvn clean

echo "************************************Converting the scr doclets with scr annotations********************************************"
converted_repo

echo "*** Building the repo with new scr annotations"
mvn clean install -Dmaven.test.skip=true
cp `find components/ -name "*.jar" | xargs grep OSGI-INF` new
find_newjar

echo "*** Validating..."
validate_result
remove_dir
remove_files

result_file=$(find . -name result.txt)
repo_converted="BUILD FAILURE : $REPO"

if [ "$result_file" = "./result.txt" ]
then
    result=$(cat result.txt)
    if [ "$result" = "success" ]
    then
    repo_converted="Repository $REPO converted : Successful"
    echo $repo_converted
    $(commit)
    fi
else
    repo_converted="Repository $REPO converted : Unsuccessful"
    echo $repo_converted
fi


rm $JAVA_PARSER
cd ..

echo "\n" >> $repo_result
echo $repo_converted >> $repo_result
}




# wso2/product-is wso2-extensions/identity-inbound-auth-oauth
REPOS=$*
for repo in $REPOS
do
 if echo "$repo" | grep -q "/"; then
 build_and_convert $repo
 else
 echo "The Repository name is incorrect. It should look like: wso2/product-is"
 fi
done






