package edu.fdu.se.graphgenerate.utils;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;

import java.net.URI;

public class HDFSUtil {

    public static FileSystem getFileSystem(){
        FileSystem fs = null;
        try{
            Configuration conf = new Configuration() ;
            fs = FileSystem.get(new URI("hdfs://ns1"), conf, "hadoop");
            return fs;
        }catch (Exception e){
            e.printStackTrace();
        }

        return fs;
    }


}
