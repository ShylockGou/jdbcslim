// Copyright (C) 2015 by six42, All rights reserved. Contact the author via http://github.com/six42
package six42.fitnesse.jdbcslim;

import java.io.FileNotFoundException;
import java.io.IOException;
//import java.util.ArrayList;
import java.util.List;


public class SheetCommandBase implements SheetCommandInterface {

	protected String command = ""; 
	protected SheetFixture myFixture;
	protected String rawResult = null;
	protected List<List<String>> resultSheet;
	protected boolean success = true;
	private PropertiesLoader loader = new PropertiesLoader();
	

	
	public SheetCommandBase(String configurationOptions,  String rawCommand, String outputFormatOptions) throws FileNotFoundException, IOException{
		parseConfiguration(configurationOptions);
		parseConfigurationString(outputFormatOptions);
		command = getCommandIfMissing(configurationOptions, rawCommand);
		myFixture = new SheetFixture(command,  this); 
	}

	public SheetCommandBase(String configurationOptions,  String rawCommand) throws FileNotFoundException, IOException{
		parseConfiguration(configurationOptions);
		command = getCommandIfMissing(configurationOptions, rawCommand);
		myFixture = new SheetFixture(command,  this); 
	}
	
	public SheetCommandBase(String configurationOptions) throws FileNotFoundException, IOException{
		parseConfiguration(configurationOptions);
		command = getCommandIfMissing(configurationOptions, null);
		myFixture = new SheetFixture(command,  this); 
		
	}


	private String getCommandIfMissing(String propertiesFile, String rawCommand) {
		if (rawCommand == null || rawCommand.isEmpty()){ 
			rawCommand = loader.getProperty("cmd");
			if (rawCommand == null){
				throw new RuntimeException("Mandatory parameter 'cmd' not found in properties  (" + propertiesFile + ")." );
			}
		}
		return rawCommand;
	}

	
	private void parseConfiguration(String configurationOptions) throws FileNotFoundException, IOException {
		
		loader.loadFromDefintionOrFile(configurationOptions );
		
	}
	private void parseConfigurationString(String configurationOptions) throws FileNotFoundException, IOException {
		
		loader.loadFromString(configurationOptions );
	}

	

	
	public List doTable(List<List<String>> ParameterTable){
		// Always do this
		return myFixture.doTable(ParameterTable);
	}
 
	@Override
	public void setCommand(String Command){
		this.command = Command;
	}

	@Override
	public void execute() {
		// This is an implementation just for demonstration and testing purposes
		resultSheet = loader.toTable();
		
		success = true;
	}

	@Override
	public void execute(String Command) {

		setCommand(Command);
		execute();
	}

	@Override
	public void reset() {
		// Nothing to be done

	}

	@Override
	public void table(List<List<String>> table) {
		// Nothing to be done

	}

	@Override
	public void beginTable() {
    // Nothing to be done

	}

	@Override
	public void endTable() {
    // Nothing to be done

	}

	public boolean success() {
		return  this.success;
	}

	public String rawResult() {
		if (rawResult != null) return this.rawResult;
		else return this.resultSheet.toString();
	}

	public String command() {
		return this.command;
	}

	public List<List<String>> resultSheet() {
		return this.resultSheet;

	}

	@Override
	public PropertiesInterface Properties() {
		return loader;
	}
	
	public String getCellValue(int row, int column){
		return this.resultSheet.get(row)
				.get(column);
	}

	public String getColumnValue( int column){
		return this.resultSheet.get(1)
				.get(column);
	}
	public String getColumnValueByName( String columnName){
		List<String> Header = this.resultSheet.get(0);
		List<String> Data = this.resultSheet.get(1);
		
		for (int i =0; i< Header.size(); i++ ){
			if (HeaderLine.isHeaderNameEqual(Header.get(i),columnName) ) return Data.get(i);
		}
		throw new RuntimeException("Column not found   (" + columnName + ")." ); 
	}

	public void ResetConfiguration(String configurationOptions) throws FileNotFoundException, IOException {
		
		loader = new PropertiesLoader();
		loader.loadFromDefintionOrFile(configurationOptions );
		
	}
	public void ResetConfigurationFromString(String configurationOptions) throws FileNotFoundException, IOException {
		
		loader = new PropertiesLoader();
		loader.loadFromString(configurationOptions );
	}

	
	public List compareSheet(List<List<String>> ParameterTable){
		// Always do this
		return doTable(ParameterTable);
	}

  @Override
  public void set(String columnName, String value) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public String get(String columnName) {
    // TODO Auto-generated method stub
    return null;
  }
	
}
