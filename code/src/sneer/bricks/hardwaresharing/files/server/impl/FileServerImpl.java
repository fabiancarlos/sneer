package sneer.bricks.hardwaresharing.files.server.impl;

import static sneer.foundation.environments.Environments.my;

import java.io.File;
import java.io.IOException;

import sneer.bricks.hardware.cpu.lang.contracts.WeakContract;
import sneer.bricks.hardware.io.IO;
import sneer.bricks.hardware.io.log.Logger;
import sneer.bricks.hardware.ram.arrays.ImmutableArrays;
import sneer.bricks.hardware.ram.meter.MemoryMeter;
import sneer.bricks.hardwaresharing.files.map.FileMap;
import sneer.bricks.hardwaresharing.files.protocol.BigFileBlocks;
import sneer.bricks.hardwaresharing.files.protocol.FileContents;
import sneer.bricks.hardwaresharing.files.protocol.FileOrFolder;
import sneer.bricks.hardwaresharing.files.protocol.FileRequest;
import sneer.bricks.hardwaresharing.files.protocol.FolderContents;
import sneer.bricks.hardwaresharing.files.server.FileServer;
import sneer.bricks.pulp.blinkinglights.BlinkingLights;
import sneer.bricks.pulp.blinkinglights.LightType;
import sneer.bricks.pulp.tuples.TupleSpace;
import sneer.foundation.brickness.Seal;
import sneer.foundation.brickness.Tuple;
import sneer.foundation.lang.Consumer;

public class FileServerImpl implements FileServer, Consumer<FileRequest> {
	
	
	@SuppressWarnings("unused") private final WeakContract _fileRequestContract;
	
	
	{
		_fileRequestContract = my(TupleSpace.class).addSubscription(FileRequest.class, this);
	}
	
	
	@Override
	public void consume(FileRequest request) {
		try {
			replyIfThereIsEnoughMemoryAvailable(request);
		} catch (IOException e) {
			my(BlinkingLights.class).turnOn(LightType.ERROR, "Error trying to reply FileServer request: " + request, "This might indicate a problem with your file device.", e, 30000);
		}
	}


	private void replyIfThereIsEnoughMemoryAvailable(FileRequest request) throws IOException {
		Object response = getContents(request);

		if (response == null) {
			my(Logger.class).log("FileCache miss.");
			return;
		}

		Tuple reply = asTuple(request.publisher(), response);
		if (reply == null) {
			my(Logger.class).log("FileServer request not answered due to lack of memory: " + request);
			return;
		}

		my(TupleSpace.class).publish(reply);
		logFolderActivity(reply);
	}


	private Object getContents(FileRequest request) {
		Object response = my(FileMap.class).getFile(request.hashOfContents);
		return response == null
			? my(FileMap.class).getFolder(request.hashOfContents)
			: response;
	}


	private Tuple asTuple(Seal addressee, Object response) throws IOException {
		if (response instanceof FolderContents)
			return new FolderContents(((FolderContents)response).contents);
		
		if (response instanceof File) {
			File fileToBeSent = (File)response;
			if (!isThereEnoughMemoryFor(fileToBeSent)) return null;
			return asFileContents(addressee, fileToBeSent);
		}
		
		if (response instanceof BigFileBlocks)
			return new BigFileBlocks(((BigFileBlocks)response)._contents);
			
		throw new IllegalStateException("I don know how to obtain a tuple from type: " + response.getClass());
	}


	private boolean isThereEnoughMemoryFor(File response) {
		return availableMemory() > safeMemoryLimitFor(response);
	}


	private int availableMemory() {
		return my(MemoryMeter.class).maxMBs() - my(MemoryMeter.class).usedMBs().currentValue();
	}


	private int safeMemoryLimitFor(File response) {
		return (int) (3 * response.length() / (1024 * 1024));
	}


	private FileContents asFileContents(Seal addressee, File file) throws IOException {
		byte[] bytes = my(IO.class).files().readBytes(file);
		String debugInfo = file.getName();
		return new FileContents(addressee, my(ImmutableArrays.class).newImmutableByteArray(bytes), debugInfo);
	}

	
	private void logFolderActivity(Tuple reply) {
		if (reply instanceof FolderContents) {
			my(Logger.class).log("Sending Folder Contents:");
			for (FileOrFolder fileOrFolder : ((FolderContents)reply).contents)
				my(Logger.class).log("   FileOrFolder: {} date: {} hash: {}", fileOrFolder.name, fileOrFolder.lastModified, fileOrFolder.hashOfContents);
		}
	}

}
