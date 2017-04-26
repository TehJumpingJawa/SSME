#!/bin/bash

DIR="$( cd "$( dirname "$BASH_SOURCE[0]}" )" && pwd )"
echo $DIR  > /tmp/out.txt

cd "$DIR"
cd "../../Contents/Resources/Java"

export JAVA_HOME=../../Home
"$JAVA_HOME/bin/java" \
    -Xdock:name="Starsector" \
    -Xdock:icon=../../Resources/s_icon128.icns \
    -Dapple.laf.useScreenMenuBar=false \
    -Dcom.apple.macos.useScreenMenuBar=false \
    -Dapple.awt.showGrowBox=false \
    -Dfile.encoding=UTF-8 \
    ${EXTRAARGS} \
	-javaagent:"../../../mods/ssme/lib/${project.build.finalName}.jar" \
	-server \
	-XX:CompilerThreadPriority=1 \
	-XX:+CompilerThreadHintNoPreempt \
	-Djava.library.path=../../Resources/Java/native/macosx \
	-Dcom.fs.starfarer.settings.paths.saves=../../../saves \
	-Dcom.fs.starfarer.settings.paths.screenshots=../../../screenshots \
	-Dcom.fs.starfarer.settings.paths.mods=../../../mods \
	-Dcom.fs.starfarer.settings.paths.logs=../../../logs \
	-Dcom.fs.starfarer.settings.osx=true \
    -Xms1024m \
    -Xmx1024m \
	-cp ../../Resources/Java/AppleJavaExtensions.jar:../../Resources/Java/commons-compiler-jdk.jar:../../Resources/Java/commons-compiler.jar:../../Resources/Java/fs.common_obf.jar:../../Resources/Java/fs.sound_obf.jar:../../Resources/Java/janino.jar:../../Resources/Java/jinput.jar:../../Resources/Java/jogg-0.0.7.jar:../../Resources/Java/jorbis-0.0.15.jar:../../Resources/Java/log4j-1.2.9.jar:../../Resources/Java/lwjgl.jar:../../Resources/Java/lwjgl_util.jar:../../Resources/Java/starfarer.api.jar:../../Resources/Java/starfarer_obf.jar:../../Resources/Java/xstream-1.4.2.jar:../../Resources/Java/json.jar:"../../../mods/ssme/lib/${project.build.finalName}.jar":"../../../mods/ssme/lib/javassist-${javassist-version}.jar":"../../../mods/ssme/lib/asm-${asm-version}.jar":"../../../mods/ssme/lib/asm-analysis-${asm-version}.jar":"../../../mods/ssme/lib/asm-commons-${asm-version}.jar":"../../../mods/ssme/lib/asm-tree-${asm-version}.jar":"../../../mods/ssme/lib/asm-util-${asm-version}.jar":"../../../mods/ssme/lib/asm-xml-${asm-version}.jar":"../../../mods/ssme/lib/jProcesses-${jProcesses.version}.jar":"../../../mods/ssme/lib/WMI4Java-${WMI4Java.version}.jar":"../../../mods/ssme/lib/jPowerShell-${jPowerShell.version}.jar" \
    org.tjj.starsector.ssme.StarsectorModExpander \
    "$@" \
    2>&1

exit 0
