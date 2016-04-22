package org.tjj.starsector.ssme.sanitizer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import org.tjj.starsector.ssme.Utils;

public class SanitizedWriter {

	private ExecutorService executor;
	private final File root;
	
	private final Future<Boolean> initialClean;
	private final List<Future<Boolean>> classWrites = new ArrayList<>();
	
	
	public SanitizedWriter(String folderName, ExecutorService executor) throws IOException {
		this.root = new File(folderName);
		
		this.executor = executor; 

		initialClean = executor.submit(new Callable<Boolean>() {
			public Boolean call() throws IOException {
				long start = System.nanoTime();
				if (root.exists()) {
					Utils.removeRecursive(root.toPath());
					if (root.exists()) {
						throw new IOException("Failed deleting " + root);
					}
				}
				System.out.println("initialClean duration: " + (System.nanoTime()-start)/1000000 + "ms");
				return Boolean.TRUE;
				
			}
		});
	}
				
				
	public void addClass(final String className, final byte[] b) {
		
		Future<Boolean> result = executor.submit(new Callable<Boolean>() {
		
			public Boolean call() throws IOException, InterruptedException, ExecutionException {
				
				//make sure the initial clean job has completed.
				initialClean.get();
				
				File f = new File(root, className + ".class");
				if (f.exists()) {
					throw new IOException("Cannot write: " + f + ", already exists!");
				}
				f.getParentFile().mkdirs();

				try (FileOutputStream fos = new FileOutputStream(f)) {
					fos.write(b);
				}
				return Boolean.TRUE;
				
			}
		});
		classWrites.add(result);
		
	}
	
	public void waitUntilComplete() throws IOException, InterruptedException, ExecutionException {
		long waitStarted = System.nanoTime();
		for (Future<Boolean> future : classWrites) {
			future.get();
		}
		
		System.out.println("Writing classes added " + (System.nanoTime()-waitStarted)/1000000 + "ms");
		
	}
}
