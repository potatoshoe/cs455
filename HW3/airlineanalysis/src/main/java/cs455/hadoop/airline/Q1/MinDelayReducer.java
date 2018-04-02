package cs455.hadoop.airline.Q1;

import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;

import java.io.IOException;
import java.util.Arrays;
import java.util.Hashtable;

public class MinDelayReducer extends Reducer<Text, Text, Text, Text> {

    @Override
    protected void reduce(Text key, Iterable<Text> values, Context context) throws IOException, InterruptedException {
        Hashtable<Integer, Integer> key_values = new Hashtable<>();

        for(Text t : values){

            try {
                String data_raw = t.toString();

                String[] data = data_raw.split("\\|");
                int data_key = 0;
                int data_value = 0;
                try {
                    data_key = Integer.parseInt(data[0]);
                    data_value = Integer.parseInt(data[1]);
                    context.write(new Text(Integer.toString(data_key)), new Text(Integer.toString(data_value)));
                    context.write(key, t);
                } catch (Exception e) {
                    context.write(key, new Text(Arrays.toString(data)));
                }

                if (key_values.containsKey(data_key)) {
                    key_values.replace(data_key, key_values.get(data_key).intValue() + data_value);
                } else {
                    key_values.put(data_key, data_value);
                }

            } catch (NumberFormatException nfe) {
                // pass
            }

        }

        int min_key = Integer.MAX_VALUE;
        int min_value = Integer.MAX_VALUE;

        for (Integer i : key_values.keySet()) {
            if (key_values.get(i) < min_value) {
                min_key = i;
                min_value = key_values.get(i);
            }
        }


        context.write(key, new Text(min_key + ": " + min_value + "\n"));
    }

}
