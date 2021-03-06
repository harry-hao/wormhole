/* license: https://mit-license.org
 *
 *  STUN: Session Traversal Utilities for NAT
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
package chat.dim.stun.attributes;

import java.util.HashMap;
import java.util.Map;

import chat.dim.stun.valus.*;
import chat.dim.tlv.Data;
import chat.dim.tlv.Tag;
import chat.dim.tlv.UInt16Data;

public class AttributeType extends Tag {

    public final int value;
    private final String name;

    public AttributeType(AttributeType type) {
        super(type);
        value = type.value;
        name = type.name;
    }

    public AttributeType(Data data, int value, String name) {
        super(data);
        this.value = value;
        this.name = name;
        s_types.put(value, this);
    }

    public AttributeType(Data data, int value) {
        this(data, value, "Attribute-" + Integer.toHexString(value));
    }

    public AttributeType(int value, String name) {
        this(new UInt16Data(value), value, name);
    }

    public AttributeType(int value) {
        this(new UInt16Data(value), value);
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof AttributeType) {
            return equals(((AttributeType) other).value);
        }
        return super.equals(other);
    }
    public boolean equals(int other) {
        return value == other;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(value);
    }

    @Override
    public String toString() {
        return name;
    }

    //
    //  Factory
    //

    public static AttributeType parse(Data data) {
        int length = data.getLength();
        if (length < 2) {
            return null;
        } else if (length > 2) {
            data = data.slice(0, 2);
        }
        int value = data.getUInt16Value(0);
        return getInstance(value);
    }

    public static synchronized AttributeType getInstance(int value) {
        AttributeType type = s_types.get(value);
        if (type == null) {
            type = new AttributeType(value);
        }
        return type;
    }

    // Attribute Types in STUN message
    private static final Map<Integer, AttributeType> s_types = new HashMap<>();

    // Comprehension-required range (0x0000-0x7FFF)
    // Comprehension-optional range (0x8000-0xFFFF)

    // [RFC-3489]
    public static final AttributeType MappedAddress     = new AttributeType(0x0001, "MAPPED-ADDRESS");
    public static final AttributeType ResponseAddress   = new AttributeType(0x0002, "RESPONSE-ADDRESS");
    public static final AttributeType ChangeRequest     = new AttributeType(0x0003, "CHANGE-REQUEST");
    public static final AttributeType SourceAddress     = new AttributeType(0x0004, "SOURCE-ADDRESS");
    public static final AttributeType ChangedAddress    = new AttributeType(0x0005, "CHANGED-ADDRESS");
    public static final AttributeType Username          = new AttributeType(0x0006, "USERNAME");
    public static final AttributeType Password          = new AttributeType(0x0007, "PASSWORD");
    public static final AttributeType MessageIntegrity  = new AttributeType(0x0008, "MESSAGE-INTEGRITY");
    public static final AttributeType ErrorCode         = new AttributeType(0x0009, "ERROR-CODE");
    public static final AttributeType UnknownAttributes = new AttributeType(0x000A, "UNKNOWN-ATTRIBUTES");
    public static final AttributeType ReflectedFrom     = new AttributeType(0x000B, "REFLECTED-FROM");

    // [RFC-5389]
    public static final AttributeType Realm             = new AttributeType(0x0014, "REALM");
    public static final AttributeType Nonce             = new AttributeType(0x0015, "NONCE");
    public static final AttributeType XorMappedAddress  = new AttributeType(0x0020, "XOR-MAPPED-ADDRESS(0020)");

    public static final AttributeType XorMappedAddress2 = new AttributeType(0x8020, "XOR-MAPPED-ADDRESS(8020)");
    public static final AttributeType XorOnly           = new AttributeType(0x8021, "XOR-ONLY");
    public static final AttributeType Software          = new AttributeType(0x8022, "SOFTWARE");
    public static final AttributeType AlternateServer   = new AttributeType(0x8023, "ALTERNATE-SERVER");
    public static final AttributeType Fingerprint       = new AttributeType(0x8028, "FINGERPRINT");

    static {
        //
        //  Register attribute parsers
        //
        AttributeValue.register(MappedAddress,     MappedAddressValue.class);
        //AttributeValue.register(XorMappedAddress,  XorMappedAddressValue.class);
        //AttributeValue.register(XorMappedAddress2, XorMappedAddressValue2.class);

        AttributeValue.register(ResponseAddress,   ResponseAddressValue.class);
        AttributeValue.register(ChangeRequest,     ChangeRequestValue.class);
        AttributeValue.register(SourceAddress,     SourceAddressValue.class);
        AttributeValue.register(ChangedAddress,    ChangedAddressValue.class);

        AttributeValue.register(Software,          SoftwareValue.class);
    }
}
