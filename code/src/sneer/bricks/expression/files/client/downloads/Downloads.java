package sneer.bricks.expression.files.client.downloads;

import java.io.File;

import basis.brickness.Brick;

import sneer.bricks.hardware.cpu.crypto.Hash;
import sneer.bricks.identity.seals.Seal;

@Brick
public interface Downloads {

	Download newFileDownload(File file, long lastModified, Hash hashOfFile, Seal source);

	Download newFolderDownload(File folder, Hash hashOfFile, boolean copyLocalFiles);

}
