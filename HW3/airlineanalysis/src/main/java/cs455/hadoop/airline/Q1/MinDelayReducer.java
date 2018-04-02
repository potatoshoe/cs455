package cs455.hadoop.airline.Q1;

import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;

import java.io.IOException;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.Set;

public class MinDelayReducer extends Reducer<Text, Text, Text, Text> {

    @Override
    protected void reduce(Text key, Iterable<Text> values, Context context) throws IOException, InterruptedException {
        Hashtable<Integer, Integer> key_values = new Hashtable<>();

        for(Text t : values){

            try {
                String[] data = t.toString().split("\\|");

                if (data.length > 1) {
                    int data_key = Integer.parseInt(data[0]);
                    int data_value = Integer.parseInt(data[1]);

                    if (key_values.containsKey(data_key)) {
                        key_values.replace(data_key, key_values.get(data_key).intValue() + data_value);
                    } else {
                        key_values.put(data_key, data_value);
                    }
                } else {

                }

            } catch (NumberFormatException nfe) {
                // pass
            }

        }

        int min_key = Integer.MAX_VALUE;
        int min_value = Integer.MAX_VALUE;

        Set<Integer> keys = key_values.keySet();
        for (Integer i : keys) {
            context.write(new Text("TEST" + i), new Text(i + ": " + key_values.get(i)));
            if (key_values.get(i) < min_value) {
                min_key = i;
                min_value = key_values.get(i);
            }
        }

        context.write(key, new Text("MIN: " + min_key + ": " + min_value + "\n"));
    }

}
