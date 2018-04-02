package cs455.hadoop.airline.Q1;

import cs455.hadoop.airline.util.IntArrayWritable;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.ArrayWritable;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

import java.io.IOException;

public class MinDelayJob {

    public static void main(String[] args) {
        try {
            Configuration conf = new Configuration();
            // Give the MapRed job a name. You'll see this name in the Yarn webapp.
            Job job = Job.getInstance(conf, "Q1 Min Delay");
            // Current class.
            job.setJarByClass(MinDelayJob.class);
            // Mapper
            job.setMapperClass(MinDelayMapper.class);
            // Combiner. We use the reducer as the combiner in this case.
            job.setCombinerClass(MinDelayReducer.class);
            // Reducer
            job.setReducerClass(MinDelayReducer.class);


            // Outputs from the Mapper.
            job.setMapOutputKeyClass(Text.class);
            job.setMapOutputValueClass(IntArrayWritable.class);


            // Outputs from Reducer. It is sufficient to set only the following two properties
            // if the Mapper and Reducer has same key and value types. It is set separately for
            // elaboration.
            job.setOutputKeyClass(Text.class);
            job.setOutputValueClass(Text.class);


            // path to input in HDFS
            FileInputFormat.addInputPath(job, new Path(args[0]));


            // path to output in HDFS
            FileOutputFormat.setOutputPath(job, new Path(args[1]));


            // Block until the job is completed.
            System.exit(job.waitForCompletion(true) ? 0 : 1);
        } catch (IOException e) {
            System.err.println(e.getMessage());
        } catch (InterruptedException e) {
            System.err.println(e.getMessage());
        } catch (ClassNotFoundException e) {
            System.err.println(e.getMessage());
        }
}
}
