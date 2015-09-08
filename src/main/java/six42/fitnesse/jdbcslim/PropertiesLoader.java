// Copyright (C) 2015 by six42, All rights reserved. Contact the author via http://github.com/six42
package six42.fitnesse.jdbcslim;

import dbfit.util.crypto.CryptoFactories;
import dbfit.util.crypto.CryptoService;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

public class PropertiesLoader implements PropertiesInterface{

    private CryptoService crypto;
    private DefineProperties theDefinitions = new DefineProperties(null);
	private final static String encryptedFormPrefix = "ENC("; 
	private final static String commentTag = "#"; 
	
	/** 
	 * Contains all loaded properties
	 */
	private Map<String,String> myProperties = new HashMap<String, String>();
	private boolean debugFlag;

	/**
	 *  Encrypted values should not be printed in clear text!
	 *  to avoid this their key will be prefixed with the below tag.
	 *  Best practice is to use the same tag as for comments as no key can be entered
	 *  which starts with such a tag. :)
	 *  Use the GetSecret method instead of the getProperty method to get the value
	 *  encrypted properties
	 */
	private final static String secretTag = commentTag; 
	
    public PropertiesLoader(CryptoService cryptoService) {
        this.crypto = cryptoService;
    }

    public PropertiesLoader() {
        this(CryptoFactories.getCryptoService());
    }

    public Map<String, String> loadFromTupleList(List<String[]> tuples) throws FileNotFoundException, IOException {
        Map<String, String> props = new HashMap<String, String>();
        String key;
        String value;
        for (String[] key_value : tuples) {
	            if (key_value == null) continue;
	            String[] key_value2 = createTuple(key_value[0],key_value[1]);
	            if (key_value2 == null) continue;
	            key = key_value2[0];
	            value = key_value2[1];
	            if (checkKeyForCommandAndExecute(key, value, props)){
	            	// Do nothing already done in the function above
	            }else{
	            	props.put(key, value);
	            }
	            
        }
        myProperties.putAll(props);
        setDebugFromProperties();
        return props;
      }
    

    public Map<String, String> loadFromList(List<String> lines) throws FileNotFoundException, IOException {
        Map<String, String> props = new HashMap<String, String>();
        Boolean multiLine = false;
        Boolean multiFirstLine = false;
        String multiLineValue ="";
        String multiLineKey ="";
        final String multiLineTag = "\"\"\"";
        
        for (String line : lines) {
        	if (multiLine){
        		if (multiLineTag.equals(line)){
        			// End of MultiLine Value
        			props.put(multiLineKey, multiLineValue);
        			multiLine = false;
        		}else{
        			if (multiFirstLine){
        				multiLineValue = line;
            			multiFirstLine = false;
        			}
        			else{
	        			// Using always the Unix separator. This also works under Windows
	        			// using  System.lineSeparator() didn't worked under Windows :(;
	        			multiLineValue = multiLineValue + "\n" + line ;
        			}
        		}
        		
        	}else{
	            String[] keyval = parseLine(line);
	            if (keyval != null) {
	            	if (checkKeyForCommandAndExecute(keyval[0], keyval[1], props)){
	            		// Do nothing already done in the function above
	            	}else if (multiLineTag.equals(keyval[1])){
	            		// Start of a multiLine value
	            		multiLine=true;
	            		multiFirstLine = true;
	            		multiLineValue="";
	            		multiLineKey=keyval[0];
	                    
	            	}else{
	            		props.put(keyval[0], keyval[1]);
	            	}
	            }
        	}
        }

        myProperties.putAll(props);
        setDebugFromProperties();
        return props;
        /**
         *  Limitations:  If a multi line tag is not closed at the end than the property is 
         *  					dropped without any error
         *  			  It is expected that an encrypted value ends always with a bracket 
         *  		
         *   
         */
    }

    private boolean checkKeyForCommandAndExecute(String key, String value,
			Map<String, String> props) throws FileNotFoundException, IOException {
		if (key.equalsIgnoreCase(".include")){
			Map<String, String> subMap;
			subMap = loadFromDefintionOrFile(value);
			props.putAll(subMap);
			return true;
		
		}
		if (key.equalsIgnoreCase(".keyStoreLocation")){
			if (value.equals("")){
				crypto = null;
			}else{
				crypto = CryptoFactories.getCryptoServiceFactory().getCryptoService(CryptoFactories.getCryptoKeyStoreFactory().newInstance(new File(value)));
				System.out.println("New Key Store Location " + value);
			}
			return true;
		}
		return false;
	}

	public Map<String, String> loadFromDefintionOrFile(String definitionName) throws FileNotFoundException, IOException{
		List<String[]> oneDefinition = theDefinitions.getDefinition(definitionName);
		if (oneDefinition == null){
			return loadFromFile(definitionName);
		}else{
			return loadFromTupleList(oneDefinition);
		}
	}

    public Map<String, String> loadFromFile(String path)
            throws FileNotFoundException, IOException {
        FileReader reader = null;
        try {
            reader = new FileReader(new File(path));
            BufferedReader br = new BufferedReader(reader);
            List<String> lines = new ArrayList<String>();
            String line;
            while ((line = br.readLine()) != null) {
                lines.add(line);
            }

            return loadFromList(lines);
        } finally {
            if (null != reader) {
                reader.close();
            }
        }
    }

    public Map<String, String> loadFromString(String spaceSeparatedKeys)
            throws FileNotFoundException, IOException {
 
    	List<String> lines = new ArrayList<String>();
    	String[] tags = spaceSeparatedKeys.split(" ");
        for (String line : tags){
        	if (!line.trim().isEmpty()) lines.add(line);
        }

        return loadFromList(lines);
    }

    /**
     * Unwrap encrypted form value "ENC(value)".
     *
     * @return text if wrapped in ENC(text)
     *         null if format is not ENC(...)
     */
    public static String unwrapEncryptedValue(String encValue) {
        if (encValue == null) {
            return null;
        }

        if (encValue.startsWith(encryptedFormPrefix)) {
            return encValue.substring(encryptedFormPrefix.length(),
                                        encValue.length() - 1);
        } else {
            return null;
        }
    }

    /**
     * Check if the value is an encrypted value 
     *  "ENC(value)".
     *
     * @return true if wrapped in ENC(text)
     *         false if not ENC(...)
     */
    private static boolean isSecret(String encValue) {
        if (encValue == null) {
            return false;
        }

        if (encValue.trim().startsWith(encryptedFormPrefix)) {
            return true;
        } else {
            return false;
        }
    }
    
    public String parseValue(String rawValue) {
    	String value = rawValue.trim();
        String encValue = unwrapEncryptedValue(value);

        if (encValue == null || crypto == null) {
            return value;
        } else {
          try{
            String decryptedValue =  crypto.decrypt(encValue);
            return decryptedValue;
          }catch (Exception e){
            throw new RuntimeException("decrypt of " + rawValue + " failed. Wrong Key Store used?", e);
          }
        }
    }
    private String parseKey(String rawKey) {
    	return rawKey.trim().toLowerCase();
    }
    
    public static String[] splitKeyVal(String line) {
        return line.split("=", 2);
    }

    private boolean isIgnorableLine(String line) {
        if (line == null) {
            return true;
        }

        String trimline = line.trim();

        if ((trimline.length() == 0) || (trimline.startsWith(commentTag))) {
            return true;
        }

        return false;
    }

    private String[] parseLine(String line) {
        if (isIgnorableLine(line)) {
            return null;
        }

        String[] keyval = splitKeyVal(line.trim());
        String key = keyval[0];
        String val;

        if (keyval.length == 1) {
            val = "";
        }else{
          val = keyval[1];
        }
        
        return createTuple(key,val);
    }

    private String[] createTuple(String rawKey, String rawValue) {
      if (isIgnorableLine(rawKey)) {
          return null;
      }
      String key = parseKey(rawKey);

      String val;

      if (rawValue == null) {
          val = "";
      } else {
          val = parseValue(rawValue);
          if (isSecret(rawValue)){
            key = secretTag + key;
          }
      }

      return new String[] {key, val};
  }

    /** 
     * Used for Testing only
     * Put all properties (key value) in a Slim table
     * encrypted values are not printed
     * @param propertiesMap with properties 
     * @return Slim Table with Properties
     */
	public List<List<String>> toTable() {
		// This is an implementation just for demonstration and testing purposes
		List<List<String>> resultSheet = new ArrayList<List<String>>();
		List<String> line = new ArrayList<String>();
		line.add("Key");
		line.add("Value");
		resultSheet.add(line);
	
		for (Map.Entry<String, String> entry: myProperties.entrySet()) {
	        String key = entry.getKey();
	        String val = entry.getValue();
	        // Don't print encrypted values in clear text
	        if (key.startsWith(secretTag)){
	        	key = key.substring(secretTag.length());
	        	val = "****";
	        }
			  line = new ArrayList<String>();
			  line.add(key);
			  line.add(val);
			  resultSheet.add(line);
			}		
		return resultSheet;
	}

	
	/**
	 * Checks for encrypted properties and returns
	 * then decrypted values. If no encrypted property exists
	 * return the value of a not encrypted property 
	 * @param propertyName
	 * @return
	 */
	public String getSecretProperty(String propertyName) {
		
		String result = getPropertyOrDefault(secretTag+propertyName, null);
		if (result == null){
			result = getPropertyOrDefault(propertyName, null);
		}
		return result;
	}

	public String getSecretProperty(ConfigurationParameters dbpassword){
	  return getSecretProperty(dbpassword.toString());
	}
	
	public String getProperty(String propertyName) {
		return getPropertyOrDefault(propertyName, null);
	}
	
	public String getProperty(ConfigurationParameters propertyName){
	  return getProperty(propertyName.toString());
	}

	public String getPropertyOrDefault(String propertyName, String defaultValue) {
		String propertyValue = myProperties.get(propertyName.toLowerCase());
		if (propertyValue == null) propertyValue =defaultValue;
		return propertyValue;
	}

	 public String getPropertyOrDefault(ConfigurationParameters propertyName, String defaultValue) {
	   return getPropertyOrDefault(propertyName.toString(), defaultValue);
	 }

	public boolean isDebug(){
		return debugFlag;
	}

	private void setDebugFromProperties(){
		debugFlag = (getProperty("debug") != null);
	}

}

