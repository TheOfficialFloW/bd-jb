BUILD = build
BDMV = bdmv
DISC = disc
LIB = lib
SRC = src
TOOLS = tools

LOADER_CLASSES = \
	$(SRC)/com/bdjb/LoaderXlet.java \
	$(SRC)/com/bdjb/Loader.java \
	$(SRC)/com/bdjb/Screen.java \

EXPLOIT_CLASSES = \
	$(SRC)/com/bdjb/Exploit.java \
	$(SRC)/com/bdjb/Screen.java \
	$(SRC)/com/bdjb/api/API.java \
	$(SRC)/com/bdjb/api/KernelAPI.java \
	$(SRC)/com/bdjb/api/Buffer.java \
	$(SRC)/com/bdjb/api/Text.java \
	$(SRC)/com/bdjb/api/Int8.java \
	$(SRC)/com/bdjb/api/Int16.java \
	$(SRC)/com/bdjb/api/Int32.java \
	$(SRC)/com/bdjb/api/Int64.java \
	$(SRC)/com/bdjb/api/UnsafeInterface.java \
	$(SRC)/com/bdjb/api/UnsafeJdkImpl.java \
	$(SRC)/com/bdjb/api/UnsafeSunImpl.java \
	$(SRC)/com/bdjb/jit/AbstractJit.java \
	$(SRC)/com/bdjb/jit/JitDefaultImpl.java \
	$(SRC)/com/bdjb/jit/JitCompilerReceiverImpl.java \
	$(SRC)/com/bdjb/exploit/sandbox/ExploitSandboxInterface.java \
	$(SRC)/com/bdjb/exploit/sandbox/ExploitDefaultImpl.java \
	$(SRC)/com/bdjb/exploit/sandbox/ExploitServiceProxyImpl.java \
	$(SRC)/com/bdjb/exploit/sandbox/IxcProxyImpl.java \
	$(SRC)/com/bdjb/exploit/sandbox/ServiceInterface.java \
	$(SRC)/com/bdjb/exploit/sandbox/ServiceImpl.java \
	$(SRC)/com/bdjb/exploit/sandbox/ProviderAccessorImpl.java \
	$(SRC)/com/bdjb/exploit/sandbox/Payload.java \
	$(SRC)/com/bdjb/exploit/kernel/ExploitKernelInterface.java \

JFLAGS = -Xlint:all -Xlint:-options -source 1.4 -target 1.4 -bootclasspath "$(LIB)/rt.jar:$(LIB)/bdjstack.jar"

all: loader exploit

loader: build_directory loader_classes loader_jar loader_bdjo_bdmv

exploit: build_directory exploit_classes exploit_jar

build_directory:
	mkdir -p $(BUILD)

loader_classes:
	javac -d $(BUILD) -sourcepath $(SRC) $(JFLAGS) $(LOADER_CLASSES)

exploit_classes:
	javac -d $(BUILD) -sourcepath $(SRC) $(JFLAGS) $(EXPLOIT_CLASSES)

loader_jar:
	mkdir -p  $(DISC)/BDMV/JAR
	cp $(SRC)/com/bdjb/bluray.LoaderXlet.perm $(BUILD)/com/bdjb/bluray.LoaderXlet.perm
	cd $(BUILD) && jar cf ../$(DISC)/BDMV/JAR/00000.jar . && cd ..
	java -cp "$(TOOLS)/security.jar:$(TOOLS)/bcprov-jdk15-137.jar:$(TOOLS)/tools.jar" net.java.bd.tools.security.BDSigner $(DISC)/BDMV/JAR/00000.jar

exploit_jar:
	rm -rf $(BUILD)/jdk
	cd $(BUILD) && jar cf 00000.jar . && cd ..

loader_bdjo_bdmv:
	mkdir -p  $(DISC)/BDMV/BDJO
	java -jar $(TOOLS)/bdjo.jar $(BDMV)/bdjo.xml $(DISC)/BDMV/BDJO/00000.bdjo
	java -jar $(TOOLS)/MovieObject.jar $(BDMV)/MovieObject.xml $(DISC)/BDMV/MovieObject.bdmv
	java -jar $(TOOLS)/index.jar $(BDMV)/index.xml $(DISC)/BDMV/index.bdmv
	java -jar $(TOOLS)/id.jar $(BDMV)/id.xml $(DISC)/CERTIFICATE/id.bdmv

clean:
	rm -rf build
	rm -rf META-INF
