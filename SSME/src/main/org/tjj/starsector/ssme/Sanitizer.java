package org.tjj.starsector.ssme;

import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;
import org.tjj.starsector.ssme.Utils.InternalClassName;
import org.tjj.starsector.ssme.sanitizer.ClassMapping;
import org.tjj.starsector.ssme.sanitizer.PackageMapping;
import org.tjj.starsector.ssme.sanitizer.SanitizedWriter;
import org.tjj.starsector.ssme.sanitizer.SanitizerContext;

public class Sanitizer implements SanitizerContext {

	/**
	 * classes that need to be sanitised
	 */
	private final Set<String> workingSet;
	/**
	 * classes that have been processed
	 */
	private final Map<String, ClassMapping> processedClasses;
	/**
	 * for avoiding upper/lower case naming collisions.
	 * Not a problem inside the VM, but writing out the classes to a filesystem that cannot distinguish between the two is a problem. 
	 */
	private final Set<String> processedLowercaseClassNames;
	
	private final PackageMapping rootPackage = new PackageMapping("", false);

	private int sanitisedPackageCount = 0;
	private int sanitisedClassCount = 0;
	private int sanitisedEnumCount = 0;
	private int sanitisedInterfaceCount = 0;
	private int sanitisedInterfaceMethodCount = 0;

	private final ClassProvider pool;
	
	private final ExecutorService executor = new ScheduledThreadPoolExecutor(Runtime.getRuntime().availableProcessors());
	
	private final SanitizedWriter writer;

	Sanitizer(final ClassProvider pool, final boolean writeClasses, String... obfuscatedJars) throws IOException, InterruptedException, ExecutionException {

		long startTime = System.nanoTime();
		
		this.pool = pool;
		this.workingSet = new HashSet<>();
		processedClasses = new HashMap<>();
		processedLowercaseClassNames = new HashSet<>();

		if (writeClasses) {
			writer = new SanitizedWriter("deobfuscated", executor);
		} else {
			writer = null;
		}
		
		for (final String jarName : obfuscatedJars) {

			try (JarFile jar = new JarFile(jarName)) {

				Enumeration<JarEntry> entries = jar.entries();

				while (entries.hasMoreElements()) {
					JarEntry entry = entries.nextElement();

					if (!entry.isDirectory()) {
						String filename = entry.getName();
						if (filename.endsWith(".class")) {
							final String classname = filename.substring(0, filename.length() - 6);

							if (!workingSet.add(classname)) {
								System.out.println("duplicate class: " + classname);
							} else {
								try (InputStream is = jar.getInputStream(entry)) {

									ClassNode classNode = new ClassNode();
									ClassReader cr = new ClassReader(is);
									cr.accept(classNode, 0);
									processedClasses.put(classname, new ClassMapping(this, classNode));
								}
							}
						}
					}
				}
			}
		}		
		
		
		// multithreaded zip decompression experiment.

//		final int threads = Runtime.getRuntime().availableProcessors();
//
//		List<Future<Boolean>> jobFutures = new ArrayList<>();
//		
//		final List<Future<List<SimpleEntry<String, ClassMapping>>>> futures = new ArrayList<>();
//
//		for (final String jarName : obfuscatedJars) {
//
//			jobFutures.add(executor.submit(new Callable<Boolean>() {
//
//				@Override
//				public Boolean call() throws IOException {
//					byte[] jarBytes = Files.readAllBytes(new File(jarName).toPath());
//
//					noMoreEntries: for (int i = 0; i < threads; i++) {
//
//						final JarInputStream jis = new JarInputStream(new ByteArrayInputStream(jarBytes));
//						JarEntry entry = null;
//						for (int j = 0; j <= i; j++) {
//							entry = jis.getNextJarEntry();
//							if (entry == null) {
//								break noMoreEntries;
//							}
//						}
//
//						final JarEntry firstEntry = entry;
//
//						futures.add(executor.submit(new Callable<List<SimpleEntry<String, ClassMapping>>>() {
//							@Override
//							public List<SimpleEntry<String, ClassMapping>> call() throws IOException {
//								List<SimpleEntry<String, ClassMapping>> loadedClasses = new ArrayList<>();
//
//								JarEntry entry = firstEntry;
//
//								complete: while (true) {
//
//									if (!entry.isDirectory()) {
//										String filename = entry.getName();
//										if (filename.endsWith(".class")) {
//											final String classname = filename.substring(0, filename.length() - 6);
//
//											ClassNode classNode = new ClassNode();
//											ClassReader cr = new ClassReader(jis);
//											cr.accept(classNode, 0);
//
//											loadedClasses.add(new SimpleEntry<>(classname, new ClassMapping(Sanitizer.this, classNode)));
//										}
//									}
//									
//									for (int i = 0; i < threads; i++) {
//										entry = jis.getNextJarEntry();
//										if (entry == null) {
//											break complete;
//										}
//									}
//									
//								}
//								return loadedClasses;
//							}
//						}));
//
//					}
//
//					return Boolean.TRUE;
//				}
//			}));
//
//		}
//		
//		for (Future<Boolean> future : jobFutures) {
//			future.get();
//		}
//
//		for (Future<List<SimpleEntry<String, ClassMapping>>> future : futures) {
//			List<SimpleEntry<String, ClassMapping>> mappings = future.get();
//			for (SimpleEntry<String, ClassMapping> mapping : mappings) {
//
//				final String classname = mapping.getKey();
//				final ClassMapping cm = mapping.getValue();
//
//				if (!workingSet.add(classname)) {
//					System.out.println("duplicate class: " + classname);
//				} else {
//					processedClasses.put(classname, cm);
//				}
//			}
//		}

		System.out.println("new Sanitizer took: " + (System.nanoTime()-startTime)/1000000 + "ms");
		
	}

	/**
	 * retrieve the list of class name mappings that the sanitiser has created.
	 * @return
	 */
	Map<String,String> copyClassnameMappings() {

		Map<String,String> mapping = new HashMap<>();
		
		for (String key : workingSet) {
			mapping.put(InternalClassName.toBinaryName(key), InternalClassName.toBinaryName(processedClasses.get(key).getNewName()));
		}
		
		return mapping;
	}
	Sanitizer apply() throws ClassAlreadyLoadedException, IOException, ClassNotFoundException, InterruptedException, ExecutionException {
		final long start = System.nanoTime();

		
		for (String obfName : workingSet) {
			
			ClassMapping cm = get(obfName);
			
			ClassWriter cw = new ClassWriter(0);
			
			cm.classNode.accept(new SanitizingVisitor(Sanitizer.this, cw));
			final byte [] bytes = cw.toByteArray();
			pool.saveTransformation(InternalClassName.toBinaryName(cm.getNewName()), bytes);
			
			if(writer!=null) {
				writer.addClass(cm.getNewName(), bytes);
			}
		}

		if(writer!=null) {
			writer.waitUntilComplete();
		}
		
		executor.shutdown();
		
		final long end = System.nanoTime();
		System.out.println("Sanitiser.apply completed in " + (end-start)/1000000 + "ms");
		return this;
	}
	
	/**
	 * Attempts to register the given output name.
	 * 
	 * @return boolean indicating whether the given name could be registered as unique. If not, calling code should choose a new name or Error.
	 * 
	 */
	@Override
	public boolean registerOutputName(String name) {
		final String outputName = name.toLowerCase(Locale.ROOT);
		return processedLowercaseClassNames.add(outputName);
	}
	
	/**
	 * TODO special case "access$0" compiler generated synthetic accessor methods.
	 * DONE this$0 synthetic fields, and Class$0 anonymous classes.
	 * At the moment they're being deobfuscated due to the '$' symbol.
	 * 
	 * @param obfName
	 * @param pool
	 * @return
	 * @throws ClassNotFoundException
	 * @throws IOException
	 */
	@Override
	public ClassMapping get(String classname) {
		ClassMapping cm = processedClasses.get(classname);
		if(cm==null) {
			try {
				cm = new ClassMapping(this, load(classname));
				processedClasses.put(classname, cm);
			} catch (ClassNotFoundException | IOException e) {
				e.printStackTrace();
			}
		}
		return cm;
	}
	
	public ClassMapping getFromWorkingSet(String classname) {
		if(workingSet.contains(classname)) {
			return get(classname);
		}
		return null;
	}

	@Override
	public ClassNode load(String classname) throws ClassNotFoundException, IOException {
		ClassReader cr = new ClassReader(pool.getClass(InternalClassName.toBinaryName(classname)));
		
		ClassNode classNode = new ClassNode();
		cr.accept(classNode, 0);
		
		return classNode;
		
	}

	@Override
	public int incrementInterfaceMethodCount() {
		return sanitisedInterfaceMethodCount++;
	}

	@Override
	public int incrementInterfaceCount() {
		return sanitisedInterfaceCount++;
	}

	@Override
	public int incrementEnumCount() {
		return sanitisedEnumCount++;
	}

	@Override
	public int incrementClassCount() {
		return sanitisedClassCount++;
	}

	@Override
	public int incrementPackageCount() {
		return sanitisedPackageCount++;
	}
	
	
	@Override
	public boolean inWorkingSet(String classname) {
		return workingSet.contains(classname);
	}

	@Override
	public PackageMapping getRootPackage() {
		return rootPackage;
	}

	
}
