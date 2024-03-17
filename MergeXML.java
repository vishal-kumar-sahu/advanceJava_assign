import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.*;
import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class MergeXML {
    private static final String LICENSE_HEADER_ROW =
            "nipr,License ID,Jurisdiction,Resident,License Class,License Effective Date,License Expiry Date,License Status,License Line,License Line Effective Date,License Line Expiry Date,License Line Status";
    private static List<String> validLicense;
    private static List<String> inValidLicense;

    public static void main(String []args){

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();

            Document doc1 = builder.parse(new File("src/License1.xml"));
            Document doc2 = builder.parse(new File("src/License2.xml"));

            doc1.getDocumentElement().normalize();
            doc2.getDocumentElement().normalize();

            validLicense = new ArrayList<>();
            inValidLicense = new ArrayList<>();
            getElementsFromXML(doc1);
            getElementsFromXML(doc2);

            BufferedWriter validLicenseWriter = new BufferedWriter(new FileWriter("src/validLicenses.txt"));
            validLicenseWriter.write(LICENSE_HEADER_ROW);

            BufferedWriter invalidLicenseWriter = new BufferedWriter(new FileWriter("src/invalidLicenses.txt"));
            invalidLicenseWriter.write(LICENSE_HEADER_ROW);

            BufferedWriter mergedFileWriter = new BufferedWriter(new FileWriter("src/mergedList.txt"));
            mergedFileWriter.write(LICENSE_HEADER_ROW);

            for(String str : validLicense){
                mergedFileWriter.newLine();
                mergedFileWriter.write(str);

                validLicenseWriter.newLine();
                validLicenseWriter.write(str);
            }

            for(String str : inValidLicense){
                mergedFileWriter.newLine();
                mergedFileWriter.write(str);

                invalidLicenseWriter.newLine();
                invalidLicenseWriter.write(str);
            }

            validLicenseWriter.close();
            invalidLicenseWriter.close();
            mergedFileWriter.close();
        }
        catch (Exception e){
            System.out.println(e.getMessage());
        }


    }
    public static void getElementsFromXML(Document doc){
        NodeList producerList = doc.getElementsByTagName("CSR_Producer");
        List<String> niprs = new ArrayList<>();
        for(int i = 0; i < producerList.getLength(); i++) {
            Element producer = (Element) producerList.item(i);
            String nipr = producer.getAttribute("NIPR_Number");

            int count = producer.getChildNodes().getLength();
            for(int j = 0; j < count; j++){
                niprs.add(nipr);
            }
        }

        NodeList licenseList = doc.getElementsByTagName("License");

        NodeList loaList = doc.getElementsByTagName("LOA");
        int loaLength = loaList.getLength();
        int count = 0;

        for(int i = 0; i < licenseList.getLength(); i++){
            Element license = (Element) licenseList.item(i);

            StringBuilder sb = getStringBuilder(niprs, i, license);

            String licenseExpirationDate = license.getAttribute("License_Expiration_Date");

            while (count < loaLength){
                Element loa = (Element) loaList.item(count);
                if(loa == null){
                    break;
                }

                StringBuilder loaSB = new StringBuilder(sb);

                String licenseLine = loa.getAttribute("LOA_Name");
                loaSB.append(licenseLine + ", ");

                String licenseLineEffectiveDate = loa.getAttribute("LOA_Issue_Date");
                loaSB.append(licenseLineEffectiveDate + ", ");

                loaSB.append(licenseExpirationDate + ", ");

                String licenseLineStatus = loa.getAttribute("LOA_Status");
                loaSB.append(licenseLineStatus);

                if(isValidLicense(license)){
                    validLicense.add(loaSB.toString());
                }
                else{
                    inValidLicense.add(loaSB.toString());
                }

                count++;
            }
        }
    }

    private static StringBuilder getStringBuilder(List<String> niprs, int i, Element license) {
        StringBuilder sb = new StringBuilder();
        sb.append(niprs.get(i) + ", ");

        String licenseId = license.getAttribute("License_Number");
        sb.append(licenseId + ", ");

        String jurisdiction = license.getAttribute("State_Code");
        sb.append(jurisdiction + ", ");

        String resident = license.getAttribute("Resident_Indicator");
        sb.append(resident + ", ");

        String licenseClass = license.getAttribute("License_Class");
        sb.append(licenseClass + ", ");

        String licenseEffectiveDate = license.getAttribute("Date_Status_Effective");
        sb.append(licenseEffectiveDate + ", ");

        String licenseExpiryDate = license.getAttribute("License_Expiration_Date");
        sb.append(licenseExpiryDate + ", ");

        String licenseStatus = license.getAttribute("License_Status");
        sb.append(licenseStatus + ", ");

        return sb;
    }

    public static boolean isValidLicense(Element license){
        LocalDate currentDate = LocalDate.now();

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/dd/yyyy");

        String expirationDateString = license.getAttribute("License_Expiration_Date");
        LocalDate expirationDate = LocalDate.parse(expirationDateString, formatter);

        return expirationDate.isAfter(currentDate) || expirationDate.isEqual(currentDate);
    }
}
