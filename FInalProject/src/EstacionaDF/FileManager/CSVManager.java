package EstacionaDF.FileManager;
import java.io.IOException;
import java.io.File;
import java.util.Scanner;
import java.io.FileWriter;
import java.lang.String;
import javax.swing.JOptionPane;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import EstacionaDF.EstacionaExceptions.BlankFieldException;
import EstacionaDF.EstacionaExceptions.RepeatedValue;


public class CSVManager {
    private File csvFile;
    private FileWriter fileWriter;
    private Scanner fileReader;
    private String filename;
    private String categories = "";
    private int columns;


    public CSVManager(String filename, String[] categories) throws Exception {
        this.filename = filename;
        this.columns = categories.length;
        for (String category : categories) {
            this.categories += category + ", ";
        }
        this.categories = this.categories.substring(0, this.categories.length() - 2);
        selectCSV();
        close();
    }

    private void selectCSV() throws IOException {
        this.csvFile = new File("./src/EstacionaDF/Database/" + this.filename + ".csv");
        try {
            // if there isn't any file 
            if (this.csvFile.createNewFile()) {
                System.out.println("File created with success");
                setPlateWriter(new FileWriter(csvFile, true));
                setPlateReader(new Scanner(csvFile));
                // Scanner doesn't recognize .csv with filename, so always use the File overloading.
                getPlateWriter().write(this.categories + "\n");
            }
            // if there is some file
            else {
                setPlateWriter(new FileWriter(csvFile, true));
                setPlateReader(new Scanner(csvFile));
                System.out.println("File found!");

            } 
        } catch (Exception e) {
            this.close();
            throw new IOException("Problem with opening the file");
        }
        

    }

    public void clean() throws IOException 
    {
        this.csvFile = new File(this.filename);
        if (csvFile.exists()) {
            csvFile.delete();
        }
        else {
            throw new IOException("File doesn't exist");
        }
    }
    
    public void addLine(String... details) throws BlankFieldException, IOException {
        if (this.columns == details.length) {
            selectCSV();
            String toWrite = "";
            int counter = 0;
            for (String detail : details) {
                if (detail.isBlank()) {
                    throw new BlankFieldException();
                } else if (counter != details.length - 1){
                    toWrite += detail + ", ";     
                } else {
                    toWrite += detail + "\n";
                }
                counter++;
            }
            getPlateWriter().write(toWrite);
            close();
        }
        else {
            close();
            throw new BlankFieldException();    
        }
        
    }
    private ArrayList<String> readAllLines() throws Exception {
        selectCSV();
        ArrayList<String> lines = new ArrayList<String>();
        while (getPlateReader().hasNextLine()) {
            lines.add(getPlateReader().nextLine());
        }
        return lines;
    }
    public void showContent() throws Exception {
        String table = "";
        for (String lines : readAllLines()) {
            table += lines + "\n";
        }
        final String table2 = table;
        new Thread("CSV Table"){
            @Override
            public void run() {
                JOptionPane.showMessageDialog(null, "<html><h2>Histórico:</h2><br></html>" + table2, "Placas", JOptionPane.PLAIN_MESSAGE);
            }
        }.start();
        close();
    }
    private ArrayList<String> ColumnToTheStart(ArrayList<String> lineByLine, int categoryColumn) throws Exception {
        List<String> allContent = new ArrayList<String>();
        ArrayList<String> allContentOrdered = new ArrayList<String>();
        allContent = lineByLine;
        if (categoryColumn > 0) {
            for (String line : allContent) {
                String[] sepLine = line.split(", ");
                String firstElement = sepLine[0]; String xElement = sepLine[categoryColumn];
                sepLine[0] = xElement; sepLine[categoryColumn] = firstElement;
                line = "";
                for (String element : sepLine) {
                    line += element + ", ";
                }
                line = line.substring(0, line.length() - 2);
                allContentOrdered.add(line);
            }
        } else if (categoryColumn == 0) {
            return lineByLine;
        }
        else {
            throw new Exception("Impossível reordenar");
        }

        return allContentOrdered;

    }

    public String findUser(String withWhat, int categoryColumn, boolean acceptMultiples) throws NoSuchElementException, IOException, RepeatedValue, Exception {
        selectCSV();
        String targetLines = "";
        int counter = 0;
        List<String> allContentOrdered = new ArrayList<String>();
        List<String> searchFrom = new ArrayList<String>();
        allContentOrdered = ColumnToTheStart(readAllLines(), categoryColumn);
        allContentOrdered.remove(0);
        Collections.sort(allContentOrdered);
        for (String singleLine : allContentOrdered) {
            searchFrom.add(singleLine.split(", ")[0]);
        }
        int resultSearch = Collections.binarySearch(searchFrom, withWhat);
        if (resultSearch >= 0) {
            while (resultSearch >= 0) {
                targetLines += allContentOrdered.remove(resultSearch) + ";";
                searchFrom.remove(resultSearch);
                resultSearch = Collections.binarySearch(searchFrom, withWhat);
                counter +=  1;
            }
        } else {
            close();
            throw new NoSuchElementException("Valor não encontrado.");
        }
        if (! acceptMultiples) {
            if (counter > 1) {
                close();
                targetLines = "";
                throw new RepeatedValue(withWhat, getFilename());
            } else {
                targetLines = targetLines.replaceAll(";", "");
            }
        } else {
            close();
        }
        return targetLines;
    }
    

    //DONT CLOSE THE <HTML> TAG!!!
    public void showSearch(String withWhat, int categoryColumn) throws Exception {
        
        String results = findUser(withWhat, categoryColumn, true).replaceAll(";", "\n");
        new Thread("Results Screen") {
            @Override
            public void run() {
                JOptionPane.showMessageDialog(null,
                "<html><h2>Resultados da pesquisa</h2><br>" + results, "Pesquisa", JOptionPane.PLAIN_MESSAGE);
            }
        }.start();
    }

    public void deleteLine(String withWhat, int categoryColumn) throws NoSuchElementException, RepeatedValue, Exception {
        ArrayList<String> allOrganized = ColumnToTheStart(readAllLines(), categoryColumn);
        String delete = findUser(withWhat, categoryColumn, false);
        close();
        // In order to not append when rewritting the document
        setPlateWriter(new FileWriter(this.csvFile, false));
        allOrganized.remove(delete);
        allOrganized = this.ColumnToTheStart(allOrganized, categoryColumn);
        // now "delete" is going to perform the task of the container to the rewritten text.
        delete = "";
        // rewriting the file
        for (String element : allOrganized) {
            delete += element + "\n";
        }

        getPlateWriter().write(delete);
        close();
    }

    public String getFilename() {
        return filename;
    }
    private FileWriter getPlateWriter() {
        return fileWriter;
    } private void setPlateWriter(FileWriter plateWriter) {
        this.fileWriter = plateWriter;
    } private Scanner getPlateReader() {
        return fileReader;
    } private void setPlateReader(Scanner plateReader) {
        this.fileReader = plateReader;
    }


    private void close() {
        try {
            getPlateReader().close();
            getPlateWriter().close();    
        } catch (IOException e) {
            e.printStackTrace();
            
        } catch (IllegalStateException err) {
            err.printStackTrace();
        }
    }


}
