/* license: https://mit-license.org
 *
 *  MTP: Message Transfer Protocol
 *
 *                                Written in 2020 by Moky <albert.moky@gmail.com>
 *
 * ==============================================================================
 * The MIT License (MIT)
 *
 * Copyright (c) 2020 Albert Moky
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 * ==============================================================================
 */
package chat.dim.mtp;

import java.lang.ref.WeakReference;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.List;

import chat.dim.mtp.protocol.DataType;
import chat.dim.mtp.protocol.Header;
import chat.dim.mtp.protocol.Package;
import chat.dim.mtp.task.Arrival;
import chat.dim.mtp.task.Assemble;
import chat.dim.mtp.task.Departure;
import chat.dim.tlv.IntData;

public class Peer extends Thread {

    private boolean running = false;

    private Pool pool = null;

    private WeakReference<PeerDelegate> delegateRef = null;

    public Peer() {
        super();
    }

    public boolean isRunning() {
        return running;
    }

    public synchronized Pool getPool() {
        if (pool == null) {
            pool = createPool();
        }
        return pool;
    }

    protected Pool createPool() {
        return new MemPool();
    }

    public synchronized PeerDelegate getDelegate() {
        if (delegateRef == null) {
            return null;
        }
        return delegateRef.get();
    }
    public synchronized void setDelegate(PeerDelegate delegate) {
        if (delegate == null) {
            delegateRef = null;
        } else {
            delegateRef = new WeakReference<>(delegate);
        }
    }

    public void start() {
        running = true;
        super.start();
    }

    // stop()
    public void close() {
        running = false;
    }

    private void _sleep(double seconds) {
        try {
            sleep((long) (seconds * 1000));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        Pool pool;
        PeerDelegate delegate;
        int done;
        Departure departure;
        List<Assemble> assembling;
        while (running) {
            try {
                pool = getPool();
                delegate = getDelegate();
                // first, process all arrivals
                done = cleanArrivals();
                // second, get one departure task
                departure = pool.shiftExpiredDeparture();
                if (departure == null) {
                    // third, if no departure task, remove expired fragments
                    assembling = pool.discardFragments();
                    for (Assemble item : assembling) {
                        delegate.recycleFragments(item.fragments, item.source, item.destination);
                    }
                    if (done == 0) {
                        // all jobs done, have a rest. ^_^
                        _sleep(0.1);
                    }
                } else {
                    // redo this departure task
                    send(departure);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     *  Process the received packages in waiting list
     *
     * @return finished task count
     */
    private int cleanArrivals() {
        int done = 0;
        Pool pool = getPool();
        int total = pool.getCountOfArrivals();
        Arrival arrival;
        while (done < total) {
            arrival = pool.shiftFirstArrival();
            if (arrival == null) {
                // no task now
                break;
            }
            handle(arrival);
            done += 1;
        }
        return done;
    }

    private void handle(Arrival task) {
        Package pack = Package.parse(task.payload);
        if (pack == null) {
            //throw new NullPointerException("package error: " + Arrays.toString(task.payload));
            return;
        }
        boolean ok;
        Header head = pack.head;
        DataType type = head.type;
        if (type.equals(DataType.CommandRespond)) {
            // command response
            if (getPool().deleteDeparture(pack, task.source, task.destination)) {
                // if departure task is deleted, means it's finished
                getDelegate().onSendCommandSuccess(head.sn, task.source, task.destination);
            }
            return;
        } else if (type.equals(DataType.MessageRespond)) {
            // message response
            if (getPool().deleteDeparture(pack, task.source, task.destination)) {
                // if departure task is deleted, means it's finished
                getDelegate().onSendMessageSuccess(head.sn, task.source, task.destination);
            }
            return;
        } else if (type.equals(DataType.Command)) {
            // handle command
            ok = getDelegate().onReceivedCommand(pack.body, task.source, task.destination);
        } else if (type.equals(DataType.Message)) {
            // handle message
            ok = getDelegate().onReceivedMessage(pack.body, task.source, task.destination);
        } else {
            // handle message fragment
            assert type.equals(DataType.MessageFragment) : "data type error: " + type;
            ok = getDelegate().checkFragment(pack, task.source, task.destination);
            if (ok) {
                // assemble fragments
                Package msg = getPool().insertFragment(pack, task.source, task.destination);
                if (msg != null) {
                    // all fragments received
                    getDelegate().onReceivedMessage(msg.body, task.source, task.destination);
                }
            }
        }
        // respond to the sender
        if (ok) {
            respond(pack, task.source, task.destination);
        }
    }

    private void respond(Package pack, SocketAddress remote, SocketAddress local) {
        byte[] body;
        Header head = pack.head;
        DataType type = head.type;
        if (type.equals(DataType.Command)) {
            type = DataType.CommandRespond;
            body = new byte[2];
            body[0] = 'O';
            body[1] = 'K';
        } else if (type.equals(DataType.Message)) {
            type = DataType.MessageRespond;
            body = new byte[2];
            body[0] = 'O';
            body[1] = 'K';
        } else if (type.equals(DataType.MessageFragment)) {
            type = DataType.MessageRespond;
            body = new byte[10];
            byte[] pages = IntData.intToBytes(head.pages, 4);
            byte[] offset = IntData.intToBytes(head.offset, 4);
            System.arraycopy(pages, 0, body, 0, 4);
            System.arraycopy(offset, 0, body, 4, 4);
            body[8] = 'O';
            body[9] = 'K';
        } else {
            throw new IllegalArgumentException("data type error: " + type);
        }
        Package response = Package.create(type, head.sn, body);
        // send response directly, don't add this task to waiting list
        int res = getDelegate().sendData(response.data, remote, local);
        assert res == response.data.length : "failed to respond: " + remote + ", " + type;
    }

    //
    //  Sending
    //

    private void send(Departure task) {
        PeerDelegate delegate = getDelegate();
        if (getPool().appendDeparture(task)) {
            // treat the task as a bundle of packages
            int res;
            List<Package> packages = task.packages;
            for (Package item : packages) {
                res = delegate.sendData(item.data, task.destination, task.source);
                assert res == item.data.length : "failed to resend task (" + packages.size() + " packages) to: " + task.destination;
            }
        } else {
            // mission failed
            DataType type = task.type;
            if (type.equals(DataType.Command)) {
                delegate.onSendCommandTimeout(task.sn, task.destination, task.source);
            } else if (type.equals(DataType.Message)) {
                delegate.onSendMessageTimeout(task.sn, task.destination, task.source);
            } else {
                throw new IllegalArgumentException("data type error: " + type);
            }
        }
    }

    //
    //  Command
    //

    public Departure sendCommand(Package pack, SocketAddress destination, SocketAddress source) {
        List<Package> packages = new ArrayList<>();
        packages.add(pack);
        Departure task = new Departure(packages, destination, source);
        send(task);
        return task;
    }

    public Departure sendCommand(byte[] cmd, SocketAddress destination, SocketAddress source) {
        Package pack = Package.create(DataType.Command, cmd);
        return sendCommand(pack, destination, source);
    }

    //
    //  Message
    //

    public Departure sendMessage(Package pack, SocketAddress destination, SocketAddress source) {
        List<Package> packages;
        if (pack.body.length <= Package.MAX_BODY_LEN || pack.head.type.equals(DataType.MessageFragment)) {
            packages = new ArrayList<>();
            packages.add(pack);
        } else {
            packages = pack.split();
        }
        Departure task = new Departure(packages, destination, source);
        send(task);
        return task;
    }

    public Departure sendMessage(byte[] msg, SocketAddress destination, SocketAddress source) {
        Package pack = Package.create(DataType.Message, msg);
        return sendMessage(pack, destination, source);
    }
}
