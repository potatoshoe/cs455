package cs455.overlay.transport.TCPConnection;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class TCPSender {

    private Socket socket;
    private DataOutputStream dout;

    public TCPSender(Socket socket) throws IOException{
        this.socket = socket;
        this.dout = new DataOutputStream(socket.getOutputStream());
    }

    public void sendData(byte[] data){
        try {
            int dataLength = data.length;
            dout.writeInt(dataLength);
            dout.write(data, 0, dataLength);
            dout.flush();
        } catch (Exception e){
            System.out.println("Error sending data: " + e.getMessage());
            return;
        }
    }

}
