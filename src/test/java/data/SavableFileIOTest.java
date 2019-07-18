package data;

import org.junit.*;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.*;

@SuppressWarnings("ResultOfMethodCallIgnored")
public class SavableFileIOTest implements Serializable {

    private static final String tmpFileDir = "./src/test/resources/tmp/";

    @BeforeClass
    public static void setUp() {
        File tmpDir = new File(tmpFileDir);
        if (tmpDir.exists()) {
            tmpDir.delete();
        }
    }

    @AfterClass
    public static void cleanup() {
        File tmpDir = new File(tmpFileDir);
        File[] files = tmpDir.listFiles();

        for (File file : Objects.requireNonNull(files)) {
            if (file != null && file.exists()) {
                file.delete();
            }
        }
        tmpDir.delete();
    }

    @Test
    public void storeAndLoadObjects() {
        // Combined testing of saving and loading objects.

        SavableFileIO<String> stringSaver = new SavableFileIO<>();

        List<String> stringList = new ArrayList<>();
        stringList.add("a");
        stringList.add("b");
        stringList.add("c");
        stringList.add("d");
        stringList.add("d"); // Should preserve a duplicate if saved from a list.

        // Save to file from list.
        File tempStringFile = new File(tmpFileDir + "string_test.tmp");
        Assert.assertFalse(tempStringFile.exists());
        stringSaver.storeObjects(stringList, tempStringFile, false);
        Assert.assertTrue(tempStringFile.exists());

        // Load and compare, to List.
        List<String> loadedStringList = new ArrayList<>();
        stringSaver.loadObjectsToCollection(tempStringFile, loadedStringList);
        Assert.assertEquals(stringList.size(), loadedStringList.size());
        for (int i = 0; i < loadedStringList.size(); i++) {
            Assert.assertEquals(stringList.get(i), loadedStringList.get(i));
        }

        // Load and compare, to Set.
        Set<String> loadedStringSet = new HashSet<>();
        stringSaver.loadObjectsToCollection(tempStringFile, loadedStringSet);
        Assert.assertEquals(stringList.size() - 1, loadedStringSet.size()); // Should be 1 smaller as duplicate is
        // eliminated.
        for (String s : stringList) {
            Assert.assertTrue(loadedStringSet.contains(s));
        }

        // Appending
        List<String> strListAppend = new ArrayList<>();
        strListAppend.add("WOW");
        strListAppend.add("APPENDING!");
        SavableFileIO<String> stringSaverAppend = new SavableFileIO<>(); // Making sure it can be done with a
        // different saver object.

        stringSaverAppend.storeObjects(strListAppend, tempStringFile, true);

        // Load
        loadedStringList.clear();
        stringSaverAppend.loadObjectsToCollection(tempStringFile, loadedStringList);
        Assert.assertEquals(7, loadedStringList.size());
        Assert.assertEquals("a", loadedStringList.get(0));
        Assert.assertEquals("b", loadedStringList.get(1));
        Assert.assertEquals("c", loadedStringList.get(2));
        Assert.assertEquals("d", loadedStringList.get(3));
        Assert.assertEquals("d", loadedStringList.get(4));
        Assert.assertEquals("WOW", loadedStringList.get(5));
        Assert.assertEquals("APPENDING!", loadedStringList.get(6));

        // Appending when file does not exist.
        // Save to file from list.
        tempStringFile = new File(tmpFileDir + "string_test_append.tmp");
        Assert.assertFalse(tempStringFile.exists());
        stringSaver.storeObjects(stringList, tempStringFile, true);
        Assert.assertTrue(tempStringFile.exists());

        // Load and compare, to List.
        loadedStringList = new ArrayList<>();
        stringSaver.loadObjectsToCollection(tempStringFile, loadedStringList);
        Assert.assertEquals(stringList.size(), loadedStringList.size());
        for (int i = 0; i < loadedStringList.size(); i++) {
            Assert.assertEquals(stringList.get(i), loadedStringList.get(i));
        }

        /* For more complicated objects. */
        SavableFileIO<TestClass> testClassSaver = new SavableFileIO<>();

        TestClassLower tcl1 = new TestClassLower(0.5d);
        TestClass tc1 = new TestClass(4, "qw", 0.7f, tcl1);

        TestClassLower tcl2 = new TestClassLower(0.9d);
        TestClass tc2 = new TestClass(-77, "op", 0.1f, tcl2);

        List<TestClass> testClassList = new ArrayList<>();
        testClassList.add(tc1);
        testClassList.add(tc2);

        File tempTestClassFile = new File(tmpFileDir + "test_class.tmp");
        Assert.assertFalse(tempTestClassFile.exists());
        testClassSaver.storeObjects(testClassList, tempTestClassFile, false);
        Assert.assertTrue(tempTestClassFile.exists());

        List<TestClass> loadedTestClassList = new ArrayList<>();
        testClassSaver.loadObjectsToCollection(tempTestClassFile, loadedTestClassList);

        Assert.assertEquals(testClassList.size(), loadedTestClassList.size());

        TestClass tc1Loaded = loadedTestClassList.get(0);
        TestClass tc2Loaded = loadedTestClassList.get(1);

        Assert.assertEquals(tc1.a, tc1Loaded.a);
        Assert.assertEquals(tc1.b, tc1Loaded.b);
        Assert.assertEquals(tc1.c, tc1Loaded.c, 1e-10);
        Assert.assertEquals(tc1.d.num, tc1Loaded.d.num, 1e-10); // Has same contents although class hash will not be
        // equal after loading.

        Assert.assertEquals(tc2.a, tc2Loaded.a);
        Assert.assertEquals(tc2.b, tc2Loaded.b);
        Assert.assertEquals(tc2.c, tc2Loaded.c, 1e-10);
        Assert.assertEquals(tc2.d.num, tc2Loaded.d.num, 1e-10);

        // Append
        TestClassLower tcl3 = new TestClassLower(0.76d);
        TestClass tc3 = new TestClass(10, "wow", 0.34f, tcl3);

        testClassList.clear();
        testClassList.add(tc3);
        testClassSaver.storeObjects(testClassList, tempTestClassFile, true);

        loadedTestClassList.clear();
        testClassSaver.loadObjectsToCollection(tempTestClassFile, loadedTestClassList);
        tc1Loaded = loadedTestClassList.get(0);
        tc2Loaded = loadedTestClassList.get(1);
        TestClass tc3Loaded = loadedTestClassList.get(2);

        Assert.assertEquals(3, loadedTestClassList.size());

        Assert.assertEquals(tc1.a, tc1Loaded.a);
        Assert.assertEquals(tc1.b, tc1Loaded.b);
        Assert.assertEquals(tc1.c, tc1Loaded.c, 1e-10);
        Assert.assertEquals(tc1.d.num, tc1Loaded.d.num, 1e-10);

        Assert.assertEquals(tc2.a, tc2Loaded.a);
        Assert.assertEquals(tc2.b, tc2Loaded.b);
        Assert.assertEquals(tc2.c, tc2Loaded.c, 1e-10);
        Assert.assertEquals(tc2.d.num, tc2Loaded.d.num, 1e-10);

        Assert.assertEquals(tc3.a, tc3Loaded.a);
        Assert.assertEquals(tc3.b, tc3Loaded.b);
        Assert.assertEquals(tc3.c, tc3Loaded.c, 1e-10);
        Assert.assertEquals(tc3.d.num, tc3Loaded.d.num, 1e-10);
    }

    @Test
    public void combineFiles() {
        SavableFileIO<String> stringSaver = new SavableFileIO<>();

        List<String> stringList1 = new ArrayList<>();
        stringList1.add("a");
        stringList1.add("b");
        stringList1.add("c");
        stringList1.add("d");
        stringList1.add("d"); // Should preserve a duplicate if saved from a list.

        // Save to file from list.
        File tempStringFile1 = new File(tmpFileDir + "f1.tmp");
        stringSaver.storeObjects(stringList1, tempStringFile1, false);

        List<String> stringList2 = new ArrayList<>();
        stringList2.add("e");
        stringList2.add("f");
        stringList2.add("c");
        stringList2.add("q");
        stringList2.add("d"); // Should preserve a duplicate if saved from a list.

        // Save to file from list.
        File tempStringFile2 = new File(tmpFileDir + "f2.tmp");
        stringSaver.storeObjects(stringList2, tempStringFile2, false);

        // Combine to single file.
        File[] filesToCombine = new File[2];
        filesToCombine[0] = tempStringFile1;
        filesToCombine[1] = tempStringFile2;

        File outFile = new File(tmpFileDir + "fcomb.tmp");
        stringSaver.combineFiles(filesToCombine, outFile);

        // Load it back and check.
        List<String> loadedStr = new ArrayList<>();
        stringSaver.loadObjectsToCollection(outFile, loadedStr);

        Assert.assertEquals(7, loadedStr.size());
        Assert.assertTrue(loadedStr.contains("a"));
        Assert.assertTrue(loadedStr.contains("b"));
        Assert.assertTrue(loadedStr.contains("c"));
        Assert.assertTrue(loadedStr.contains("d"));
        Assert.assertTrue(loadedStr.contains("e"));
        Assert.assertTrue(loadedStr.contains("f"));
        Assert.assertTrue(loadedStr.contains("q"));
    }

    @Test
    public void getFilesByExtension() {
        File dir = new File(tmpFileDir);
        if (!dir.exists())
            dir.mkdirs();

        // Create some files in a directory.
        try {
            new File(tmpFileDir + "file1.RedHerring").createNewFile();
            new File(tmpFileDir + "file2.BlueHerring").createNewFile();
            new File(tmpFileDir + "file3.PurpleHerring").createNewFile();
            new File(tmpFileDir + "file4.blueherring").createNewFile();
            new File(tmpFileDir + "file5.blueHerring").createNewFile();
            new File(tmpFileDir + "file6.JustAHerring").createNewFile();
            new File(tmpFileDir + "HerringWithoutExtension").createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Make sure it finds all the ones of a correct file extension.
        // Without . before extension.
        Set<File> filesFiltered = SavableFileIO.getFilesByExtension(dir, "blueherring");
        Assert.assertEquals(3, filesFiltered.size());
        Assert.assertTrue(filesFiltered.contains(new File(tmpFileDir + "file4.blueherring")));
        Assert.assertTrue(filesFiltered.contains(new File(tmpFileDir + "file5.blueHerring")));
        Assert.assertTrue(filesFiltered.contains(new File(tmpFileDir + "file2.BlueHerring")));

        // With . before extension.
        filesFiltered = SavableFileIO.getFilesByExtension(dir, ".blueherring");
        Assert.assertEquals(3, filesFiltered.size());
        Assert.assertTrue(filesFiltered.contains(new File(tmpFileDir + "file4.blueherring")));
        Assert.assertTrue(filesFiltered.contains(new File(tmpFileDir + "file5.blueHerring")));
        Assert.assertTrue(filesFiltered.contains(new File(tmpFileDir + "file2.BlueHerring")));
    }

    private class TestClass implements Serializable {
        private static final long serialVersionUID = 1L;

        int a;
        String b;
        float c;
        TestClassLower d;

        TestClass(int a, String b, float c, TestClassLower d) {
            this.a = a;
            this.b = b;
            this.c = c;
            this.d = d;
        }
    }

    private class TestClassLower implements Serializable {
        private static final long serialVersionUID = 1L;

        double num;

        TestClassLower(double num) {
            this.num = num;
        }
    }
}