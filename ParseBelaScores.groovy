// groovy -cp .:/usr/share/java/opencsv-5.1.jar:/usr/share/java/commons-lang3-3.8.jar ParseBelaScores.groovy

// http://zetcode.com/java/opencsv/
import com.opencsv.CSVParser
import com.opencsv.CSVParserBuilder
import com.opencsv.CSVReaderBuilder
import com.opencsv.CSVReader
import com.opencsv.CSVWriter
import org.apache.commons.lang3.StringUtils

class ParseBelaScores {
	
	static final ORIG_CSV_FILE = "/home/bibu/Workspace/Panca2Duod/data/tuep.csv"
	static final OUT_CSV_FILE = "/home/bibu/Workspace/Panca2Duod/data/tuep_parsed.csv"
	
	static final QUALIT_PARTIAL_ONLY = /^4$/
	static final QUALIT_PARTIAL_AND_NUMERIC = /^4\((\d+)\)$/
	static final QUALIT_DIFFUSE = /^1|2|3$/
	
	static final TECHNICAL_ERROR = /^22$/
	
	static final QUANTIT_NUCLEAR = /x+-(\d+)/
	
	static colNames = [:]
	

def parseCell(aCell, colNumber) {
	
	print("Parsing cell: $aCell")
	
	def newVal = null

	if((aCell =~ QUALIT_PARTIAL_ONLY).matches()) { // Partial without numeric cell value
		assert true == ("4" =~ QUALIT_PARTIAL_ONLY).matches()
		assert false == ("4(30)" =~ QUALIT_PARTIAL_ONLY).matches()
		assert "47.5" == "4".replaceFirst(QUALIT_PARTIAL_ONLY, '47.5')
		
		newVal = aCell.replaceFirst(QUALIT_PARTIAL_ONLY, '47.5')
		
	
	} else if((aCell =~ QUALIT_PARTIAL_AND_NUMERIC).matches()) { // Partial and numeric cell value
		assert true == ("4(45)" =~ QUALIT_PARTIAL_AND_NUMERIC).matches()
		assert false == ("4" =~ QUALIT_PARTIAL_AND_NUMERIC).matches()
		assert "45" == "4(45)".replaceFirst(QUALIT_PARTIAL_AND_NUMERIC) { all, numericValue ->
			"$numericValue"
		}
		
		newVal = aCell.replaceFirst(QUALIT_PARTIAL_AND_NUMERIC) { all, numericValue ->
			"$numericValue"
		}	
			
	} else if((aCell =~ QUALIT_DIFFUSE).matches()) { // Diffuse
		assert true == ("2" =~ QUALIT_DIFFUSE).matches()
		assert false == ("0" =~ QUALIT_DIFFUSE).matches()
		assert "92.5" == "2".replaceFirst(QUALIT_DIFFUSE, '92.5')
		
		newVal = aCell.replaceFirst(QUALIT_DIFFUSE, '92.5')
	
	} else if((aCell =~ TECHNICAL_ERROR).matches()) { // Technical error on staining, non interpretable
		assert true == ("22" =~ TECHNICAL_ERROR).matches()
		assert false == ("2)" =~ TECHNICAL_ERROR).matches()
		assert "" == "22".replaceFirst(TECHNICAL_ERROR, '')
		
		newVal = aCell.replaceFirst(TECHNICAL_ERROR, '')
		
	} else if(aCell.indexOf('x') > -1) {
		newVal = parseNuclearMarker(aCell, colNumber)
	}
	
	println " -> ${newVal == null ? aCell : newVal}"
	
	// Return parsed / original cell value
	if(newVal == null) {
		aCell
	} else {
		newVal
	}
		
}

def parseNuclearMarker(aCell, colNumber) {
	
	
	def intensityScore = StringUtils.countMatches(aCell, 'x')
	print(" intensity = $intensityScore, marker = ${colNames[colNumber]}")
	
	def newVal = null
			
	assert true == ("xx-30" =~ QUANTIT_NUCLEAR).matches() // Nuclear markers scored by intensity (x+) and % cells
	assert false == ("3" =~ QUANTIT_NUCLEAR).matches()
	assert false == ("xx" =~ QUANTIT_NUCLEAR).matches()
	assert "30" == "xx-30".replaceFirst(QUANTIT_NUCLEAR) { all, numericValue ->
		"$numericValue"
	}
	
	newVal = aCell.replaceFirst(QUANTIT_NUCLEAR) { all, numericValue ->
		"$numericValue"
	}
	
	// Disregard weak immunoreactivity for p53
	if((colNames[colNumber].indexOf('p53') > -1) && (intensityScore < 2)) {
		newVal = 0
		print(" disregarding weak p53 intensity")
	}
	
	if(newVal == null) {
		aCell
	} else {
		newVal
	}
}

def runParseBelaScores() {
	
		
	// Open csv file for read
	CSVParser parser = new CSVParserBuilder().withSeparator(';' as char).build();
	
	CSVReader reader = new CSVReaderBuilder(new File(ORIG_CSV_FILE).newReader()).withCSVParser(parser).build();
	

	// Open csv file for write
	CSVWriter writer = new CSVWriter(new File(OUT_CSV_FILE).newWriter());
	
	// Iterate rows in csv file
	String [] aRow
	int rowNum, colNum
	
	try {
		rowNum = 0
		while ((aRow = reader.readNext()) != null) {
	
		if(rowNum > 0 && !aRow[0].startsWith('T')) {
			continue
		}
	 
		   // nextLine[] is an array of values from the line
	   println("Parsing row:\n ${aRow}")
			   
	   colNum = 0
	   for (aCell in aRow) {
		   
		   if(rowNum == 0) { // catch column/marker names
			   colNames.put(colNum,aCell.trim())
		   } else {
			   aRow[colNum] = parseCell(aCell.trim(), colNum)
		   }
		   
		   colNum += 1
	   }
	   
	   println('\n')
	   
	   writer.writeNext(aRow)
	   
	   rowNum += 1
	}
	
	} catch (all) {
		all.printStackTrace()
		
	} finally {
		println("Closing CSV writer...")
		writer.close()
	}
}

static main(args) {

	new ParseBelaScores().runParseBelaScores()

}

}
