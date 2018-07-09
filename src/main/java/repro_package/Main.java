package repro_package;

/*

Does authentication work correctly when we're using both NIO and Dataproc? This test aims to find
out (for this particular configuration at least).

Follow the steps in README.txt to compile and run this program.

*/

import java.io.Serializable;
import java.net.URI;
import java.nio.file.Paths;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.JavaRDD;
import com.google.cloud.storage.contrib.nio.CloudStorageFileSystem;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.io.IOException;

public class Main {

  /**
   * The test, packaged as a class so we can conveniently send the test and its parameters
   * to remote machines.
   *
   * The test is just to call "Files.exists" on an existing file.
   */
  public static class MyTest implements Serializable {

    final String bucket;
    final String testPath;
    static final long serialVersionUID = 1L;

    public MyTest(String bucket, String testPath) {
      this.bucket = bucket;
      this.testPath = testPath;
    }

    public String run() {
      String ret = "";
      String method = "CloudStorageFileSystem.forBucket";
      try {
        CloudStorageFileSystem fs = CloudStorageFileSystem.forBucket(bucket);
        java.nio.file.Path p = fs.getPath(testPath);
        // We call it just to check whether it'll throw an exception.
        Files.exists(p);
        ret += method + ": works.";
      } catch (Exception x) {
        ret += method + ": threw an exception: " + x.getMessage();
      }

      ret += "\n";
      String pathWithBucket = "gs://" + bucket + testPath;
      method = "Paths.get("+pathWithBucket+")";
      try {
        java.nio.file.Path p = Paths.get(URI.create(pathWithBucket));
        Files.exists(p);
        ret += method + ": works.";
      } catch (Exception x) {
        ret += method + ": threw an exception: " + x.getMessage();
      }
      return ret;
    }

  }


  public static void main(final String[] args) {
    // No command-line processing library makes our dependency list shorter
    // and the test perhaps more convincing. It makes for uglier command-line processing
    // code and for that I apologize.
    if ((args.length>0 && args[0].contains("help")) || args.length<2) {
      System.out.println("Usage:");
      System.out.println("--help for help");
      System.out.println("<bucket> <path> --local for local test only");
      System.out.println("<bucket> <path> for local and Dataproc test (run via gcloud dataproc jobs submit)");
    }
    String bucket = args[0];
    String testPath = args[1];
    MyTest test = new MyTest(bucket, testPath);
    boolean localOnly = (args.length>2 && args[2].contains("local"));
    System.out.println("Checking exists(gs://" + bucket + testPath+")" + (localOnly?", local test only":", local and remote test."));
    System.out.println("\n(a) Running 'exists' locally.");
    System.out.println("    It should work if you have set the default credentials.");
    // return value describes success/failure
    System.out.println(test.run());
    if (!localOnly) {
      System.out.println("\n(b) Running 'exists' on a Spark cluster via Dataproc.");
      ArrayList<String> l = new ArrayList<>();
      l.add("1");
      final SparkConf sparkConf = new SparkConf().setAppName("repro_package").setMaster("yarn");
      final JavaSparkContext ctx = new JavaSparkContext(sparkConf);
      JavaRDD<String> rdd = ctx.parallelize(l, l.size()).map(i -> test.run());
      List<String> ret = rdd.collect();
      for (String r : ret) {
        // return value describes success/failure
        System.out.println("\nResult from remote test:\n" + r + "\n");
      }
      System.out.println("If you see an error message above (or two), that indicates the remote test failed.");
    }
    System.out.println("Done.");
  }
}
