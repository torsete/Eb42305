package torsete.util;

import java.io.File;
import java.util.Arrays;

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
        return this;
    }

    public TestUtil teardownTestFolder() {
        return this;
    }

    public String getTestFolderName() {
        return testFolderName;
    }
}
