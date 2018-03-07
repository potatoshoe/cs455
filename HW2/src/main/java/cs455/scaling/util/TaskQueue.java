package cs455.scaling.util;

import cs455.scaling.task.Task;

import java.util.Comparator;
import java.util.PriorityQueue;

public class TaskQueue {

    private PriorityQueue<Task> queue;

    public TaskQueue() {
        this.queue = new PriorityQueue(new Comparator() {
            @Override
            public int compare(Object o1, Object o2) {
                return 0;
            }
        });
    }

    public Task poll() {
        try {
            synchronized (this.queue) {
                while (this.queue.peek() == null) {
                    queue.wait();
                }
                return queue.poll();
            }
        } catch (Exception e) {}
        return null;
    }

    public void put(Task t) {
        try {
            synchronized (this.queue) {
                this.queue.add(t);
                this.queue.notifyAll();
            }
        } catch (Exception e) {

        }
    }

}
