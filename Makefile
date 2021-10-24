SRC = \
	com/bdjb/ExploitXlet.java \
	com/bdjb/Exploit.java \
	com/bdjb/ExploitInterface.java \
	com/bdjb/ExploitUserPrefsImpl.java \
	com/bdjb/ExploitServiceProxyImpl.java \
	com/bdjb/IxcProxyImpl.java \
	com/bdjb/ServiceInterface.java \
	com/bdjb/ServiceImpl.java \
	com/bdjb/ProviderAccessorImpl.java \
	com/bdjb/PayloadClassLoader.java \
	com/bdjb/Payload.java \
	com/bdjb/UnsafeInterface.java \
	com/bdjb/UnsafeJdkImpl.java \
	com/bdjb/UnsafeSunImpl.java \
	com/bdjb/API.java \
	com/bdjb/JIT.java \
	com/bdjb/Screen.java \

all:
	javac com/bdjb/PayloadClassLoaderSerializer.java && java com/bdjb/PayloadClassLoaderSerializer
	javac -source 1.4 -target 1.4 -bootclasspath "lib/rt.jar:lib/bdjstack.jar:lib/fakejdk.jar" $(SRC)
	jar cf disc/BDMV/JAR/00000.jar com/bdjb/*.class com/bdjb/*.ser com/bdjb/bluray.ExploitXlet.perm
	java -cp "tools/security.jar:tools/bcprov-jdk15-137.jar:tools/tools.jar" net.java.bd.tools.security.BDSigner disc/BDMV/JAR/00000.jar
	java -jar tools/bdjo.jar bdmv/bdjo.xml disc/BDMV/BDJO/00000.bdjo
	java -jar tools/MovieObject.jar bdmv/MovieObject.xml disc/BDMV/MovieObject.bdmv
	java -jar tools/index.jar bdmv/index.xml disc/BDMV/index.bdmv
	java -jar tools/id.jar bdmv/id.xml disc/CERTIFICATE/id.bdmv

clean:
	rm -rf *.class
	rm -rf com/bdjb/*.class
	rm -rf com/bdjb/*.ser
	rm -rf META-INF
