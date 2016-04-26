cd ../..
./jre_linux/bin/java -javaagent:"./mods/ssme/lib/${project.build.finalName}.jar" -server -XX:CompilerThreadPriority=1 -XX:+CompilerThreadHintNoPreempt -Djava.library.path=./native/linux -Xms1024m -Xmx1024m -Dcom.fs.starfarer.settings.paths.saves=./saves -Dcom.fs.starfarer.settings.paths.screenshots=./screenshots -Dcom.fs.starfarer.settings.paths.mods=./mods -Dcom.fs.starfarer.settings.paths.logs=. -classpath janino.jar:commons-compiler.jar:commons-compiler-jdk.jar:starfarer.api.jar:starfarer_obf.jar:jogg-0.0.7.jar:jorbis-0.0.15.jar:lwjgl.jar:jinput.jar:log4j-1.2.9.jar:lwjgl_util.jar:fs.sound_obf.jar:fs.common_obf.jar:xstream-1.4.2.jar:json.jar:"./mods/ssme/lib/${project.build.finalName}.jar":"./mods/ssme/lib/javassist-${javassist-version}.jar":"./mods/ssme/lib/asm-${asm-version}.jar":"./mods/ssme/lib/asm-analysis-${asm-version}.jar":"./mods/ssme/lib/asm-commons-${asm-version}.jar":"./mods/ssme/lib/asm-tree-${asm-version}.jar":"./mods/ssme/lib/asm-util-${asm-version}.jar":"./mods/ssme/lib/asm-xml-${asm-version}.jar" org.tjj.starsector.ssme.StarsectorModExpander