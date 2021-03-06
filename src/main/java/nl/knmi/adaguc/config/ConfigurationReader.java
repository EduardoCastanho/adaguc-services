package nl.knmi.adaguc.config;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import nl.knmi.adaguc.security.SecurityConfigurator;
import nl.knmi.adaguc.services.adagucserver.ADAGUCConfigurator;
import nl.knmi.adaguc.services.autowms.AutoWMSConfigurator;
import nl.knmi.adaguc.services.basket.BasketConfigurator;
import nl.knmi.adaguc.services.datasetcatalog.DatasetCatalogConfigurator;
import nl.knmi.adaguc.services.esgfsearch.ESGFSearchConfigurator;
import nl.knmi.adaguc.services.joblist.JobListConfigurator;
import nl.knmi.adaguc.services.oauth2.OAuthConfigurator;
import nl.knmi.adaguc.services.pywpsserver.PyWPSConfigurator;
import nl.knmi.adaguc.services.servicehealth.ServiceHealthConfigurator;
import nl.knmi.adaguc.tools.Debug;
import nl.knmi.adaguc.tools.ElementNotFoundException;
import nl.knmi.adaguc.tools.MyXMLParser.XMLElement;
import nl.knmi.adaguc.tools.Tools;

/**
 * @author maartenplieger
 * Configuration framework for reading one configuration file with multiple sections for pluggable configurators.
 * New configurators must implement the nl.knmi.adaguc.config.ConfigurationInterface class. 
 * New configurators are automatically found in the project by using Reflection on classes which implement this interface.
 *
 */
@Component
public class ConfigurationReader {
	static public long readConfigPolInterval = 0;
	static public boolean readConfigDone = false;
	static public boolean refreshConfig =false;
	
	static private final long readConfigPolIntervalDuration = 10000;
	static public String configFileLocationByEnvironment = "ADAGUC_SERVICES_CONFIG";
	static public String configFileNameInHomeByDefault = "adaguc-services-config.xml";

	@EventListener(ApplicationReadyEvent.class)
	public void doSomethingAfterStartup() {
	    try {
			readConfig();
		} catch (ElementNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			Debug.errprintln(e.getMessage());
		}
	}
	
	public ConfigurationReader(){
	}
	
	static private String _getHomePath(){
		return System.getProperty("user.home")+"/";
	}

	private static String _getConfigFile(){
		try{
			String configLocation = System.getenv(configFileLocationByEnvironment);
			if(configLocation!=null){
				if(configLocation.length()>0){
					return configLocation;
				}
			}
		}catch(Exception e){
		}
		return _getHomePath()+configFileNameInHomeByDefault;
	}

	static public synchronized void reset() {
		readConfigPolInterval = 0;
	}

	/**
	 * 1) Reads the configuration file periodically, re-reads the configuration file
	 * every readConfigPolIntervalDuration ms 2) When read, the doConfig method for
	 * all classes which implement ConfiguratorInterface is called
	 * 
	 * @throws ConfigurationItemNotFoundException
	 * @throws ElementNotFoundException
	 * @throws IOException
	 * @throws Exception
	 */
	static public synchronized void readConfig() throws ElementNotFoundException, IOException {
		if (refreshConfig == false && readConfigDone == true)
			return;
		/* Re-read the configuration file every 10 seconds. */
		if (readConfigPolInterval != 0) {
			if (System.currentTimeMillis() < readConfigPolInterval + readConfigPolIntervalDuration)
				return;
		}

		readConfigDone = true;
		readConfigPolInterval=System.currentTimeMillis(); 
		Debug.println("Reading configfile "+_getConfigFile());
		XMLElement configReader = new XMLElement();
		//try {
			String configFile;
			try {
				configFile = Tools.readFile(_getConfigFile());
			} catch (IOException e2) {
				throw new IOException("Unable to read configuration file " + _getConfigFile() + ". Specify env ADAGUC_SERVICES_CONFIG or place configuration file at expected place.");
			}
			
			/* Replace ${ENV.} with environment variable */
			Set<String> foundValues = new HashSet<String>();
			int start = 0, end = 0, index = -1;
			do{
				index = configFile.substring(start).indexOf("{ENV.");
				if(index>=0){
					start += index;
					end = configFile.substring(start).indexOf("}");
					if(end >=0){
						end+=(start+1);
					    foundValues.add(configFile.substring(start, end));
						start=end;
					}else{
						start+=5; /* In case } is missing, jump 5 places forward */
					}
				}
			}while(index !=-1);
			for(String key : foundValues) {
				String envKey = key.substring(5,key.length()-1);
				String envValue = System.getenv(envKey);
				if(envValue == null){
					Debug.errprintln("[WARNING]: Environment variable ["+envKey+"] not set");
				} else {
					configFile = configFile.replace(key,envValue);
				}
			}
			// Debug.println("configfile=" + configFile);
	
			try {
				configReader.parseString(configFile);
			} catch (Exception e1) {
				e1.printStackTrace();
			}
		///}
		
		
		SecurityConfigurator.doConfig(configReader);
		OAuthConfigurator.doConfig(configReader);
		MainServicesConfigurator.doConfig(configReader);
		ADAGUCConfigurator.doConfig(configReader);
		BasketConfigurator.doConfig(configReader);
		DatasetCatalogConfigurator.doConfig(configReader);
		ESGFSearchConfigurator.doConfig(configReader);
		JobListConfigurator.doConfig(configReader);
		PyWPSConfigurator.doConfig(configReader);
		AutoWMSConfigurator.doConfig(configReader);
		ServiceHealthConfigurator.doConfig(configReader);
	}
}

