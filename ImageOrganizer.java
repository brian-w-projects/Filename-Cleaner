import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.stream.*;

public class ImageOrganizer {
	private Path folder;
	private boolean verbose;
	private boolean inOrder;
	private boolean simulate;
	private String extension;
	
	private ImageOrganizer(Path folder, boolean verbose, boolean inOrder, boolean simulate, String extension){
		this.folder = folder;
		this.verbose = verbose;
		this.inOrder = inOrder;
		this.simulate = simulate;
		this.extension = extension;
	}
	
	public static void main(String[] args){
		ImageOrganizer organizer = new ImageOrganizer.Builder().folder(args[0]).verbose(false).inOrder(true).simulate(true).build();
		try{
			organizer.renameFolder();
		}catch(IOException e){
			System.out.println(e.getMessage());
			System.exit(1);
		}
	}
	
	public void renameFolder() throws IOException{
		String folderName = folder.getFileName().toString();
		Path tempDirectory = Files.createTempDirectory(folderName);
		AtomicLong fileNameCounter = new AtomicLong(0);
	
		Stream<Path> stream = null;
		if(inOrder){
			List<Path> data = Files
					.list(folder)
					.collect(Collectors.toList());
			Collections.sort(data, (a, b) -> timeCompare(a, b));
			stream = data.stream();
		}else{
			stream = Files.list(folder).parallel();
		}
		
		System.out.println("STARTING: " + folderName);
		streamPipeline(stream, p -> processFile(p, tempDirectory.resolve(folderName + "_" + fileNameCounter.getAndIncrement() + fileExtensionGenerator(p))));
		streamPipeline(Files.list(tempDirectory).parallel(), p -> processFile(p, folder.resolve(p.getFileName())));
		System.out.println("FINISHED");
	}
	
	private void streamPipeline(Stream<Path> stream, Consumer<Path> consumer){
		stream
			.filter(Files::isRegularFile)
			.filter(p -> extension == null || fileExtensionGenerator(p).endsWith(extension))
			.forEach(consumer);
	}
	
	private void processFile(Path file, Path newName){
		try{
			if(simulate){
				System.out.println("SIMULATING: " + file.getFileName() + " TO " + newName.getFileName());
			}else{
				if(verbose && !file.getFileName().equals(newName.getFileName())){
					System.out.println("PROCESSING: " + file.getFileName() + " TO " + newName.getFileName());
				}
				Files.copy(file, newName);
				Files.delete(file);
			}
		}catch(IOException e){
			System.out.println(e.getMessage());
			System.exit(1);
		}
	}

	private static int timeCompare(Path a, Path b){
		int toRet = 0;
		try{
			toRet = Files.readAttributes(a, BasicFileAttributes.class).creationTime().compareTo(Files.readAttributes(b, BasicFileAttributes.class).creationTime());
		}catch(IOException e){
			System.out.println(e.getMessage());
			System.exit(1);
		}
		return(toRet);
	}
	
	public String fileExtensionGenerator(Path p){
		String s = p.toString();
		return("." + s.substring(s.lastIndexOf('.')+1));
	}

	public static class Builder{
		private Path folder;
		private boolean verbose = true;
		private boolean inOrder = false;
		private boolean simulate = false;
		private String extension = null;
		
		public Builder verbose(boolean verbose){
			this.verbose = verbose;
			return this;
		}
		
		public Builder inOrder(boolean inOrder){
			this.inOrder = inOrder;
			return this;
		}
		
		public Builder simulate(boolean simulate){
			this.simulate = simulate;
			return this;
		}
		
		public Builder folder(String path){
			try{
				Path folder = Paths.get(path).toRealPath();
				if(!Files.isDirectory(folder))
					throw new IllegalArgumentException("Directory does not exist.");
				this.folder = folder;
			}catch(IOException e){
				System.out.println("IO Exception");
				System.exit(1);
			}
			return this;
		}
		
		public Builder extension(String ext){
			this.extension = ext;
			return this;
		}
		
		public ImageOrganizer build(){
			if(folder == null){
				throw new IllegalArgumentException("Folder must be specified.");
			}
			return new ImageOrganizer(folder, verbose, inOrder, simulate, extension);
		}
	}
}