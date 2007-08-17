package sneer.kernel.appmanager;

import java.io.File;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

import sneer.SneerDirectories;
import sneer.kernel.communication.impl.Communicator;
import sneer.kernel.pointofview.Contact;
import wheel.io.Log;
import wheel.io.ui.User;
import wheel.reactive.lists.ListSignal;
import wheel.reactive.lists.ListSource;
import wheel.reactive.lists.impl.ListSourceImpl;

public class AppManager {
	
	private ListSource<SovereignApplicationUID> _publishedApps = new ListSourceImpl<SovereignApplicationUID>();
	private User _user;
	private Communicator _communicator;
	private ListSignal<Contact> _contacts;
	
	public AppManager(User user, Communicator communicator, ListSignal<Contact> contacts){
		_user = user;
		_communicator = communicator;
		_contacts = contacts;
	}

	private void createDirectories(){ //should be moved to install???
		SneerDirectories.appsDirectory().mkdirs();
		SneerDirectories.compiledAppsDirectory().mkdirs();
		SneerDirectories.appSourceCodesDirectory().mkdirs();
	}
	
	public void rebuild(){
		removeRecursive(SneerDirectories.compiledAppsDirectory());
		removeRecursive(SneerDirectories.appSourceCodesDirectory());
		for(SovereignApplicationUID app:_publishedApps.output())
			_publishedApps.remove(app);
		
	}
	
	public void remove(String appName){
		removeRecursive(new File(SneerDirectories.appsDirectory(),appName));
		removeRecursive(new File(SneerDirectories.appSourceCodesDirectory(),appName));
		removeRecursive(new File(SneerDirectories.compiledAppsDirectory(),appName));
		rebuild();
	}
	
	public void publish(File srcFolder) { //Fix: what if the app is already installed? test appuid
		try{
			File targetDirectory = new File(SneerDirectories.appsDirectory(),appName(srcFolder));
			targetDirectory.mkdirs();
			File zipFile = new File(targetDirectory,"app.zip");
			AppTools.zip(srcFolder, zipFile);
			AppTools.generateAppUID(zipFile);
		}catch(Exception e){
			Log.log(e);
			e.printStackTrace();
		}
	}

	private String appName(File srcFolder) {
		return AppTools.pathToPackage(new File(srcFolder,"src"), AppTools.findApplicationSource(srcFolder).getParentFile());
	}
	
	public SovereignApplication appByUID(String appUID){
		for(SovereignApplicationUID app:_publishedApps.output())
			if (app._appUID.equals(appUID))
				return app._sovereignApplication;
		return null;
	}
	
	public boolean isAppPublished(String appName){
		return (new File(SneerDirectories.appsDirectory(),appName)).exists();
	}
	
	private void unpackageApps(){
		for(File appDirectory:notUnpackagedApps()){
			File jar = appDirectory.listFiles()[0]; //first file should be the jar
			File target = new File(SneerDirectories.appSourceCodesDirectory(),appDirectory.getName());
			target.mkdir();
			System.out.println("Unpackaging "+jar.getName());
			try{
				AppTools.unzip(jar,target);
			}catch(Exception e){
				target.delete();
				Log.log(e);
				e.printStackTrace();
			}
		}
	}
	
	private List<File> notUnpackagedApps(){ 
		List<File> notUnpackagedApps = new ArrayList<File>();
		for(File appDirectory:SneerDirectories.appsDirectory().listFiles()){
			if (alreadyUnpackaged(appDirectory))
				continue;
			notUnpackagedApps.add(appDirectory);	
		}
		return notUnpackagedApps;
	}
	
	private void compileApps(){ //FixUrgent... if the compilation fails, the directory MUST be cleaned
		for(File sourceDirectory:notCompiledApps()){

			String targetDirectory=SneerDirectories.compiledAppsDirectory()+"/"+sourceDirectory.getName()+"/"+"classes";
			String sourceApplication = AppTools.findApplicationSource(sourceDirectory).getAbsolutePath();
			(new File(targetDirectory)).mkdirs();

			System.out.println("Compiling "+sourceApplication);
			String sneerJarLocation = null;
			try{
				sneerJarLocation = AppTools.tryToFindSneerLocation().getAbsolutePath();
			}catch(Exception e){
				Log.log(e);
				e.printStackTrace();
				_user.acknowledgeNotification("Sneer.jar not found. If you are not running from the jar (from eclipse for example) you need SneerXXXX.jar as the ONLY jar in the .bin directory.");
				return;
			}
			
			System.out.println(sneerJarLocation);
			try{
				String[] parameters = {"-classpath",sneerJarLocation+File.pathSeparator+targetDirectory,"-sourcepath",sourceDirectory.getAbsolutePath()+"/src","-d",targetDirectory,sourceApplication};
				com.sun.tools.javac.Main.compile(parameters);
			}catch(Exception e){
				e.printStackTrace();
				System.out.println("Could not compile "+sourceApplication); //Fix: use log
				removeRecursive(new File(targetDirectory));
			}
		}
	}
	
	private void removeRecursive(File file){
		if (file.isDirectory())
			for(File children:file.listFiles())
				removeRecursive(children);
		file.delete();
	}
	
	public ListSource<SovereignApplicationUID> publishedApps(){
		createDirectories();
		unpackageApps();
		compileApps();
		loadApps();
		return _publishedApps;
	}

	private void loadApps() {
		File[] compiledAppDirectories = SneerDirectories.compiledAppsDirectory().listFiles();
		for(File compiledAppDirectory:compiledAppDirectories){
			String appName = compiledAppDirectory.getName();
			if (isAppLoaded(appName))
				continue;
			try{
				File appUIDFile = AppTools.findAppUID(new File(SneerDirectories.appsDirectory(),compiledAppDirectory.getName()));
				String appUID = new String(AppTools.getBytesFromFile(appUIDFile));
				_publishedApps.add(new SovereignApplicationUID(compiledAppDirectory.getName(),appUID,appLoad(compiledAppDirectory)));
			}catch(Exception e){
				Log.log(e);
				e.printStackTrace();
			}
		}
	}

	private boolean isAppLoaded(String appName) {
		for(SovereignApplicationUID app:_publishedApps.output())
			if (app._appName.equals(appName))
				return true;
		return false;
	}

	@SuppressWarnings("deprecation")
	private SovereignApplication appLoad(File compiledAppDirectory) throws Exception {
		File classesDirectory = new File(compiledAppDirectory,"classes");
		File applicationFile = AppTools.findApplicationClass(compiledAppDirectory);
		String packageName = AppTools.pathToPackage(classesDirectory, applicationFile.getParentFile());
		URL[] urls = new URL[]{classesDirectory.toURL()}; //in the future libs directory will be added here
		URLClassLoader ucl = new URLClassLoader(urls, ClassLoader.getSystemClassLoader());  
		Class<?> clazz = ucl.loadClass(packageName+".Application"); 
		AppConfig config = new AppConfig(_user, new AppChannelFactory(_communicator), _contacts, _publishedApps.output());
		Class<?>[] types = {AppConfig.class};
		Object[] instances = {config};
		Constructor<?> constructor = clazz.getConstructor(types);
		return (SovereignApplication) constructor.newInstance(instances);
	}
	
	private List<File> notCompiledApps(){ 
		List<File> notCompiledApps = new ArrayList<File>();
		for(File sourceDirectory:SneerDirectories.appSourceCodesDirectory().listFiles()){
			if (alreadyCompiled(sourceDirectory))
				continue;
			notCompiledApps.add(sourceDirectory);	
		}
		return notCompiledApps;
	}
	
	private boolean alreadyUnpackaged(File appDirectory) {
		for(File sourceCodeDirectory:SneerDirectories.appSourceCodesDirectory().listFiles()){
			if (sourceCodeDirectory.getName().equals(appDirectory.getName()))
				return true;
		}
		return false;
	}

	private boolean alreadyCompiled(File sourceDirectory) {
		for(File compiledDirectory:SneerDirectories.compiledAppsDirectory().listFiles()){
			if (sourceDirectory.getName().equals(compiledDirectory.getName()))
				return true;
		}
		return false;
	}
	
}
