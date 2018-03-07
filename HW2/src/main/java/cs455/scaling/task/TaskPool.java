package cs455.scaling.task;

import cs455.scaling.util.TaskQueue;

import java.util.ArrayList;

public class TaskPool {

    private ArrayList<TaskWorker> workers;
    private TaskQueue queue;

    public TaskPool(int numThreads) {
        this.queue = new TaskQueue();
        workers = new ArrayList<>();
        initializeWorkers(numThreads);
    }

    private void initializeWorkers(int numThreads) {
        for (int i = 0 ; i < numThreads ; i++) {
            this.workers.add(new TaskWorker(queue, i));
            this.workers.get(i).start();
        }
    }

    public void addTask(Task t) {
        this.queue.put(t);
    }

}
