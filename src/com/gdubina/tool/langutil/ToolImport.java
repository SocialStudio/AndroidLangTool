package com.gdubina.tool.langutil;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Iterator;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class ToolImport {

	private DocumentBuilder builder;
	private File outResDir;

	public ToolImport() throws ParserConfigurationException {
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		builder = dbf.newDocumentBuilder();
	}

	public static void main(String[] args) throws FileNotFoundException, IOException, ParserConfigurationException, TransformerException {
		if(args == null || args.length == 0){
			System.out.println("File name is missed");
			return;
		}
		run(args[0]);
	}
	
	public static void run(String input) throws FileNotFoundException, IOException, ParserConfigurationException, TransformerException{
		if(input == null || "".equals(input)){
			System.out.println("File name is missed");
			return;
		}
		HSSFWorkbook wb = new HSSFWorkbook(new FileInputStream(new File(input)));
		HSSFSheet sheet = wb.getSheetAt(0);
		

		ToolImport tool = new ToolImport();
		tool.outResDir = new File("out/" + sheet.getSheetName()+ "/res");
		tool.outResDir.mkdirs();
		tool.parse(sheet);		
	}

	private void parse(HSSFSheet sheet) throws IOException, TransformerException {
		Row row = sheet.getRow(0);
		Iterator<Cell> cells = row.cellIterator();
		cells.next();// ignore key
		int i = 1;
		while (cells.hasNext()) {
			String lang = cells.next().getStringCellValue();
			generateLang(sheet, lang, i);
			i++;
		}
	}

	private void generateLang(HSSFSheet sheet, String lang, int column) throws IOException, TransformerException {
		
		Document dom = builder.newDocument();
		Element root = dom.createElement("resources");
		dom.appendChild(root);
		
		Iterator<Row> iterator = sheet.rowIterator();
		iterator.next();//ignore first row;
		
		while (iterator.hasNext()) {
			HSSFRow row = (HSSFRow) iterator.next();
			Cell cell = row.getCell(0);// android key
			if (cell == null) {
				continue;
			}
			String key = cell.getStringCellValue();
			if (key == null || "".equals(key)){
				root.appendChild(dom.createTextNode(""));
				continue;
			}
			if(key.startsWith("/**")){
				root.appendChild(dom.createComment(key.substring(3, key.length() - 3)));
				continue;
			}

			String value = row.getCell(column).getStringCellValue();// value
			
			Element node = dom.createElement("string");
			node.setAttribute("name", key);
			node.setTextContent(eluminateText(value));
			root.appendChild(node);
		}
		
		save(dom, lang);
	}

	private static String eluminateText(String text) {
		return text.replace("'", "\\'");
	}

	private void save(Document doc, String lang) throws TransformerException {
		File dir;
		if("default".equals(lang) || lang == null || "".equals(lang)){
			dir = new File(outResDir, "values");
		}else{
			dir = new File(outResDir, "values-" + lang);
		}
		dir.mkdir();
		
		//DOMUtils.prettyPrint(doc);
		TransformerFactory transformerFactory = TransformerFactory.newInstance();
		Transformer transformer = transformerFactory.newTransformer();
		transformer.setOutputProperty(OutputKeys.INDENT, "yes");
		transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
		
		DOMSource source = new DOMSource(doc);
		StreamResult result = new StreamResult(new File(dir, "strings.xml"));

		transformer.transform(source, result);
	}
}
