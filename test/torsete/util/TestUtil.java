package torsete.util;

import java.io.*;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Created by Torsten on 18.05.2017.
 */
public class TestUtil {
    public static final String TEMP_DIR = "../temporary_testdata";
    private String testName;
    private String testFolderName;

    public TestUtil(Object object) {
        testName = object instanceof Class ? ((Class) object).getName() : object.getClass().getName();
    }

    public TestUtil setupTestFolder() {
        File folderFile = new File(TEMP_DIR + "/" + testName);
        folderFile.mkdir();
        Arrays.stream(folderFile.listFiles()).forEach(file -> System.out.print(file.getAbsolutePath()));
        testFolderName = folderFile.getAbsolutePath();
        Arrays.stream(new File(testFolderName).listFiles()).forEach(f -> f.delete());
        return this;
    }

    public TestUtil teardownTestFolder() {


        return this;
    }

    public String getTestFolderName() {
        return testFolderName;
    }

    public void writeFile(String filename, String content) throws IOException {
        Writer writer = new BufferedWriter(new FileWriter(testFolderName + File.separator + filename));
        writer.write(content);
        writer.close();
        System.out.println("******************************");
        System.out.println(new File(filename).getAbsoluteFile() + ":");
        System.out.println(content);
        System.out.println("******************************");
    }

    public void writeFile(String filename, String... lines) throws IOException {
        writeFile(filename, Arrays.stream(lines).collect(Collectors.joining("\n")));
    }

    public String getFoldername(String filename) {
        return testFolderName + File.separator + filename;
    }

    public File getFile(String filename) {
        return new File(testFolderName + File.separator + filename);
    }

    public InputStream getInputStream(String filename) throws FileNotFoundException {
        return new FileInputStream(testFolderName + File.separator + filename);
    }

    public BufferedWriter getBufferedWriter(String filename) throws IOException {
        return new BufferedWriter(new FileWriter(testFolderName + File.separator + filename));
    }

    public BufferedReader getBufferedReader(String filename) throws IOException {
        return new BufferedReader(new FileReader(testFolderName + File.separator + filename));
    }

    public Reader getFileReader(String filename) throws IOException {
        return new FileReader(testFolderName + File.separator + filename);
    }

    public String getContent(String filename) throws IOException {
        InputStream inputStream = getInputStream(filename);
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int nRead;
        byte[] data = new byte[1024];
        while ((nRead = inputStream.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }
        buffer.flush();
        return new String(buffer.toByteArray());
    }
}
