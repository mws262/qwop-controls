Things to note:

1. Increase Java heap space to prevent OutOfMemoryError. e.g. for 10gig
export _JAVA_OPTIONS="-Xmx10g"
View settings with:
java -XshowSettings:vm 