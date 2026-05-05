bash ```


jextract --output ../java -t os_p2.native_c -l filededup filededup.h
gcc -Wall -Wextra -pedantic -std=gnu11 -O3 -fPIC -shared filededup.c -o libfilededup.so
./gradlew clean build
java -Djava.library.path=../../../libs -jar Deduplicator.jar
zip -j submission.zip app/build/libs/Deduplicator.jar report/report.pdf app/build.gradle.kts && zip -j -r submission.zip app/src/

```