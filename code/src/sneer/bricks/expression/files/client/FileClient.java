package sneer.bricks.expression.files.client;

import java.io.File;

import basis.brickness.Brick;

import sneer.bricks.expression.files.client.downloads.Download;
import sneer.bricks.hardware.cpu.crypto.Hash;
import sneer.bricks.identity.seals.Seal;

@Brick
public interface FileClient {

	Download startFileDownload(File file, long lastModified, Hash hashOfFile, Seal source);

	Download startFolderDownload(File folder, Hash hashOfFolder);

	Download startFolderNoveltiesDownload(File folder, Hash hashOfFolder);

}
