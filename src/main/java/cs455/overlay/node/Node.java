package cs455.overlay.node;

import cs455.overlay.wireformats.Event;

import java.net.Socket;

public abstract class Node {

    public abstract void onEvent(Event e);
}
