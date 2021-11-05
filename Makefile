BUILD = build
BDMV = bdmv
DISC = disc
LIB = lib
SRC = src
TOOLS = tools

CLASSES = \
	$(SRC)/com/bdjb/ExploitXlet.java \
	$(SRC)/com/bdjb/Exploit.java \
	$(SRC)/com/bdjb/JIT.java \
	$(SRC)/com/bdjb/Screen.java \
	$(SRC)/com/bdjb/api/API.java \
	$(SRC)/com/bdjb/api/UnsafeInterface.java \
	$(SRC)/com/bdjb/api/UnsafeJdkImpl.java \
	$(SRC)/com/bdjb/api/UnsafeSunImpl.java \
	$(SRC)/com/bdjb/exploit/sandbox/ExploitSandboxInterface.java \
	$(SRC)/com/bdjb/exploit/sandbox/ExploitDefaultImpl.java \
	$(SRC)/com/bdjb/exploit/sandbox/ExploitUserPrefsImpl.java \
	$(SRC)/com/bdjb/exploit/sandbox/ExploitServiceProxyImpl.java \
	$(SRC)/com/bdjb/exploit/sandbox/IxcProxyImpl.java \
	$(SRC)/com/bdjb/exploit/sandbox/ServiceInterface.java \
	$(SRC)/com/bdjb/exploit/sandbox/ServiceImpl.java \
	$(SRC)/com/bdjb/exploit/sandbox/ProviderAccessorImpl.java \
	$(SRC)/com/bdjb/exploit/sandbox/PayloadClassLoader.java \
	$(SRC)/com/bdjb/exploit/sandbox/Payload.java \
	$(SRC)/com/bdjb/exploit/kernel/ExploitKernelInterface.java \

JFLAGS = -Xlint:all -Xlint:-options -source 1.4 -target 1.4 -bootclasspath "$(LIB)/rt.jar:$(LIB)/bdjstack.jar"

all: directory serialized classes jar bdjo_bdmv

directory:
	mkdir -p $(BUILD)

serialized:
	javac -d $(BUILD) -sourcepath $(SRC) $(SRC)/com/bdjb/exploit/sandbox/PayloadClassLoaderSerializer.java
	java -cp $(BUILD) com/bdjb/exploit/sandbox/PayloadClassLoaderSerializer $(BUILD)/com/bdjb/exploit/sandbox/PayloadClassLoader.ser
	rm $(BUILD)/com/bdjb/exploit/sandbox/PayloadClassLoaderSerializer.class

classes:
	javac -d $(BUILD) -sourcepath $(SRC) $(JFLAGS) $(CLASSES)

jar:
	rm -rf $(BUILD)/jdk
	cp $(SRC)/com/bdjb/bluray.ExploitXlet.perm $(BUILD)/com/bdjb/bluray.ExploitXlet.perm
	cd $(BUILD) && jar cf ../$(DISC)/BDMV/JAR/00000.jar . && cd ..
	java -cp "$(TOOLS)/security.jar:$(TOOLS)/bcprov-jdk15-137.jar:$(TOOLS)/tools.jar" net.java.bd.tools.security.BDSigner $(DISC)/BDMV/JAR/00000.jar

bdjo_bdmv:
	java -jar $(TOOLS)/bdjo.jar $(BDMV)/bdjo.xml $(DISC)/BDMV/BDJO/00000.bdjo
	java -jar $(TOOLS)/MovieObject.jar $(BDMV)/MovieObject.xml $(DISC)/BDMV/MovieObject.bdmv
	java -jar $(TOOLS)/index.jar $(BDMV)/index.xml $(DISC)/BDMV/index.bdmv
	java -jar $(TOOLS)/id.jar $(BDMV)/id.xml $(DISC)/CERTIFICATE/id.bdmv

clean:
	rm -rf build
	rm -rf META-INF
