package cs455.overlay.transport;

import cs455.overlay.routing.Route;

import java.net.Socket;
import java.util.Arrays;
import java.util.PriorityQueue;
import java.util.Queue;

public class TCPConnection {

    private Socket socket;
    private Queue<byte[]> queue;
    private int id;
    private Route routingData;

    public TCPConnection(Socket socket) {
        this.queue = new PriorityQueue<>();
        this.socket = socket;
        this.routingData = new Route(socket.getInetAddress().getAddress(), socket.getPort(), -1);
        this.id = -1;
        init();
    }

    private void init(){
        Thread receiver = new Thread(() -> {
            try (
                    TCPReceiver rec = new TCPReceiver(this)
            ){
                rec.read();
            } catch (Exception e){
                System.out.println("[" + Thread.currentThread().getName() + "] Error: " + e.getMessage());
                e.printStackTrace();
            }
        }, "Connection Receiver Thread");
        receiver.start();

        Thread sender = new Thread(() -> {
            try (
                    TCPSender send = new TCPSender(this)
            ){
                while(true){
                    synchronized (queue) {
                        while (queue.peek() == null) {
                            queue.wait();
                        }

                        System.out.println("OK:" + Arrays.toString(queue.peek()));
                        send.sendData(queue.poll());

                        queue.notify();
                    }
                }
            } catch (Exception e){
                System.out.println("[" + Thread.currentThread().getName() + "] Error: " + e.getMessage());
                e.printStackTrace();
            }
        }, "Connection Sender Thread");
        sender.start();
    }

    public void sendData(byte[] b){
        synchronized (queue) {
            this.queue.add(b);
            queue.notify();
        }
    }

    public Socket getSocket(){
        return this.socket;
    }

    public void setId(int id){
        this.id = id;
    }

    public void setGuid(int guid){
        this.routingData.setGuid(guid);
    }

}