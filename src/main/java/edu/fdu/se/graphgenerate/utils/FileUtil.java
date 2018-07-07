package edu.fdu.se.graphgenerate.utils;

import japa.parser.JavaParser;
import japa.parser.ast.CompilationUnit;
import org.apache.hadoop.fs.*;

import java.io.File;
import java.io.FileInputStream;
import java.util.*;

public class FileUtil {

    public static CompilationUnit openCU(String path) {

        CompilationUnit cu = null;
        FSDataInputStream fis;
        try {
            Path p = new Path(path);
            fis = HDFSUtil.getFileSystem().open(p);
            cu = JavaParser.parse(fis);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return cu;
    }

    public static CompilationUnit openCUByFile(String filePath) {
        CompilationUnit cu = null;

        FileInputStream in;
        try {
            in = new FileInputStream(filePath);
            cu = JavaParser.parse(in); // 解析为语法树
        } catch (Exception e) {
            e.printStackTrace();
        }
        return cu;
    }

    public static List<String> javaFilePaths(String projectPath) {
        List<String> filePaths = new ArrayList<>();
        projectPath = "/java/" + projectPath;
        FileSystem fs = HDFSUtil.getFileSystem();
        RemoteIterator<LocatedFileStatus> iterator;
        try {
            iterator = fs.listFiles(new Path(projectPath), true);
            while (iterator.hasNext()) {
                LocatedFileStatus fileStatus = iterator.next();
                String path = fileStatus.getPath().toString();
                if (path.endsWith(".java")) {
                    String javaFileName = path.split("/")[path.split("/").length - 1];
                    if(except(javaFileName))
                        filePaths.add(path);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return filePaths;
    }

    public static List<File> getJavaFiles(File file) {
        List<File> javaFiles = new ArrayList<>();
        if (!file.exists())
            return new ArrayList<>();
        if (!file.isDirectory())
            javaFiles.add(file);
        else {
            File[] files = file.listFiles();
            if(files != null) {
                for (File f : files) {
                    if (f.isDirectory())
                        javaFiles.addAll(getJavaFiles(f));
                    else if (f.getName().length() > 5 && f.getName().substring(f.getName().length() - 5).equals(".java")
                            && except(f.getName()))
                        javaFiles.add(f);
                }
            }
        }
        return javaFiles;
    }

    private static boolean except(String fileName) {
        return !fileName.equals("R.java") && !fileName.equals("BuildConfig.java") && !fileName.startsWith(".")
                && !fileName.startsWith("_") && !fileName.startsWith("Test") && !fileName.endsWith("Test.java") && !fileName.startsWith("Enum");
    }

    public static void main(String[] args) {
        FileUtil.javaFilePaths("repositories/JakeWharton/ActionBarSherlock").forEach(System.out::println);
        //CompilationUnit cu = FileUtil.openCU("hdfs://ns1/java/repositories/JakeWharton/ActionBarSherlock/actionbarsherlock/src/com/actionbarsherlock/internal/view/menu/MenuView.java");
        //System.out.println(cu.getPackage());
    }

    public static String base(List<String> extendsClass,List<String> implInterface){
        Set<String> except = new HashSet<>(Arrays.asList(LoadProperties.get("EXCEPTCLASS").split(",")));
        for(String str : extendsClass){
            if(!except.contains(str))
                return str;
        }
        for(String str : implInterface){
            if(!except.contains(str))
                return str;
        }
        return "";
    }


}
