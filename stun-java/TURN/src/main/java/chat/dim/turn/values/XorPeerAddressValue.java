/* license: https://mit-license.org
 *
 *  TURN: Traversal Using Relays around NAT
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
package chat.dim.turn.values;

import chat.dim.stun.valus.MappedAddressValue;
import chat.dim.stun.valus.XorMappedAddressValue;
import chat.dim.tlv.Data;

public class XorPeerAddressValue extends XorMappedAddressValue {

    /*  14.3.  XOR-PEER-ADDRESS
     *
     *         The XOR-PEER-ADDRESS specifies the address and port of the peer as
     *         seen from the TURN server.  (For example, the peer's server-reflexive
     *         transport address if the peer is behind a NAT.)  It is encoded in the
     *         same way as XOR-MAPPED-ADDRESS [RFC5389].
     */

    public XorPeerAddressValue(MappedAddressValue addressValue) {
        super(addressValue);
    }

    public XorPeerAddressValue(Data data, String ip, int port, byte family) {
        super(data, ip, port, family);
    }
}
