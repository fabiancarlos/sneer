package sneer.bricks.expression.files.map;

import java.util.List;

import basis.brickness.Brick;

import sneer.bricks.expression.files.protocol.FileOrFolder;
import sneer.bricks.expression.files.protocol.FolderContents;
import sneer.bricks.hardware.cpu.crypto.Hash;
import sneer.bricks.hardware.io.prevalence.nature.Prevalent;
import sneer.bricks.hardware.io.prevalence.nature.Transaction;

@Brick (Prevalent.class)
public interface FileMap {

	void putFile(String path, long lastModified, Hash hash);
	void putFolder(String path, Hash hash);
	
	Hash getHash(String path);

	String getFile(Hash hash);
	List<String> getFiles(Hash hash);
	List<FileOrFolder> dir(String path);
	String getFolder(Hash hash);
	long getLastModified(String file);
	
	FolderContents getFolderContents(Hash hash);

	@Transaction
	Hash remove(String path);
	void rename(String fromPath, String toPath);
	
}
