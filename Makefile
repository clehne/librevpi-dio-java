SHELL=/bin/bash
CXX=g++
NAME:=librevpi-dio-java

## Recognize OS in order to allow some make-targets to also run
##      on MacOS:
UNAME_S := $(shell uname -s)
        ifeq ($(UNAME_S),Darwin)
                JAVA_HOME=/usr
                SKIPJAVAHOME=true
        endif

### JAVA_HOME
ifndef SKIPJAVAHOME
        JAVA_HOME=$(shell readlink -f /usr/bin/javac | sed "s:bin/javac::")
endif
ifeq ($(OS),Windows_NT)
       JAVA_HOME="/cygdrive/c/Program Files/Java/jdk1.8.0_121/"
endif

JAVA_INCLUDES=-I$(JAVA_HOME)/include/linux -I$(JAVA_HOME)/include
JAVA=$(JAVA_HOME)/bin/java
JAVAC=$(JAVA_HOME)/bin/javac
JAR=$(JAVA_HOME)/bin/jar
JAVA_SRC:=$(shell find src -type f -and -name '*.java')
JAVA_TEST_SRC:=$(shell find src.test -type f -and -name '*.java')
#JNI_SRC:=$(shell find jni -type f -and -regex '^.*\.\(c\|cpp\|h\)$$')
JNI_SRC:=$(shell find jni/src -type f -and -regex '^.*\.\(c\|cpp\)')
JAVA_DEST=classes
JAVA_TEST_DEST=classes.test
LIB_DEST=lib
JAR_DEST=dist
JAR_DEST_FILE=$(JAR_DEST)/$(NAME).jar
JAR_MANIFEST_FILE=META-INF/MANIFEST.MF
DIRS=stamps obj $(JAVA_DEST) $(JAVA_TEST_DEST) $(LIB_DEST) $(JAR_DEST)
JNI_DIR=jni/generated
JNI_CLASSES=org.clehne.revpi.dataio.DataInOut
JAVAC_FLAGS=-g -Xlint:all
CXXFLAGS=-I./include -I./include/picontrol -I./jni/src -I./jni/src/picontrol -I./jni/generated\
 -O2 -g -pipe -Wall -Wp,-D_FORTIFY_SOURCE=2 -fexceptions \
 -fstack-protector --param=ssp-buffer-size=4 -fPIC -Wno-unused-parameter \
 -pedantic -D_REENTRANT -D_GNU_SOURCE $(JAVA_INCLUDES) 
SONAME=jni_revpi_dio
LDFLAGS=-Wl,-soname,$(SONAME)
REMOTE=169.254.23.187#192.168.29.250#
REMOTEDIR=/home/awaal/revpi-dio-java

.DEFAULT_GOAL := all
.LIBPATTERNS :=
.SUFFIXES:

.PHONY: all
all: stamps/create-jar stamps/compile-test

.PHONY: clean
clean:
		$(RM) -r $(DIRS) $(STAMPS) $(filter %.h,$(JNI_SRC))
		$(RM) -r $(JNI_DIR)/*

stamps/dirs:
		mkdir $(DIRS)
		@touch $@

stamps/compile-src: stamps/dirs $(JAVA_SRC)
		$(JAVAC) $(JAVAC_FLAGS) -d $(JAVA_DEST) $(sort $(JAVA_SRC))
		@touch $@

stamps/compile-test: stamps/compile-src $(JAVA_TEST_SRC)
		$(JAVAC) $(JAVAC_FLAGS) -cp $(JAVA_DEST) -d $(JAVA_TEST_DEST) \
                $(sort $(JAVA_TEST_SRC))
		@touch $@

stamps/generate-jni-h: stamps/compile-src
		$(JAVAC) -h $(JNI_DIR) -classpath ./src/org/clehne/revpi/dataio -sourcepath src src/org/clehne/revpi/dataio/DataInOut.java
		@touch $@

stamps/compile-jni: stamps/generate-jni-h $(JNI_SRC)
		$(CXX) $(CXXFLAGS) $(LDFLAGS) -shared -o $(LIB_DEST)/lib$(SONAME).so \
                $(sort $(filter %.cpp,$(JNI_SRC)) $(filter %.c,$(JNI_SRC)))
		@touch $@
        
stamps/create-jar: stamps/compile-jni $(JAR_MANIFEST_FILE)
		$(JAR) cMf $(JAR_DEST_FILE) $(JAR_MANIFEST_FILE) lib -C $(JAVA_DEST) .
		@touch $@

stamps/pi-xfer: clean
        ## This target transfers the source files to the remote
        ## After this, it is possible to do a remote make via
        ## the target pi-build
        ## Pre-req: make sure the local key-pair is also
        ## allowed on the pi:
        ## on local machine (in ~/.ssh): ssh-keygen -t rsa
        ## cat id_rsa.pub | ssh <user>@$(REMOTE) ' cat >>.ssh/authorized_keys'
        ## ssh-keyscan $(REMOTE) >> ~/.ssh/known_hosts
		ssh $(REMOTE) 'mkdir -p '$(REMOTEDIR)''
		scp -r ./* $(REMOTE):$(REMOTEDIR)

stamps/pi-build: stamps/pi-xfer
		ssh $(REMOTE) 'make clean --directory=$(REMOTEDIR)'
		ssh $(REMOTE) 'make --directory=$(REMOTEDIR)'

stamps/pi2local-xfer: stamps/pi-build
        ## This target takes the native library (.so) and the
        ## JAVA classes (creates a jar) and transfers them
        ## to the local filesystem
        ## Prepare a local folder:
		mkdir -p lib
        ## Transfer the native library to the local project:
		scp $(REMOTE):$(REMOTEDIR)/$(LIB_DEST)/lib$(SONAME).so $(LIB_DEST)/lib$(SONAME).so
        ## Transfer the native library to the local JAVA project that uses it:

.PHONY: check
check: stamps/create-jar stamps/compile-test
		$(JAVA) -ea -cp $(JAR_DEST_FILE):$(JAVA_TEST_DEST) \
                -Xcheck:jni \
                org.clehne.revpi.dataio.DataInOutTest
