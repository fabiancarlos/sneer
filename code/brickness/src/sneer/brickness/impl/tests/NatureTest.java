package sneer.brickness.impl.tests;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.Assert;
import org.junit.Test;

import sneer.brickness.BrickLoadingException;
import sneer.brickness.Brickness;
import sneer.brickness.ClassDefinition;
import sneer.brickness.impl.tests.fixtures.nature.brick.BrickOfSomeNature;
import sneer.brickness.impl.tests.fixtures.nature.brick.impl.BrickOfSomeNatureImpl;
import sneer.brickness.impl.tests.fixtures.nature.provider.SomeNature;
import sneer.brickness.testsupport.ClassFiles;
import sneer.commons.environments.Environment;
import sneer.commons.environments.EnvironmentUtils;

// TODO: test multiple natures
public class NatureTest extends Assert {
	
	@Test
	public void realizeIsCalled() throws Exception {
		final Mockery mockery = new JUnit4Mockery();
		final SomeNature nature = mockery.mock(SomeNature.class);
		
		mockery.checking(new Expectations() {{
			
			final Class<BrickOfSomeNatureImpl> clazz = BrickOfSomeNatureImpl.class;
			final byte[] brickImplBytecode = bytecodeFor(clazz);
			final ClassDefinition classDef = new ClassDefinition(clazz.getName(), brickImplBytecode);
			exactly(1).of(nature).realize(classDef);
				will(returnValue(Arrays.asList(classDef)));
		}});
		
		loadBrick(Brickness.newBrickContainer(nature), BrickOfSomeNature.class);
		
		mockery.assertIsSatisfied();
	}
	
	@Test (expected = BrickLoadingException.class)
	public void natureWithoutImplementation() {
		final Class<BrickOfSomeNature> brick = BrickOfSomeNature.class;
		Environment container = Brickness.newBrickContainer();
		loadBrick(container, brick);
	}

	private void loadBrick(Environment container, final Class<?> brick) {
		EnvironmentUtils.retrieveFrom(container, brick);
	}

	private byte[] bytecodeFor(final Class<?> clazz) throws IOException {
		File file = ClassFiles.fileFor(clazz);

		if (!file.exists())  throw new FileNotFoundException("File '" + file + "' does not exist");
		if (file.isDirectory()) throw new IOException("File '" + file + "' exists but is a directory");
		if (file.canRead() == false)  throw new IOException("File '" + file + "' cannot be read");
		
		InputStream in = null;
		try {
			in = new FileInputStream(file);
		    ByteArrayOutputStream output = new ByteArrayOutputStream();
			byte[] buffer = new byte[1024*4];
			int n = 0;
			while (-1 != (n = in.read(buffer))) 
				output.write(buffer, 0, n);
			
			return output.toByteArray();
		} finally {
		    try {	in.close(); } catch (Throwable ignore) {}
		}
	}
}